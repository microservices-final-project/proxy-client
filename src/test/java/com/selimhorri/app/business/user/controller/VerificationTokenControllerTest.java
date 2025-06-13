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

import java.time.LocalDate;
import java.util.Arrays;
import java.util.Collection;

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
import com.selimhorri.app.business.user.model.VerificationTokenDto;
import com.selimhorri.app.business.user.model.response.VerificationUserTokenServiceCollectionDtoResponse;
import com.selimhorri.app.business.user.service.VerificationTokenClientService;
import com.selimhorri.app.exception.wrapper.UnauthorizedException;

@ExtendWith(MockitoExtension.class)
@DisplayName("VerificationTokenController Unit Tests")
class VerificationTokenControllerTest {

    @Mock
    private AuthUtil authUtil;

    @Mock
    private VerificationTokenClientService verificationTokenClientService;

    @Mock
    private HttpServletRequest request;

    @Mock
    private UserDetails userDetails;

    private VerificationTokenController verificationTokenController;
    private VerificationTokenDto verificationTokenDto;
    private VerificationUserTokenServiceCollectionDtoResponse collectionResponse;

    @BeforeEach
    void setUp() {
        // Create controller and inject mocks
        verificationTokenController = new VerificationTokenController(verificationTokenClientService);
        ReflectionTestUtils.setField(verificationTokenController, "authUtil", authUtil);
        
        // Setup CredentialDto
        CredentialDto credentialDto = CredentialDto.builder()
                .credentialId(1)
                .username("testuser")
                .password("encodedPassword")
                .build();

        // Setup VerificationTokenDto
        verificationTokenDto = VerificationTokenDto.builder()
                .verificationTokenId(1)
                .token("test-token")
                .expireDate(LocalDate.now().plusDays(1))
                .credentialDto(credentialDto)
                .build();

        // Setup Collection Response
        Collection<VerificationTokenDto> tokens = Arrays.asList(verificationTokenDto);
        collectionResponse = VerificationUserTokenServiceCollectionDtoResponse.builder()
                .collection(tokens)
                .build();
    }

    @Test
    @DisplayName("Should find all verification tokens successfully")
    void findAll_ShouldReturnAllTokens_WhenCalled() {
        // Given
        ResponseEntity<VerificationUserTokenServiceCollectionDtoResponse> serviceResponse = 
            new ResponseEntity<>(collectionResponse, HttpStatus.OK);
        when(verificationTokenClientService.findAll()).thenReturn(serviceResponse);

        // When
        ResponseEntity<VerificationUserTokenServiceCollectionDtoResponse> result = verificationTokenController.findAll();

        // Then
        assertNotNull(result);
        assertEquals(HttpStatus.OK, result.getStatusCode());
        assertEquals(collectionResponse, result.getBody());
        verify(verificationTokenClientService, times(1)).findAll();
    }

    @Test
    @DisplayName("Should find verification token by ID successfully")
    void findById_ShouldReturnToken_WhenTokenExists() {
        // Given
        String verificationTokenId = "1";
        ResponseEntity<VerificationTokenDto> serviceResponse = 
            new ResponseEntity<>(verificationTokenDto, HttpStatus.OK);
        when(verificationTokenClientService.findById(verificationTokenId)).thenReturn(serviceResponse);

        // When
        ResponseEntity<VerificationTokenDto> result = verificationTokenController.findById(verificationTokenId);

        // Then
        assertNotNull(result);
        assertEquals(HttpStatus.OK, result.getStatusCode());
        assertEquals(verificationTokenDto, result.getBody());
        verify(verificationTokenClientService, times(1)).findById(verificationTokenId);
    }

    @Test
    @DisplayName("Should save verification token successfully when user is authorized")
    void save_ShouldReturnSavedToken_WhenUserIsAuthorized() {
        // Given
        String userId = "1";
        ResponseEntity<VerificationTokenDto> serviceResponse = 
            new ResponseEntity<>(verificationTokenDto, HttpStatus.OK);
        
        when(authUtil.getOwner(
            verificationTokenDto.getCredentialDto().getCredentialId().toString(), 
            ResourceType.CREDENTIALS))
            .thenReturn(userId);
        doNothing().when(authUtil).canActivate(request, userId, userDetails);
        when(verificationTokenClientService.save(verificationTokenDto)).thenReturn(serviceResponse);

        // When
        ResponseEntity<VerificationTokenDto> result = verificationTokenController.save(
            verificationTokenDto, request, userDetails);

        // Then
        assertNotNull(result);
        assertEquals(HttpStatus.OK, result.getStatusCode());
        assertEquals(verificationTokenDto, result.getBody());
        verify(authUtil, times(1)).getOwner(
            verificationTokenDto.getCredentialDto().getCredentialId().toString(), 
            ResourceType.CREDENTIALS);
        verify(authUtil, times(1)).canActivate(request, userId, userDetails);
        verify(verificationTokenClientService, times(1)).save(verificationTokenDto);
    }

    @Test
    @DisplayName("Should throw UnauthorizedException when user is not authorized to save token")
    void save_ShouldThrowUnauthorizedException_WhenUserIsNotAuthorized() {
        // Given
        String userId = "1";
        
        when(authUtil.getOwner(
            verificationTokenDto.getCredentialDto().getCredentialId().toString(), 
            ResourceType.CREDENTIALS))
            .thenReturn(userId);
        doThrow(new UnauthorizedException("You can access to resources of your own"))
            .when(authUtil).canActivate(request, userId, userDetails);

        // When & Then
        UnauthorizedException exception = assertThrows(UnauthorizedException.class, () -> {
            verificationTokenController.save(verificationTokenDto, request, userDetails);
        });
        
        assertEquals("You can access to resources of your own", exception.getMessage());
        verify(authUtil, times(1)).getOwner(
            verificationTokenDto.getCredentialDto().getCredentialId().toString(), 
            ResourceType.CREDENTIALS);
        verify(authUtil, times(1)).canActivate(request, userId, userDetails);
        verify(verificationTokenClientService, times(0)).save(any(VerificationTokenDto.class));
    }

    @Test
    @DisplayName("Should update verification token successfully")
    void update_ShouldReturnUpdatedToken_WhenValidTokenProvided() {
        // Given
        String verificationTokenId = "1";
        ResponseEntity<VerificationTokenDto> serviceResponse = 
            new ResponseEntity<>(verificationTokenDto, HttpStatus.OK);
        when(verificationTokenClientService.update(verificationTokenDto)).thenReturn(serviceResponse);

        // When
        ResponseEntity<VerificationTokenDto> result = verificationTokenController.update(
            verificationTokenId, verificationTokenDto);

        // Then
        assertNotNull(result);
        assertEquals(HttpStatus.OK, result.getStatusCode());
        assertEquals(verificationTokenDto, result.getBody());
        verify(verificationTokenClientService, times(1)).update(verificationTokenDto);
    }

    @Test
    @DisplayName("Should delete verification token successfully")
    void deleteById_ShouldReturnTrue_WhenDeletionSuccessful() {
        // Given
        String verificationTokenId = "1";
        ResponseEntity<Boolean> serviceResponse = new ResponseEntity<>(true, HttpStatus.OK);
        when(verificationTokenClientService.deleteById(verificationTokenId)).thenReturn(serviceResponse);

        // When
        ResponseEntity<Boolean> result = verificationTokenController.deleteById(verificationTokenId);

        // Then
        assertNotNull(result);
        assertEquals(HttpStatus.OK, result.getStatusCode());
        assertEquals(true, result.getBody());
        verify(verificationTokenClientService, times(1)).deleteById(verificationTokenId);
    }

    @Test
    @DisplayName("Should handle null response body from service")
    void findAll_ShouldHandleNullResponseBody_WhenServiceReturnsNullBody() {
        // Given
        ResponseEntity<VerificationUserTokenServiceCollectionDtoResponse> serviceResponse = 
            new ResponseEntity<>(null, HttpStatus.OK);
        when(verificationTokenClientService.findAll()).thenReturn(serviceResponse);

        // When
        ResponseEntity<VerificationUserTokenServiceCollectionDtoResponse> result = verificationTokenController.findAll();

        // Then
        assertNotNull(result);
        assertEquals(HttpStatus.OK, result.getStatusCode());
        assertEquals(null, result.getBody());
        verify(verificationTokenClientService, times(1)).findAll();
    }

    @Test
    @DisplayName("Should handle empty token collection")
    void findAll_ShouldHandleEmptyCollection_WhenNoTokensExist() {
        // Given
        Collection<VerificationTokenDto> emptyTokens = Arrays.asList();
        VerificationUserTokenServiceCollectionDtoResponse emptyResponse = 
            VerificationUserTokenServiceCollectionDtoResponse.builder()
                .collection(emptyTokens)
                .build();
        ResponseEntity<VerificationUserTokenServiceCollectionDtoResponse> serviceResponse = 
            new ResponseEntity<>(emptyResponse, HttpStatus.OK);
        when(verificationTokenClientService.findAll()).thenReturn(serviceResponse);

        // When
        ResponseEntity<VerificationUserTokenServiceCollectionDtoResponse> result = verificationTokenController.findAll();

        // Then
        assertNotNull(result);
        assertEquals(HttpStatus.OK, result.getStatusCode());
        assertNotNull(result.getBody());
        assertEquals(0, result.getBody().getCollection().size());
        verify(verificationTokenClientService, times(1)).findAll();
    }

    @Test
    @DisplayName("Should handle null credential in verification token during save")
    void save_ShouldThrowException_WhenCredentialIsNull() {
        // Given
        VerificationTokenDto tokenWithNullCredential = VerificationTokenDto.builder()
                .verificationTokenId(1)
                .token("test-token")
                .expireDate(LocalDate.now().plusDays(1))
                .credentialDto(null)
                .build();

        // When & Then
        assertThrows(NullPointerException.class, () -> {
            verificationTokenController.save(tokenWithNullCredential, request, userDetails);
        });

        verify(verificationTokenClientService, times(0)).save(any(VerificationTokenDto.class));
    }

    @Test
    @DisplayName("Should handle null credential ID in verification token during save")
    void save_ShouldThrowException_WhenCredentialIdIsNull() {
        // Given
        CredentialDto credentialWithNullId = CredentialDto.builder()
                .credentialId(null)
                .username("testuser")
                .password("encodedPassword")
                .build();
        
        VerificationTokenDto tokenWithNullCredentialId = VerificationTokenDto.builder()
                .verificationTokenId(1)
                .token("test-token")
                .expireDate(LocalDate.now().plusDays(1))
                .credentialDto(credentialWithNullId)
                .build();

        // When & Then
        assertThrows(NullPointerException.class, () -> {
            verificationTokenController.save(tokenWithNullCredentialId, request, userDetails);
        });

        verify(verificationTokenClientService, times(0)).save(any(VerificationTokenDto.class));
    }
}