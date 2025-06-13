package com.selimhorri.app.business.user.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
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
import com.selimhorri.app.business.user.model.AddressDto;
import com.selimhorri.app.business.user.model.UserDto;
import com.selimhorri.app.business.user.model.response.AddressUserServiceCollectionDtoResponse;
import com.selimhorri.app.business.user.service.AddressClientService;
import com.selimhorri.app.exception.wrapper.UnauthorizedException;

import java.util.Arrays;
import java.util.Collection;

@ExtendWith(MockitoExtension.class)
@DisplayName("AddressController Unit Tests")
class AddressControllerTest {

    @Mock
    private AuthUtil authUtil;

    @Mock
    private AddressClientService addressClientService;

    @Mock
    private HttpServletRequest request;

    @Mock
    private UserDetails userDetails;

    private AddressController addressController;
    private AddressDto addressDto;
    private UserDto userDto;
    private AddressUserServiceCollectionDtoResponse collectionResponse;

    @BeforeEach
    void setUp() {
        // Create controller and inject mocks
        addressController = new AddressController(addressClientService);
        ReflectionTestUtils.setField(addressController, "authUtil", authUtil);
        
        // Setup UserDto
        userDto = UserDto.builder()
                .userId(1)
                .firstName("John")
                .lastName("Doe")
                .email("john.doe@example.com")
                .phone("123456789")
                .build();

        // Setup AddressDto
        addressDto = AddressDto.builder()
                .addressId(1)
                .fullAddress("123 Main Street")
                .postalCode("12345")
                .city("Test City")
                .userDto(userDto)
                .build();

        // Setup Collection Response
        Collection<AddressDto> addresses = Arrays.asList(addressDto);
        collectionResponse = AddressUserServiceCollectionDtoResponse.builder()
                .collection(addresses)
                .build();
    }

    @Test
    @DisplayName("Should find all addresses successfully")
    void findAll_ShouldReturnAllAddresses_WhenCalled() {
        // Given
        ResponseEntity<AddressUserServiceCollectionDtoResponse> serviceResponse = 
            new ResponseEntity<>(collectionResponse, HttpStatus.OK);
        when(addressClientService.findAll()).thenReturn(serviceResponse);

        // When
        ResponseEntity<AddressUserServiceCollectionDtoResponse> result = addressController.findAll();

        // Then
        assertNotNull(result);
        assertEquals(HttpStatus.OK, result.getStatusCode());
        assertEquals(collectionResponse, result.getBody());
        verify(addressClientService, times(1)).findAll();
    }

    @Test
    @DisplayName("Should find address by ID successfully when user is authorized")
    void findById_ShouldReturnAddress_WhenUserIsAuthorized() {
        // Given
        String addressId = "1";
        String userId = "1";
        ResponseEntity<AddressDto> serviceResponse = new ResponseEntity<>(addressDto, HttpStatus.OK);
        
        when(authUtil.getOwner(addressId, ResourceType.ADDRESSES)).thenReturn(userId);
        doNothing().when(authUtil).canActivate(request, userId, userDetails);
        when(addressClientService.findById(addressId)).thenReturn(serviceResponse);

        // When
        ResponseEntity<AddressDto> result = addressController.findById(addressId, request, userDetails);

        // Then
        assertNotNull(result);
        assertEquals(HttpStatus.OK, result.getStatusCode());
        assertEquals(addressDto, result.getBody());
        verify(authUtil, times(1)).getOwner(addressId, ResourceType.ADDRESSES);
        verify(authUtil, times(1)).canActivate(request, userId, userDetails);
        verify(addressClientService, times(1)).findById(addressId);
    }

    @Test
    @DisplayName("Should throw UnauthorizedException when user is not authorized to find address by ID")
    void findById_ShouldThrowUnauthorizedException_WhenUserIsNotAuthorized() {
        // Given
        String addressId = "1";
        String userId = "1";
        
        when(authUtil.getOwner(addressId, ResourceType.ADDRESSES)).thenReturn(userId);
        doThrow(new UnauthorizedException("You can access to resources of your own"))
            .when(authUtil).canActivate(request, userId, userDetails);

        // When & Then
        UnauthorizedException exception = assertThrows(UnauthorizedException.class, () -> {
            addressController.findById(addressId, request, userDetails);
        });
        
        assertEquals("You can access to resources of your own", exception.getMessage());
        verify(authUtil, times(1)).getOwner(addressId, ResourceType.ADDRESSES);
        verify(authUtil, times(1)).canActivate(request, userId, userDetails);
        verify(addressClientService, times(0)).findById(anyString());
    }

    @Test
    @DisplayName("Should save address successfully when user is authorized")
    void save_ShouldReturnSavedAddress_WhenUserIsAuthorized() {
        // Given
        String userId = "1";
        ResponseEntity<AddressDto> serviceResponse = new ResponseEntity<>(addressDto, HttpStatus.OK);
        
        doNothing().when(authUtil).canActivate(request, userId, userDetails);
        when(addressClientService.save(addressDto)).thenReturn(serviceResponse);

        // When
        ResponseEntity<AddressDto> result = addressController.save(addressDto, request, userDetails);

        // Then
        assertNotNull(result);
        assertEquals(HttpStatus.OK, result.getStatusCode());
        assertEquals(addressDto, result.getBody());
        verify(authUtil, times(1)).canActivate(request, userId, userDetails);
        verify(addressClientService, times(1)).save(addressDto);
    }

    @Test
    @DisplayName("Should throw UnauthorizedException when user is not authorized to save address")
    void save_ShouldThrowUnauthorizedException_WhenUserIsNotAuthorized() {
        // Given
        String userId = "1";
        
        doThrow(new UnauthorizedException("You can access to resources of your own"))
            .when(authUtil).canActivate(request, userId, userDetails);

        // When & Then
        UnauthorizedException exception = assertThrows(UnauthorizedException.class, () -> {
            addressController.save(addressDto, request, userDetails);
        });
        
        assertEquals("You can access to resources of your own", exception.getMessage());
        verify(authUtil, times(1)).canActivate(request, userId, userDetails);
        verify(addressClientService, times(0)).save(any(AddressDto.class));
    }

    @Test
    @DisplayName("Should update address successfully when user is authorized")
    void update_ShouldReturnUpdatedAddress_WhenUserIsAuthorized() {
        // Given
        String addressId = "1";
        String userId = "1";
        ResponseEntity<AddressDto> serviceResponse = new ResponseEntity<>(addressDto, HttpStatus.OK);
        
        when(authUtil.getOwner(addressId, ResourceType.ADDRESSES)).thenReturn(userId);
        doNothing().when(authUtil).canActivate(request, userId, userDetails);
        when(addressClientService.update(addressId, addressDto)).thenReturn(serviceResponse);

        // When
        ResponseEntity<AddressDto> result = addressController.update(addressId, addressDto, request, userDetails);

        // Then
        assertNotNull(result);
        assertEquals(HttpStatus.OK, result.getStatusCode());
        assertEquals(addressDto, result.getBody());
        verify(authUtil, times(1)).getOwner(addressId, ResourceType.ADDRESSES);
        verify(authUtil, times(1)).canActivate(request, userId, userDetails);
        verify(addressClientService, times(1)).update(addressId, addressDto);
    }

    @Test
    @DisplayName("Should throw UnauthorizedException when user is not authorized to update address")
    void update_ShouldThrowUnauthorizedException_WhenUserIsNotAuthorized() {
        // Given
        String addressId = "1";
        String userId = "1";
        
        when(authUtil.getOwner(addressId, ResourceType.ADDRESSES)).thenReturn(userId);
        doThrow(new UnauthorizedException("You can access to resources of your own"))
            .when(authUtil).canActivate(request, userId, userDetails);

        // When & Then
        UnauthorizedException exception = assertThrows(UnauthorizedException.class, () -> {
            addressController.update(addressId, addressDto, request, userDetails);
        });
        
        assertEquals("You can access to resources of your own", exception.getMessage());
        verify(authUtil, times(1)).getOwner(addressId, ResourceType.ADDRESSES);
        verify(authUtil, times(1)).canActivate(request, userId, userDetails);
        verify(addressClientService, times(0)).update(anyString(), any(AddressDto.class));
    }

    @Test
    @DisplayName("Should delete address successfully when user is authorized")
    void deleteById_ShouldReturnTrue_WhenUserIsAuthorizedAndDeletionSuccessful() {
        // Given
        String addressId = "1";
        String userId = "1";
        ResponseEntity<Boolean> serviceResponse = new ResponseEntity<>(true, HttpStatus.OK);
        
        when(authUtil.getOwner(addressId, ResourceType.ADDRESSES)).thenReturn(userId);
        doNothing().when(authUtil).canActivate(request, userId, userDetails);
        when(addressClientService.deleteById(addressId)).thenReturn(serviceResponse);

        // When
        ResponseEntity<Boolean> result = addressController.deleteById(addressId, request, userDetails);

        // Then
        assertNotNull(result);
        assertEquals(HttpStatus.OK, result.getStatusCode());
        assertEquals(true, result.getBody());
        verify(authUtil, times(1)).getOwner(addressId, ResourceType.ADDRESSES);
        verify(authUtil, times(1)).canActivate(request, userId, userDetails);
        verify(addressClientService, times(1)).deleteById(addressId);
    }

    @Test
    @DisplayName("Should throw UnauthorizedException when user is not authorized to delete address")
    void deleteById_ShouldThrowUnauthorizedException_WhenUserIsNotAuthorized() {
        // Given
        String addressId = "1";
        String userId = "1";
        
        when(authUtil.getOwner(addressId, ResourceType.ADDRESSES)).thenReturn(userId);
        doThrow(new UnauthorizedException("You can access to resources of your own"))
            .when(authUtil).canActivate(request, userId, userDetails);

        // When & Then
        UnauthorizedException exception = assertThrows(UnauthorizedException.class, () -> {
            addressController.deleteById(addressId, request, userDetails);
        });
        
        assertEquals("You can access to resources of your own", exception.getMessage());
        verify(authUtil, times(1)).getOwner(addressId, ResourceType.ADDRESSES);
        verify(authUtil, times(1)).canActivate(request, userId, userDetails);
        verify(addressClientService, times(0)).deleteById(anyString());
    }

    @Test
    @DisplayName("Should handle null userId from getOwner method")
    void findById_ShouldHandleNullUserId_WhenGetOwnerReturnsNull() {
        // Given
        String addressId = "1";
        ResponseEntity<AddressDto> serviceResponse = new ResponseEntity<>(addressDto, HttpStatus.OK);
        
        when(authUtil.getOwner(addressId, ResourceType.ADDRESSES)).thenReturn(null);
        doNothing().when(authUtil).canActivate(request, null, userDetails);
        when(addressClientService.findById(addressId)).thenReturn(serviceResponse);

        // When
        ResponseEntity<AddressDto> result = addressController.findById(addressId, request, userDetails);

        // Then
        assertNotNull(result);
        assertEquals(HttpStatus.OK, result.getStatusCode());
        assertEquals(addressDto, result.getBody());
        verify(authUtil, times(1)).getOwner(addressId, ResourceType.ADDRESSES);
        verify(authUtil, times(1)).canActivate(request, null, userDetails);
        verify(addressClientService, times(1)).findById(addressId);
    }

    @Test
    @DisplayName("Should handle service returning null response body")
    void findAll_ShouldHandleNullResponseBody_WhenServiceReturnsNullBody() {
        // Given
        ResponseEntity<AddressUserServiceCollectionDtoResponse> serviceResponse = 
            new ResponseEntity<>(null, HttpStatus.OK);
        when(addressClientService.findAll()).thenReturn(serviceResponse);

        // When
        ResponseEntity<AddressUserServiceCollectionDtoResponse> result = addressController.findAll();

        // Then
        assertNotNull(result);
        assertEquals(HttpStatus.OK, result.getStatusCode());
        assertEquals(null, result.getBody());
        verify(addressClientService, times(1)).findAll();
    }

    @Test
    @DisplayName("Should handle empty address collection")
    void findAll_ShouldHandleEmptyCollection_WhenNoAddressesExist() {
        // Given
        Collection<AddressDto> emptyAddresses = Arrays.asList();
        AddressUserServiceCollectionDtoResponse emptyResponse = AddressUserServiceCollectionDtoResponse.builder()
                .collection(emptyAddresses)
                .build();
        ResponseEntity<AddressUserServiceCollectionDtoResponse> serviceResponse = 
            new ResponseEntity<>(emptyResponse, HttpStatus.OK);
        when(addressClientService.findAll()).thenReturn(serviceResponse);

        // When
        ResponseEntity<AddressUserServiceCollectionDtoResponse> result = addressController.findAll();

        // Then
        assertNotNull(result);
        assertEquals(HttpStatus.OK, result.getStatusCode());
        assertNotNull(result.getBody());
        assertEquals(0, result.getBody().getCollection().size());
        verify(addressClientService, times(1)).findAll();
    }

    @Test
    @DisplayName("Should handle address with null user")
    void save_ShouldHandleAddressWithNullUser_WhenUserDtoIsNull() {
        // Given
        AddressDto addressWithNullUser = AddressDto.builder()
                .addressId(1)
                .fullAddress("123 Main Street")
                .postalCode("12345")
                .city("Test City")
                .userDto(null)
                .build();

        // When & Then
        assertThrows(NullPointerException.class, () -> {
            addressController.save(addressWithNullUser, request, userDetails);
        });

        verify(addressClientService, times(0)).save(any(AddressDto.class));
    }
}