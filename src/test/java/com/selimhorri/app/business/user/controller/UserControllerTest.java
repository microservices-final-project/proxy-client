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

import com.selimhorri.app.business.auth.util.AuthUtil;
import com.selimhorri.app.business.user.model.UserDto;
import com.selimhorri.app.business.user.model.response.UserUserServiceCollectionDtoResponse;
import com.selimhorri.app.business.user.service.UserClientService;
import com.selimhorri.app.exception.wrapper.UnauthorizedException;

import java.util.Arrays;
import java.util.Collection;

@ExtendWith(MockitoExtension.class)
@DisplayName("UserController Unit Tests")
class UserControllerTest {

    @Mock
    private AuthUtil authUtil;

    @Mock
    private UserClientService userClientService;

    @Mock
    private HttpServletRequest request;

    @Mock
    private UserDetails userDetails;

    private UserController userController;
    private UserDto userDto;
    private UserUserServiceCollectionDtoResponse collectionResponse;

    @BeforeEach
    void setUp() {
        // Create controller and inject mocks
        userController = new UserController(userClientService);
        ReflectionTestUtils.setField(userController, "authUtil", authUtil);
        
        // Setup UserDto
        userDto = UserDto.builder()
                .userId(1)
                .firstName("John")
                .lastName("Doe")
                .email("john.doe@example.com")
                .phone("123456789")
                .build();

        // Setup Collection Response
        Collection<UserDto> users = Arrays.asList(userDto);
        collectionResponse = UserUserServiceCollectionDtoResponse.builder()
                .collection(users)
                .build();
    }

    @Test
    @DisplayName("Should find all users successfully")
    void findAll_ShouldReturnAllUsers_WhenCalled() {
        // Given
        ResponseEntity<UserUserServiceCollectionDtoResponse> serviceResponse = 
            new ResponseEntity<>(collectionResponse, HttpStatus.OK);
        when(userClientService.findAll()).thenReturn(serviceResponse);

        // When
        ResponseEntity<UserUserServiceCollectionDtoResponse> result = userController.findAll();

        // Then
        assertNotNull(result);
        assertEquals(HttpStatus.OK, result.getStatusCode());
        assertEquals(collectionResponse, result.getBody());
        verify(userClientService, times(1)).findAll();
    }

    @Test
    @DisplayName("Should find user by ID successfully when user is authorized")
    void findById_ShouldReturnUser_WhenUserIsAuthorized() {
        // Given
        String userId = "1";
        ResponseEntity<UserDto> serviceResponse = new ResponseEntity<>(userDto, HttpStatus.OK);
        
        doNothing().when(authUtil).canActivate(request, userId, userDetails);
        when(userClientService.findById(userId)).thenReturn(serviceResponse);

        // When
        ResponseEntity<UserDto> result = userController.findById(userId, request, userDetails);

        // Then
        assertNotNull(result);
        assertEquals(HttpStatus.OK, result.getStatusCode());
        assertEquals(userDto, result.getBody());
        verify(authUtil, times(1)).canActivate(request, userId, userDetails);
        verify(userClientService, times(1)).findById(userId);
    }

    @Test
    @DisplayName("Should throw UnauthorizedException when user is not authorized to find user by ID")
    void findById_ShouldThrowUnauthorizedException_WhenUserIsNotAuthorized() {
        // Given
        String userId = "1";
        
        doThrow(new UnauthorizedException("You can access to resources of your own"))
            .when(authUtil).canActivate(request, userId, userDetails);

        // When & Then
        UnauthorizedException exception = assertThrows(UnauthorizedException.class, () -> {
            userController.findById(userId, request, userDetails);
        });
        
        assertEquals("You can access to resources of your own", exception.getMessage());
        verify(authUtil, times(1)).canActivate(request, userId, userDetails);
        verify(userClientService, times(0)).findById(anyString());
    }

    @Test
    @DisplayName("Should find user by username successfully")
    void findByUsername_ShouldReturnUser_WhenUsernameExists() {
        // Given
        String username = "johndoe";
        ResponseEntity<UserDto> serviceResponse = new ResponseEntity<>(userDto, HttpStatus.OK);
        
        when(userClientService.findByUsername(username)).thenReturn(serviceResponse);

        // When
        ResponseEntity<UserDto> result = userController.findByUsername(username);

        // Then
        assertNotNull(result);
        assertEquals(HttpStatus.OK, result.getStatusCode());
        assertEquals(userDto, result.getBody());
        verify(userClientService, times(1)).findByUsername(username);
    }

    @Test
    @DisplayName("Should save user successfully")
    void save_ShouldReturnSavedUser_WhenValidUserProvided() {
        // Given
        ResponseEntity<UserDto> serviceResponse = new ResponseEntity<>(userDto, HttpStatus.OK);
        
        when(userClientService.save(userDto)).thenReturn(serviceResponse);

        // When
        ResponseEntity<UserDto> result = userController.save(userDto);

        // Then
        assertNotNull(result);
        assertEquals(HttpStatus.OK, result.getStatusCode());
        assertEquals(userDto, result.getBody());
        verify(userClientService, times(1)).save(userDto);
    }

    @Test
    @DisplayName("Should update user successfully when user is authorized")
    void update_ShouldReturnUpdatedUser_WhenUserIsAuthorized() {
        // Given
        String userId = "1";
        ResponseEntity<UserDto> serviceResponse = new ResponseEntity<>(userDto, HttpStatus.OK);
        
        doNothing().when(authUtil).canActivate(request, userId, userDetails);
        when(userClientService.update(userId, userDto)).thenReturn(serviceResponse);

        // When
        ResponseEntity<UserDto> result = userController.update(userId, userDto, request, userDetails);

        // Then
        assertNotNull(result);
        assertEquals(HttpStatus.OK, result.getStatusCode());
        assertEquals(userDto, result.getBody());
        verify(authUtil, times(1)).canActivate(request, userId, userDetails);
        verify(userClientService, times(1)).update(userId, userDto);
    }

    @Test
    @DisplayName("Should throw UnauthorizedException when user is not authorized to update user")
    void update_ShouldThrowUnauthorizedException_WhenUserIsNotAuthorized() {
        // Given
        String userId = "1";
        
        doThrow(new UnauthorizedException("You can access to resources of your own"))
            .when(authUtil).canActivate(request, userId, userDetails);

        // When & Then
        UnauthorizedException exception = assertThrows(UnauthorizedException.class, () -> {
            userController.update(userId, userDto, request, userDetails);
        });
        
        assertEquals("You can access to resources of your own", exception.getMessage());
        verify(authUtil, times(1)).canActivate(request, userId, userDetails);
        verify(userClientService, times(0)).update(anyString(), any(UserDto.class));
    }

    @Test
    @DisplayName("Should delete user successfully when user is authorized")
    void deleteById_ShouldReturnTrue_WhenUserIsAuthorizedAndDeletionSuccessful() {
        // Given
        String userId = "1";
        ResponseEntity<Boolean> serviceResponse = new ResponseEntity<>(true, HttpStatus.OK);
        
        doNothing().when(authUtil).canActivate(request, userId, userDetails);
        when(userClientService.deleteById(userId)).thenReturn(serviceResponse);

        // When
        ResponseEntity<Boolean> result = userController.deleteById(userId, request, userDetails);

        // Then
        assertNotNull(result);
        assertEquals(HttpStatus.OK, result.getStatusCode());
        assertEquals(true, result.getBody());
        verify(authUtil, times(1)).canActivate(request, userId, userDetails);
        verify(userClientService, times(1)).deleteById(userId);
    }

    @Test
    @DisplayName("Should throw UnauthorizedException when user is not authorized to delete user")
    void deleteById_ShouldThrowUnauthorizedException_WhenUserIsNotAuthorized() {
        // Given
        String userId = "1";
        
        doThrow(new UnauthorizedException("You can access to resources of your own"))
            .when(authUtil).canActivate(request, userId, userDetails);

        // When & Then
        UnauthorizedException exception = assertThrows(UnauthorizedException.class, () -> {
            userController.deleteById(userId, request, userDetails);
        });
        
        assertEquals("You can access to resources of your own", exception.getMessage());
        verify(authUtil, times(1)).canActivate(request, userId, userDetails);
        verify(userClientService, times(0)).deleteById(anyString());
    }

    @Test
    @DisplayName("Should handle service returning null response body")
    void findAll_ShouldHandleNullResponseBody_WhenServiceReturnsNullBody() {
        // Given
        ResponseEntity<UserUserServiceCollectionDtoResponse> serviceResponse = 
            new ResponseEntity<>(null, HttpStatus.OK);
        when(userClientService.findAll()).thenReturn(serviceResponse);

        // When
        ResponseEntity<UserUserServiceCollectionDtoResponse> result = userController.findAll();

        // Then
        assertNotNull(result);
        assertEquals(HttpStatus.OK, result.getStatusCode());
        assertEquals(null, result.getBody());
        verify(userClientService, times(1)).findAll();
    }

    @Test
    @DisplayName("Should handle empty user collection")
    void findAll_ShouldHandleEmptyCollection_WhenNoUsersExist() {
        // Given
        Collection<UserDto> emptyUsers = Arrays.asList();
        UserUserServiceCollectionDtoResponse emptyResponse = UserUserServiceCollectionDtoResponse.builder()
                .collection(emptyUsers)
                .build();
        ResponseEntity<UserUserServiceCollectionDtoResponse> serviceResponse = 
            new ResponseEntity<>(emptyResponse, HttpStatus.OK);
        when(userClientService.findAll()).thenReturn(serviceResponse);

        // When
        ResponseEntity<UserUserServiceCollectionDtoResponse> result = userController.findAll();

        // Then
        assertNotNull(result);
        assertEquals(HttpStatus.OK, result.getStatusCode());
        assertNotNull(result.getBody());
        assertEquals(0, result.getBody().getCollection().size());
        verify(userClientService, times(1)).findAll();
    }

    @Test
    @DisplayName("Should handle null user ID in findById")
    void findById_ShouldHandleNullUserId_WhenUserIdIsNull() {
        // Given
        String userId = null;
        
        // When & Then
        assertThrows(NullPointerException.class, () -> {
            userController.findById(userId, request, userDetails);
        });

        verify(userClientService, times(0)).findById(anyString());
    }
}