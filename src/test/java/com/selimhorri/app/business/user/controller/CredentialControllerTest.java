package com.selimhorri.app.business.user.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import javax.servlet.http.HttpServletRequest;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.test.util.ReflectionTestUtils;

import com.selimhorri.app.business.auth.enums.ResourceType;
import com.selimhorri.app.business.auth.util.AuthUtil;
import com.selimhorri.app.business.user.model.CredentialDto;
import com.selimhorri.app.business.user.model.UserDto;
import com.selimhorri.app.business.user.model.response.CredentialUserServiceCollectionDtoResponse;
import com.selimhorri.app.business.user.service.CredentialClientService;
import com.selimhorri.app.exception.wrapper.UnauthorizedException;

import java.util.Arrays;
import java.util.Collection;

@ExtendWith(MockitoExtension.class)
@DisplayName("CredentialController Unit Tests")
class CredentialControllerTest {

    @Mock
    private AuthUtil authUtil;

    @Mock
    private CredentialClientService credentialClientService;

    @Mock
    private HttpServletRequest request;

    @Mock
    private UserDetails userDetails;

    private CredentialController credentialController;
    private CredentialDto credentialDto;
    private UserDto userDto;
    private CredentialUserServiceCollectionDtoResponse collectionResponse;

    @BeforeEach
    void setUp() {
        // Create controller and inject mocks
        credentialController = new CredentialController(credentialClientService);
        ReflectionTestUtils.setField(credentialController, "authUtil", authUtil);
        
        // Setup UserDto
        userDto = UserDto.builder()
                .userId(1)
                .firstName("John")
                .lastName("Doe")
                .email("john.doe@example.com")
                .phone("123456789")
                .build();

        // Setup CredentialDto
        credentialDto = CredentialDto.builder()
                .credentialId(1)
                .username("johndoe")
                .password("encodedPassword")
                .isEnabled(true)
                .isAccountNonExpired(true)
                .isAccountNonLocked(true)
                .isCredentialsNonExpired(true)
                .userDto(userDto)
                .build();

        // Setup Collection Response
        Collection<CredentialDto> credentials = Arrays.asList(credentialDto);
        collectionResponse = CredentialUserServiceCollectionDtoResponse.builder()
                .collection(credentials)
                .build();
    }

    @Test
    @DisplayName("Should find all credentials successfully")
    void findAll_ShouldReturnAllCredentials_WhenCalled() {
        // Given
        ResponseEntity<CredentialUserServiceCollectionDtoResponse> serviceResponse = 
            new ResponseEntity<>(collectionResponse, HttpStatus.OK);
        when(credentialClientService.findAll()).thenReturn(serviceResponse);

        // When
        ResponseEntity<CredentialUserServiceCollectionDtoResponse> result = credentialController.findAll();

        // Then
        assertNotNull(result);
        assertEquals(HttpStatus.OK, result.getStatusCode());
        assertEquals(collectionResponse, result.getBody());
        verify(credentialClientService, times(1)).findAll();
    }

    @Test
    @DisplayName("Should find credential by ID successfully when user is authorized")
    void findById_ShouldReturnCredential_WhenUserIsAuthorized() {
        // Given
        String credentialId = "1";
        String userId = "1";
        ResponseEntity<CredentialDto> serviceResponse = new ResponseEntity<>(credentialDto, HttpStatus.OK);
        
        when(authUtil.getOwner(credentialId, ResourceType.CREDENTIALS)).thenReturn(userId);
        doNothing().when(authUtil).canActivate(request, userId, userDetails);
        when(credentialClientService.findById(credentialId)).thenReturn(serviceResponse);

        // When
        ResponseEntity<CredentialDto> result = credentialController.findById(credentialId, request, userDetails);

        // Then
        assertNotNull(result);
        assertEquals(HttpStatus.OK, result.getStatusCode());
        assertEquals(credentialDto, result.getBody());
        verify(authUtil, times(1)).getOwner(credentialId, ResourceType.CREDENTIALS);
        verify(authUtil, times(1)).canActivate(request, userId, userDetails);
        verify(credentialClientService, times(1)).findById(credentialId);
    }

    @Test
    @DisplayName("Should throw UnauthorizedException when user is not authorized to find credential by ID")
    void findById_ShouldThrowUnauthorizedException_WhenUserIsNotAuthorized() {
        // Given
        String credentialId = "1";
        String userId = "1";
        
        when(authUtil.getOwner(credentialId, ResourceType.CREDENTIALS)).thenReturn(userId);
        doThrow(new UnauthorizedException("You can access to resources of your own"))
            .when(authUtil).canActivate(request, userId, userDetails);

        // When & Then
        UnauthorizedException exception = assertThrows(UnauthorizedException.class, () -> {
            credentialController.findById(credentialId, request, userDetails);
        });
        
        assertEquals("You can access to resources of your own", exception.getMessage());
        verify(authUtil, times(1)).getOwner(credentialId, ResourceType.CREDENTIALS);
        verify(authUtil, times(1)).canActivate(request, userId, userDetails);
        verify(credentialClientService, times(0)).findById(anyString());
    }

    @Test
    @DisplayName("Should find credential by username successfully")
    void findByUsername_ShouldReturnCredential_WhenUsernameExists() {
        // Given
        String username = "johndoe";
        ResponseEntity<CredentialDto> serviceResponse = new ResponseEntity<>(credentialDto, HttpStatus.OK);
        
        when(credentialClientService.findByUsername(username)).thenReturn(serviceResponse);

        // When
        ResponseEntity<CredentialDto> result = credentialController.findByCredentialname(username);

        // Then
        assertNotNull(result);
        assertEquals(HttpStatus.OK, result.getStatusCode());
        assertEquals(credentialDto, result.getBody());
        verify(credentialClientService, times(1)).findByUsername(username);
    }

    @Test
    @DisplayName("Should save credential successfully when user is authorized")
    void save_ShouldReturnSavedCredential_WhenUserIsAuthorized() {
        // Given
        String userId = "1";
        ResponseEntity<CredentialDto> serviceResponse = new ResponseEntity<>(credentialDto, HttpStatus.OK);
        
        doNothing().when(authUtil).canActivate(request, userId, userDetails);
        when(credentialClientService.save(credentialDto)).thenReturn(serviceResponse);

        // When
        ResponseEntity<CredentialDto> result = credentialController.save(credentialDto, request, userDetails);

        // Then
        assertNotNull(result);
        assertEquals(HttpStatus.OK, result.getStatusCode());
        assertEquals(credentialDto, result.getBody());
        verify(authUtil, times(1)).canActivate(request, userId, userDetails);
        verify(credentialClientService, times(1)).save(credentialDto);
    }

    @Test
    @DisplayName("Should throw UnauthorizedException when user is not authorized to save credential")
    void save_ShouldThrowUnauthorizedException_WhenUserIsNotAuthorized() {
        // Given
        String userId = "1";
        
        doThrow(new UnauthorizedException("You can access to resources of your own"))
            .when(authUtil).canActivate(request, userId, userDetails);

        // When & Then
        UnauthorizedException exception = assertThrows(UnauthorizedException.class, () -> {
            credentialController.save(credentialDto, request, userDetails);
        });
        
        assertEquals("You can access to resources of your own", exception.getMessage());
        verify(authUtil, times(1)).canActivate(request, userId, userDetails);
        verify(credentialClientService, times(0)).save(any(CredentialDto.class));
    }

    @Test
    @DisplayName("Should delete credential successfully when user is authorized")
    void deleteById_ShouldReturnTrue_WhenUserIsAuthorizedAndDeletionSuccessful() {
        // Given
        String credentialId = "1";
        String userId = "1";
        ResponseEntity<Boolean> serviceResponse = new ResponseEntity<>(true, HttpStatus.OK);
        
        when(authUtil.getOwner(credentialId, ResourceType.CREDENTIALS)).thenReturn(userId);
        doNothing().when(authUtil).canActivate(request, userId, userDetails);
        when(credentialClientService.deleteById(credentialId)).thenReturn(serviceResponse);

        // When
        ResponseEntity<Boolean> result = credentialController.deleteById(credentialId, request, userDetails);

        // Then
        assertNotNull(result);
        assertEquals(HttpStatus.OK, result.getStatusCode());
        assertEquals(true, result.getBody());
        verify(authUtil, times(1)).getOwner(credentialId, ResourceType.CREDENTIALS);
        verify(authUtil, times(1)).canActivate(request, userId, userDetails);
        verify(credentialClientService, times(1)).deleteById(credentialId);
    }

    @Test
    @DisplayName("Should throw UnauthorizedException when user is not authorized to delete credential")
    void deleteById_ShouldThrowUnauthorizedException_WhenUserIsNotAuthorized() {
        // Given
        String credentialId = "1";
        String userId = "1";
        
        when(authUtil.getOwner(credentialId, ResourceType.CREDENTIALS)).thenReturn(userId);
        doThrow(new UnauthorizedException("You can access to resources of your own"))
            .when(authUtil).canActivate(request, userId, userDetails);

        // When & Then
        UnauthorizedException exception = assertThrows(UnauthorizedException.class, () -> {
            credentialController.deleteById(credentialId, request, userDetails);
        });
        
        assertEquals("You can access to resources of your own", exception.getMessage());
        verify(authUtil, times(1)).getOwner(credentialId, ResourceType.CREDENTIALS);
        verify(authUtil, times(1)).canActivate(request, userId, userDetails);
        verify(credentialClientService, times(0)).deleteById(anyString());
    }

    @Test
    @DisplayName("Should handle null userId from getOwner method")
    void findById_ShouldHandleNullUserId_WhenGetOwnerReturnsNull() {
        // Given
        String credentialId = "1";
        ResponseEntity<CredentialDto> serviceResponse = new ResponseEntity<>(credentialDto, HttpStatus.OK);
        
        when(authUtil.getOwner(credentialId, ResourceType.CREDENTIALS)).thenReturn(null);
        doNothing().when(authUtil).canActivate(request, null, userDetails);
        when(credentialClientService.findById(credentialId)).thenReturn(serviceResponse);

        // When
        ResponseEntity<CredentialDto> result = credentialController.findById(credentialId, request, userDetails);

        // Then
        assertNotNull(result);
        assertEquals(HttpStatus.OK, result.getStatusCode());
        assertEquals(credentialDto, result.getBody());
        verify(authUtil, times(1)).getOwner(credentialId, ResourceType.CREDENTIALS);
        verify(authUtil, times(1)).canActivate(request, null, userDetails);
        verify(credentialClientService, times(1)).findById(credentialId);
    }

    @Test
    @DisplayName("Should handle service returning null response body")
    void findAll_ShouldHandleNullResponseBody_WhenServiceReturnsNullBody() {
        // Given
        ResponseEntity<CredentialUserServiceCollectionDtoResponse> serviceResponse = 
            new ResponseEntity<>(null, HttpStatus.OK);
        when(credentialClientService.findAll()).thenReturn(serviceResponse);

        // When
        ResponseEntity<CredentialUserServiceCollectionDtoResponse> result = credentialController.findAll();

        // Then
        assertNotNull(result);
        assertEquals(HttpStatus.OK, result.getStatusCode());
        assertEquals(null, result.getBody());
        verify(credentialClientService, times(1)).findAll();
    }

    @Test
    @DisplayName("Should handle empty credential collection")
    void findAll_ShouldHandleEmptyCollection_WhenNoCredentialsExist() {
        // Given
        Collection<CredentialDto> emptyCredentials = Arrays.asList();
        CredentialUserServiceCollectionDtoResponse emptyResponse = CredentialUserServiceCollectionDtoResponse.builder()
                .collection(emptyCredentials)
                .build();
        ResponseEntity<CredentialUserServiceCollectionDtoResponse> serviceResponse = 
            new ResponseEntity<>(emptyResponse, HttpStatus.OK);
        when(credentialClientService.findAll()).thenReturn(serviceResponse);

        // When
        ResponseEntity<CredentialUserServiceCollectionDtoResponse> result = credentialController.findAll();

        // Then
        assertNotNull(result);
        assertEquals(HttpStatus.OK, result.getStatusCode());
        assertNotNull(result.getBody());
        assertEquals(0, result.getBody().getCollection().size());
        verify(credentialClientService, times(1)).findAll();
    }

    @Test
    @DisplayName("Should handle credential with null user")
    void save_ShouldThrowException_WhenUserDtoIsNull() {
        // Given
        CredentialDto credentialWithNullUser = CredentialDto.builder()
                .credentialId(1)
                .username("johndoe")
                .password("encodedPassword")
                .userDto(null)
                .build();

        // When & Then
        assertThrows(NullPointerException.class, () -> {
            credentialController.save(credentialWithNullUser, request, userDetails);
        });

        verify(credentialClientService, times(0)).save(any(CredentialDto.class));
    }
}