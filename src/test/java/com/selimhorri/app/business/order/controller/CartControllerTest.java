package com.selimhorri.app.business.order.controller;

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

import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;

import javax.servlet.http.HttpServletRequest;
import javax.validation.ConstraintViolationException;

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
import org.springframework.web.bind.MethodArgumentNotValidException;

import com.selimhorri.app.business.auth.enums.ResourceType;
import com.selimhorri.app.business.auth.util.AuthUtil;
import com.selimhorri.app.business.order.model.CartDto;
import com.selimhorri.app.business.order.model.OrderDto;
import com.selimhorri.app.business.order.model.UserDto;
import com.selimhorri.app.business.order.model.response.CartOrderServiceDtoCollectionResponse;
import com.selimhorri.app.business.order.service.CartClientService;
import com.selimhorri.app.exception.wrapper.UnauthorizedException;

@ExtendWith(MockitoExtension.class)
@DisplayName("CartController Unit Tests")
class CartControllerTest {

    @Mock
    private AuthUtil authUtil;

    @Mock
    private CartClientService cartClientService;

    @Mock
    private HttpServletRequest request;

    @Mock
    private UserDetails userDetails;

    private CartController cartController;
    private CartDto cartDto;
    private CartOrderServiceDtoCollectionResponse collectionResponse;

    @BeforeEach
    void setUp() {
        // Create controller and inject mocks
        cartController = new CartController(cartClientService);
        ReflectionTestUtils.setField(cartController, "authUtil", authUtil);
        
        // Setup UserDto
        UserDto userDto = UserDto.builder()
                .userId(1)
                .firstName("John")
                .lastName("Doe")
                .email("john.doe@example.com")
                .phone("123456789")
                .build();

        // Setup OrderDto
        OrderDto orderDto = OrderDto.builder()
                .orderId(1)
                .orderDesc("Test order")
                .orderStatus("PENDING")
                .orderFee(99.99)
                .build();

        // Setup CartDto
        cartDto = CartDto.builder()
                .cartId(1)
                .userId(1)
                .userDto(userDto)
                .orderDtos(new HashSet<>(Arrays.asList(orderDto)))
                .build();

        // Setup Collection Response
        Collection<CartDto> carts = Arrays.asList(cartDto);
        collectionResponse = CartOrderServiceDtoCollectionResponse.builder()
                .collection(carts)
                .build();
    }

    @Test
    @DisplayName("Should find all carts successfully")
    void findAll_ShouldReturnAllCarts_WhenCalled() {
        // Given
        ResponseEntity<CartOrderServiceDtoCollectionResponse> serviceResponse = 
            new ResponseEntity<>(collectionResponse, HttpStatus.OK);
        when(cartClientService.findAll()).thenReturn(serviceResponse);

        // When
        ResponseEntity<CartOrderServiceDtoCollectionResponse> result = cartController.findAll();

        // Then
        assertNotNull(result);
        assertEquals(HttpStatus.OK, result.getStatusCode());
        assertEquals(collectionResponse, result.getBody());
        verify(cartClientService, times(1)).findAll();
    }

    @Test
    @DisplayName("Should find cart by ID successfully when user is authorized")
    void findById_ShouldReturnCart_WhenUserIsAuthorized() {
        // Given
        String cartId = "1";
        String userId = "1";
        ResponseEntity<CartDto> serviceResponse = new ResponseEntity<>(cartDto, HttpStatus.OK);
        
        when(authUtil.getOwner(cartId, ResourceType.CARTS)).thenReturn(userId);
        doNothing().when(authUtil).canActivate(request, userId, userDetails);
        when(cartClientService.findById(cartId)).thenReturn(serviceResponse);

        // When
        ResponseEntity<CartDto> result = cartController.findById(cartId, request, userDetails);

        // Then
        assertNotNull(result);
        assertEquals(HttpStatus.OK, result.getStatusCode());
        assertEquals(cartDto, result.getBody());
        verify(authUtil, times(1)).getOwner(cartId, ResourceType.CARTS);
        verify(authUtil, times(1)).canActivate(request, userId, userDetails);
        verify(cartClientService, times(1)).findById(cartId);
    }

    @Test
    @DisplayName("Should throw UnauthorizedException when user is not authorized to find cart by ID")
    void findById_ShouldThrowUnauthorizedException_WhenUserIsNotAuthorized() {
        // Given
        String cartId = "1";
        String userId = "1";
        
        when(authUtil.getOwner(cartId, ResourceType.CARTS)).thenReturn(userId);
        doThrow(new UnauthorizedException("You can access to resources of your own"))
            .when(authUtil).canActivate(request, userId, userDetails);

        // When & Then
        UnauthorizedException exception = assertThrows(UnauthorizedException.class, () -> {
            cartController.findById(cartId, request, userDetails);
        });
        
        assertEquals("You can access to resources of your own", exception.getMessage());
        verify(authUtil, times(1)).getOwner(cartId, ResourceType.CARTS);
        verify(authUtil, times(1)).canActivate(request, userId, userDetails);
        verify(cartClientService, times(0)).findById(anyString());
    }

    @Test
    @DisplayName("Should save cart successfully when user is authorized")
    void save_ShouldReturnSavedCart_WhenUserIsAuthorized() {
        // Given
        String userId = "1";
        ResponseEntity<CartDto> serviceResponse = new ResponseEntity<>(cartDto, HttpStatus.OK);
        
        doNothing().when(authUtil).canActivate(request, userId, userDetails);
        when(cartClientService.save(cartDto)).thenReturn(serviceResponse);

        // When
        ResponseEntity<CartDto> result = cartController.save(cartDto, request, userDetails);

        // Then
        assertNotNull(result);
        assertEquals(HttpStatus.OK, result.getStatusCode());
        assertEquals(cartDto, result.getBody());
        verify(authUtil, times(1)).canActivate(request, userId, userDetails);
        verify(cartClientService, times(1)).save(cartDto);
    }

    @Test
    @DisplayName("Should throw UnauthorizedException when user is not authorized to save cart")
    void save_ShouldThrowUnauthorizedException_WhenUserIsNotAuthorized() {
        // Given
        String userId = "1";
        
        doThrow(new UnauthorizedException("You can access to resources of your own"))
            .when(authUtil).canActivate(request, userId, userDetails);

        // When & Then
        UnauthorizedException exception = assertThrows(UnauthorizedException.class, () -> {
            cartController.save(cartDto, request, userDetails);
        });
        
        assertEquals("You can access to resources of your own", exception.getMessage());
        verify(authUtil, times(1)).canActivate(request, userId, userDetails);
        verify(cartClientService, times(0)).save(any(CartDto.class));
    }

    @Test
    @DisplayName("Should delete cart successfully when user is authorized")
    void deleteById_ShouldReturnTrue_WhenUserIsAuthorizedAndDeletionSuccessful() {
        // Given
        String cartId = "1";
        String userId = "1";
        ResponseEntity<Boolean> serviceResponse = new ResponseEntity<>(true, HttpStatus.OK);
        
        when(authUtil.getOwner(cartId, ResourceType.CARTS)).thenReturn(userId);
        doNothing().when(authUtil).canActivate(request, userId, userDetails);
        when(cartClientService.deleteById(cartId)).thenReturn(serviceResponse);

        // When
        ResponseEntity<Boolean> result = cartController.deleteById(cartId, request, userDetails);

        // Then
        assertNotNull(result);
        assertEquals(HttpStatus.OK, result.getStatusCode());
        assertEquals(true, result.getBody());
        verify(authUtil, times(1)).getOwner(cartId, ResourceType.CARTS);
        verify(authUtil, times(1)).canActivate(request, userId, userDetails);
        verify(cartClientService, times(1)).deleteById(cartId);
    }

    @Test
    @DisplayName("Should throw UnauthorizedException when user is not authorized to delete cart")
    void deleteById_ShouldThrowUnauthorizedException_WhenUserIsNotAuthorized() {
        // Given
        String cartId = "1";
        String userId = "1";
        
        when(authUtil.getOwner(cartId, ResourceType.CARTS)).thenReturn(userId);
        doThrow(new UnauthorizedException("You can access to resources of your own"))
            .when(authUtil).canActivate(request, userId, userDetails);

        // When & Then
        UnauthorizedException exception = assertThrows(UnauthorizedException.class, () -> {
            cartController.deleteById(cartId, request, userDetails);
        });
        
        assertEquals("You can access to resources of your own", exception.getMessage());
        verify(authUtil, times(1)).getOwner(cartId, ResourceType.CARTS);
        verify(authUtil, times(1)).canActivate(request, userId, userDetails);
        verify(cartClientService, times(0)).deleteById(anyString());
    }

    @Test
    @DisplayName("Should handle null response body from service")
    void findAll_ShouldHandleNullResponseBody_WhenServiceReturnsNullBody() {
        // Given
        ResponseEntity<CartOrderServiceDtoCollectionResponse> serviceResponse = 
            new ResponseEntity<>(null, HttpStatus.OK);
        when(cartClientService.findAll()).thenReturn(serviceResponse);

        // When
        ResponseEntity<CartOrderServiceDtoCollectionResponse> result = cartController.findAll();

        // Then
        assertNotNull(result);
        assertEquals(HttpStatus.OK, result.getStatusCode());
        assertEquals(null, result.getBody());
        verify(cartClientService, times(1)).findAll();
    }

    @Test
    @DisplayName("Should handle empty cart collection")
    void findAll_ShouldHandleEmptyCollection_WhenNoCartsExist() {
        // Given
        Collection<CartDto> emptyCarts = Arrays.asList();
        CartOrderServiceDtoCollectionResponse emptyResponse = CartOrderServiceDtoCollectionResponse.builder()
                .collection(emptyCarts)
                .build();
        ResponseEntity<CartOrderServiceDtoCollectionResponse> serviceResponse = 
            new ResponseEntity<>(emptyResponse, HttpStatus.OK);
        when(cartClientService.findAll()).thenReturn(serviceResponse);

        // When
        ResponseEntity<CartOrderServiceDtoCollectionResponse> result = cartController.findAll();

        // Then
        assertNotNull(result);
        assertEquals(HttpStatus.OK, result.getStatusCode());
        assertNotNull(result.getBody());
        assertEquals(0, result.getBody().getCollection().size());
        verify(cartClientService, times(1)).findAll();
    }

    @Test
    @DisplayName("Should handle cart with null user ID")
    void save_ShouldThrowException_WhenUserIdIsNull() {
        // Given
        CartDto cartWithNullUserId = CartDto.builder()
                .cartId(1)
                .userId(null)
                .build();

        // When & Then
        assertThrows(NullPointerException.class, () -> {
            cartController.save(cartWithNullUserId, request, userDetails);
        });

        verify(cartClientService, times(0)).save(any(CartDto.class));
    }
}