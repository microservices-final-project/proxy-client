package com.selimhorri.app.business.order.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import com.selimhorri.app.business.order.model.CartDto;
import com.selimhorri.app.business.order.model.OrderDto;
import com.selimhorri.app.business.order.model.UserDto;
import com.selimhorri.app.business.order.model.response.CartOrderServiceDtoCollectionResponse;
import com.selimhorri.app.business.order.service.CartClientService;

@ExtendWith(MockitoExtension.class)
@DisplayName("CartController - Unit Tests")
class CartControllerTest {

    @Mock
    private CartClientService cartClientService;

    @InjectMocks
    private CartController cartController;

    private CartDto cartDto;
    private CartOrderServiceDtoCollectionResponse collectionResponse;
    private UserDto userDto;
    private OrderDto orderDto;

    @BeforeEach
    void setUp() {
        // Setup UserDto
        userDto = UserDto.builder()
                .userId(1)
                .firstName("John")
                .lastName("Doe")
                .email("john.doe@example.com")
                .phone("1234567890")
                .imageUrl("http://example.com/image.jpg")
                .build();

        // Setup OrderDto
        orderDto = OrderDto.builder()
                .orderId(1)
                .orderDesc("Test Order")
                .orderStatus("PENDING")
                .orderFee(100.0)
                .build();

        // Setup CartDto
        Set<OrderDto> orders = new HashSet<>();
        orders.add(orderDto);
        
        cartDto = CartDto.builder()
                .cartId(1)
                .userId(1)
                .userDto(userDto)
                .orderDtos(orders)
                .build();

        // Setup Collection Response
        Collection<CartDto> carts = Arrays.asList(cartDto);
        collectionResponse = CartOrderServiceDtoCollectionResponse.builder()
                .collection(carts)
                .build();
    }

    @Test
    @DisplayName("Should find all carts successfully")
    void testFindAll_Success() {
        // Given
        ResponseEntity<CartOrderServiceDtoCollectionResponse> mockResponse = 
            new ResponseEntity<>(collectionResponse, HttpStatus.OK);
        when(cartClientService.findAll()).thenReturn(mockResponse);

        // When
        ResponseEntity<CartOrderServiceDtoCollectionResponse> result = cartController.findAll();

        // Then
        assertNotNull(result);
        assertEquals(HttpStatus.OK, result.getStatusCode());
        assertNotNull(result.getBody());
        assertEquals(1, result.getBody().getCollection().size());
        
        CartDto firstCart = result.getBody().getCollection().iterator().next();
        assertEquals(1, firstCart.getCartId());
        assertEquals(1, firstCart.getUserId());
        
        verify(cartClientService, times(1)).findAll();
    }

    @Test
    @DisplayName("Should find cart by ID successfully")
    void testFindById_Success() {
        // Given
        String cartId = "1";
        ResponseEntity<CartDto> mockResponse = new ResponseEntity<>(cartDto, HttpStatus.OK);
        when(cartClientService.findById(cartId)).thenReturn(mockResponse);

        // When
        ResponseEntity<CartDto> result = cartController.findById(cartId);

        // Then
        assertNotNull(result);
        assertEquals(HttpStatus.OK, result.getStatusCode());
        assertNotNull(result.getBody());
        assertEquals(1, result.getBody().getCartId());
        assertEquals(1, result.getBody().getUserId());
        assertEquals("John", result.getBody().getUserDto().getFirstName());
        assertEquals("Doe", result.getBody().getUserDto().getLastName());
        assertEquals(1, result.getBody().getOrderDtos().size());
        
        verify(cartClientService, times(1)).findById(cartId);
    }

    @Test
    @DisplayName("Should find cart by ID with different cart ID")
    void testFindById_DifferentCartId() {
        // Given
        String cartId = "999";
        CartDto differentCart = CartDto.builder()
                .cartId(999)
                .userId(2)
                .build();
        ResponseEntity<CartDto> mockResponse = new ResponseEntity<>(differentCart, HttpStatus.OK);
        when(cartClientService.findById(cartId)).thenReturn(mockResponse);

        // When
        ResponseEntity<CartDto> result = cartController.findById(cartId);

        // Then
        assertNotNull(result);
        assertEquals(HttpStatus.OK, result.getStatusCode());
        assertEquals(999, result.getBody().getCartId());
        assertEquals(2, result.getBody().getUserId());
        
        verify(cartClientService, times(1)).findById(cartId);
    }

    @Test
    @DisplayName("Should save cart successfully")
    void testSave_Success() {
        // Given
        ResponseEntity<CartDto> mockResponse = new ResponseEntity<>(cartDto, HttpStatus.OK);
        when(cartClientService.save(any(CartDto.class))).thenReturn(mockResponse);

        // When
        ResponseEntity<CartDto> result = cartController.save(cartDto);

        // Then
        assertNotNull(result);
        assertEquals(HttpStatus.OK, result.getStatusCode());
        assertNotNull(result.getBody());
        assertEquals(1, result.getBody().getCartId());
        assertEquals(1, result.getBody().getUserId());
        assertEquals("john.doe@example.com", result.getBody().getUserDto().getEmail());
        
        verify(cartClientService, times(1)).save(cartDto);
    }

    @Test
    @DisplayName("Should save cart with minimal data")
    void testSave_MinimalData() {
        // Given
        CartDto minimalCart = CartDto.builder()
                .userId(2)
                .build();
        
        CartDto savedCart = CartDto.builder()
                .cartId(2)
                .userId(2)
                .build();
        
        ResponseEntity<CartDto> mockResponse = new ResponseEntity<>(savedCart, HttpStatus.OK);
        when(cartClientService.save(any(CartDto.class))).thenReturn(mockResponse);

        // When
        ResponseEntity<CartDto> result = cartController.save(minimalCart);

        // Then
        assertNotNull(result);
        assertEquals(HttpStatus.OK, result.getStatusCode());
        assertEquals(2, result.getBody().getCartId());
        assertEquals(2, result.getBody().getUserId());
        
        verify(cartClientService, times(1)).save(minimalCart);
    }

    @Test
    @DisplayName("Should delete cart by ID successfully")
    void testDeleteById_Success() {
        // Given
        String cartId = "1";
        ResponseEntity<Boolean> mockResponse = new ResponseEntity<>(true, HttpStatus.OK);
        when(cartClientService.deleteById(cartId)).thenReturn(mockResponse);

        // When
        ResponseEntity<Boolean> result = cartController.deleteById(cartId);

        // Then
        assertNotNull(result);
        assertEquals(HttpStatus.OK, result.getStatusCode());
        assertTrue(result.getBody());
        
        verify(cartClientService, times(1)).deleteById(cartId);
    }

    @Test
    @DisplayName("Should delete cart by ID and return true regardless of service response")
    void testDeleteById_AlwaysReturnsTrue() {
        // Given
        String cartId = "999";
        ResponseEntity<Boolean> mockResponse = new ResponseEntity<>(false, HttpStatus.OK);
        when(cartClientService.deleteById(cartId)).thenReturn(mockResponse);

        // When
        ResponseEntity<Boolean> result = cartController.deleteById(cartId);

        // Then
        assertNotNull(result);
        assertEquals(HttpStatus.OK, result.getStatusCode());
        assertTrue(result.getBody()); // Controller always returns true
        
        verify(cartClientService, times(1)).deleteById(cartId);
    }

    @Test
    @DisplayName("Should handle empty collection response")
    void testFindAll_EmptyCollection() {
        // Given
        CartOrderServiceDtoCollectionResponse emptyResponse = 
            CartOrderServiceDtoCollectionResponse.builder()
                .collection(Arrays.asList())
                .build();
        ResponseEntity<CartOrderServiceDtoCollectionResponse> mockResponse = 
            new ResponseEntity<>(emptyResponse, HttpStatus.OK);
        when(cartClientService.findAll()).thenReturn(mockResponse);

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
    @DisplayName("Should handle cart with multiple orders")
    void testFindById_CartWithMultipleOrders() {
        // Given
        OrderDto order1 = OrderDto.builder()
                .orderId(1)
                .orderDesc("First Order")
                .orderFee(50.0)
                .build();
        
        OrderDto order2 = OrderDto.builder()
                .orderId(2)
                .orderDesc("Second Order")
                .orderFee(75.0)
                .build();

        Set<OrderDto> orders = new HashSet<>();
        orders.add(order1);
        orders.add(order2);

        CartDto cartWithMultipleOrders = CartDto.builder()
                .cartId(1)
                .userId(1)
                .orderDtos(orders)
                .build();

        String cartId = "1";
        ResponseEntity<CartDto> mockResponse = new ResponseEntity<>(cartWithMultipleOrders, HttpStatus.OK);
        when(cartClientService.findById(cartId)).thenReturn(mockResponse);

        // When
        ResponseEntity<CartDto> result = cartController.findById(cartId);

        // Then
        assertNotNull(result);
        assertEquals(HttpStatus.OK, result.getStatusCode());
        assertEquals(2, result.getBody().getOrderDtos().size());
        
        verify(cartClientService, times(1)).findById(cartId);
    }

    @Test
    @DisplayName("Should verify service method calls with correct parameters")
    void testServiceMethodCalls() {
        // Given
        String cartId = "123";
        ResponseEntity<CartDto> findByIdResponse = new ResponseEntity<>(cartDto, HttpStatus.OK);
        ResponseEntity<CartDto> saveResponse = new ResponseEntity<>(cartDto, HttpStatus.OK);
        ResponseEntity<Boolean> deleteResponse = new ResponseEntity<>(true, HttpStatus.OK);
        ResponseEntity<CartOrderServiceDtoCollectionResponse> findAllResponse = 
            new ResponseEntity<>(collectionResponse, HttpStatus.OK);

        when(cartClientService.findById(cartId)).thenReturn(findByIdResponse);
        when(cartClientService.save(cartDto)).thenReturn(saveResponse);
        when(cartClientService.deleteById(cartId)).thenReturn(deleteResponse);
        when(cartClientService.findAll()).thenReturn(findAllResponse);

        // When
        cartController.findById(cartId);
        cartController.save(cartDto);
        cartController.deleteById(cartId);
        cartController.findAll();

        // Then
        verify(cartClientService, times(1)).findById(eq(cartId));
        verify(cartClientService, times(1)).save(eq(cartDto));
        verify(cartClientService, times(1)).deleteById(eq(cartId));
        verify(cartClientService, times(1)).findAll();
    }
}