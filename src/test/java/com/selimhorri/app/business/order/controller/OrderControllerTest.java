package com.selimhorri.app.business.order.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import javax.servlet.http.HttpServletRequest;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collection;

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
import com.selimhorri.app.business.order.model.CartDto;
import com.selimhorri.app.business.order.model.OrderDto;
import com.selimhorri.app.business.order.model.response.OrderOrderServiceDtoCollectionResponse;
import com.selimhorri.app.business.order.service.OrderClientService;
import com.selimhorri.app.business.user.model.UserDto;
import com.selimhorri.app.exception.wrapper.UnauthorizedException;

@ExtendWith(MockitoExtension.class)
@DisplayName("OrderController Unit Tests")
class OrderControllerTest {

    @Mock
    private AuthUtil authUtil;

    @Mock
    private OrderClientService orderClientService;

    @Mock
    private HttpServletRequest request;

    @Mock
    private UserDetails userDetails;

    private OrderController orderController;
    private OrderDto orderDto;
    private UserDto userDto;
    private CartDto cartDto;
    private OrderOrderServiceDtoCollectionResponse collectionResponse;

    @BeforeEach
    void setUp() {
        // Create controller and inject mocks
        orderController = new OrderController(orderClientService);
        ReflectionTestUtils.setField(orderController, "authUtil", authUtil);
        
        // Setup UserDto
        userDto = UserDto.builder()
                .userId(1)
                .firstName("John")
                .lastName("Doe")
                .email("john.doe@example.com")
                .phone("123456789")
                .build();

        // Setup CartDto
        cartDto = CartDto.builder()
                .cartId(1)
                .userId(1)
                .build();

        // Setup OrderDto
        orderDto = OrderDto.builder()
                .orderId(1)
                .orderDate(LocalDateTime.now())
                .orderDesc("Test Order")
                .orderStatus("PENDING")
                .orderFee(100.0)
                .cartDto(cartDto)
                .build();

        // Setup Collection Response
        Collection<OrderDto> orders = Arrays.asList(orderDto);
        collectionResponse = OrderOrderServiceDtoCollectionResponse.builder()
                .collection(orders)
                .build();
    }

    @Test
    @DisplayName("Should find all orders successfully")
    void findAll_ShouldReturnAllOrders_WhenCalled() {
        // Given
        ResponseEntity<OrderOrderServiceDtoCollectionResponse> serviceResponse = 
            new ResponseEntity<>(collectionResponse, HttpStatus.OK);
        when(orderClientService.findAll()).thenReturn(serviceResponse);

        // When
        ResponseEntity<OrderOrderServiceDtoCollectionResponse> result = orderController.findAll();

        // Then
        assertNotNull(result);
        assertEquals(HttpStatus.OK, result.getStatusCode());
        assertEquals(collectionResponse, result.getBody());
        verify(orderClientService, times(1)).findAll();
    }

    @Test
    @DisplayName("Should find order by ID successfully when user is authorized")
    void findById_ShouldReturnOrder_WhenUserIsAuthorized() {
        // Given
        String orderId = "1";
        String userId = "1";
        ResponseEntity<OrderDto> serviceResponse = new ResponseEntity<>(orderDto, HttpStatus.OK);
        
        when(authUtil.getOwner(orderId, ResourceType.ORDERS)).thenReturn(userId);
        doNothing().when(authUtil).canActivate(request, userId, userDetails);
        when(orderClientService.findById(orderId)).thenReturn(serviceResponse);

        // When
        ResponseEntity<OrderDto> result = orderController.findById(orderId, request, userDetails);

        // Then
        assertNotNull(result);
        assertEquals(HttpStatus.OK, result.getStatusCode());
        assertEquals(orderDto, result.getBody());
        verify(authUtil, times(1)).getOwner(orderId, ResourceType.ORDERS);
        verify(authUtil, times(1)).canActivate(request, userId, userDetails);
        verify(orderClientService, times(1)).findById(orderId);
    }

    @Test
    @DisplayName("Should throw UnauthorizedException when user is not authorized to find order by ID")
    void findById_ShouldThrowUnauthorizedException_WhenUserIsNotAuthorized() {
        // Given
        String orderId = "1";
        String userId = "1";
        
        when(authUtil.getOwner(orderId, ResourceType.ORDERS)).thenReturn(userId);
        doThrow(new UnauthorizedException("You can access to resources of your own"))
            .when(authUtil).canActivate(request, userId, userDetails);

        // When & Then
        UnauthorizedException exception = assertThrows(UnauthorizedException.class, () -> {
            orderController.findById(orderId, request, userDetails);
        });
        
        assertEquals("You can access to resources of your own", exception.getMessage());
        verify(authUtil, times(1)).getOwner(orderId, ResourceType.ORDERS);
        verify(authUtil, times(1)).canActivate(request, userId, userDetails);
        verify(orderClientService, times(0)).findById(anyString());
    }

    @Test
    @DisplayName("Should save order successfully when user is authorized")
    void save_ShouldReturnSavedOrder_WhenUserIsAuthorized() {
        // Given
        String userId = "1";
        ResponseEntity<OrderDto> serviceResponse = new ResponseEntity<>(orderDto, HttpStatus.OK);
        
        when(authUtil.getOwner(orderDto.getCartDto().getCartId().toString(), ResourceType.CARTS)).thenReturn(userId);
        doNothing().when(authUtil).canActivate(request, userId, userDetails);
        when(orderClientService.save(orderDto)).thenReturn(serviceResponse);

        // When
        ResponseEntity<OrderDto> result = orderController.save(orderDto, request, userDetails);

        // Then
        assertNotNull(result);
        assertEquals(HttpStatus.OK, result.getStatusCode());
        assertEquals(orderDto, result.getBody());
        verify(authUtil, times(1)).getOwner(orderDto.getCartDto().getCartId().toString(), ResourceType.CARTS);
        verify(authUtil, times(1)).canActivate(request, userId, userDetails);
        verify(orderClientService, times(1)).save(orderDto);
    }

    @Test
    @DisplayName("Should throw UnauthorizedException when user is not authorized to save order")
    void save_ShouldThrowUnauthorizedException_WhenUserIsNotAuthorized() {
        // Given
        String userId = "1";
        
        when(authUtil.getOwner(orderDto.getCartDto().getCartId().toString(), ResourceType.CARTS)).thenReturn(userId);
        doThrow(new UnauthorizedException("You can access to resources of your own"))
            .when(authUtil).canActivate(request, userId, userDetails);

        // When & Then
        UnauthorizedException exception = assertThrows(UnauthorizedException.class, () -> {
            orderController.save(orderDto, request, userDetails);
        });
        
        assertEquals("You can access to resources of your own", exception.getMessage());
        verify(authUtil, times(1)).getOwner(orderDto.getCartDto().getCartId().toString(), ResourceType.CARTS);
        verify(authUtil, times(1)).canActivate(request, userId, userDetails);
        verify(orderClientService, times(0)).save(any(OrderDto.class));
    }

    @Test
    @DisplayName("Should update order status successfully")
    void updateStatus_ShouldReturnUpdatedOrder_WhenCalled() {
        // Given
        int orderId = 1;
        ResponseEntity<OrderDto> serviceResponse = new ResponseEntity<>(orderDto, HttpStatus.OK);
        
        when(orderClientService.updateStatus(orderId)).thenReturn(serviceResponse);

        // When
        ResponseEntity<OrderDto> result = orderController.update(orderId);

        // Then
        assertNotNull(result);
        assertEquals(HttpStatus.OK, result.getStatusCode());
        assertEquals(orderDto, result.getBody());
        verify(orderClientService, times(1)).updateStatus(orderId);
    }

    @Test
    @DisplayName("Should update order successfully when user is authorized")
    void update_ShouldReturnUpdatedOrder_WhenUserIsAuthorized() {
        // Given
        String orderId = "1";
        String userId = "1";
        ResponseEntity<OrderDto> serviceResponse = new ResponseEntity<>(orderDto, HttpStatus.OK);
        
        when(authUtil.getOwner(orderId, ResourceType.ORDERS)).thenReturn(userId);
        doNothing().when(authUtil).canActivate(request, userId, userDetails);
        when(orderClientService.update(orderId, orderDto)).thenReturn(serviceResponse);

        // When
        ResponseEntity<OrderDto> result = orderController.update(orderId, orderDto, request, userDetails);

        // Then
        assertNotNull(result);
        assertEquals(HttpStatus.OK, result.getStatusCode());
        assertEquals(orderDto, result.getBody());
        verify(authUtil, times(1)).getOwner(orderId, ResourceType.ORDERS);
        verify(authUtil, times(1)).canActivate(request, userId, userDetails);
        verify(orderClientService, times(1)).update(orderId, orderDto);
    }

    @Test
    @DisplayName("Should throw UnauthorizedException when user is not authorized to update order")
    void update_ShouldThrowUnauthorizedException_WhenUserIsNotAuthorized() {
        // Given
        String orderId = "1";
        String userId = "1";
        
        when(authUtil.getOwner(orderId, ResourceType.ORDERS)).thenReturn(userId);
        doThrow(new UnauthorizedException("You can access to resources of your own"))
            .when(authUtil).canActivate(request, userId, userDetails);

        // When & Then
        UnauthorizedException exception = assertThrows(UnauthorizedException.class, () -> {
            orderController.update(orderId, orderDto, request, userDetails);
        });
        
        assertEquals("You can access to resources of your own", exception.getMessage());
        verify(authUtil, times(1)).getOwner(orderId, ResourceType.ORDERS);
        verify(authUtil, times(1)).canActivate(request, userId, userDetails);
        verify(orderClientService, times(0)).update(anyString(), any(OrderDto.class));
    }

    @Test
    @DisplayName("Should delete order successfully when user is authorized")
    void deleteById_ShouldReturnTrue_WhenUserIsAuthorizedAndDeletionSuccessful() {
        // Given
        String orderId = "1";
        String userId = "1";
        ResponseEntity<Boolean> serviceResponse = new ResponseEntity<>(true, HttpStatus.OK);
        
        when(authUtil.getOwner(orderId, ResourceType.ORDERS)).thenReturn(userId);
        doNothing().when(authUtil).canActivate(request, userId, userDetails);
        when(orderClientService.deleteById(orderId)).thenReturn(serviceResponse);

        // When
        ResponseEntity<Boolean> result = orderController.deleteById(orderId, request, userDetails);

        // Then
        assertNotNull(result);
        assertEquals(HttpStatus.OK, result.getStatusCode());
        assertTrue(result.getBody());
        verify(authUtil, times(1)).getOwner(orderId, ResourceType.ORDERS);
        verify(authUtil, times(1)).canActivate(request, userId, userDetails);
        verify(orderClientService, times(1)).deleteById(orderId);
    }

    @Test
    @DisplayName("Should throw UnauthorizedException when user is not authorized to delete order")
    void deleteById_ShouldThrowUnauthorizedException_WhenUserIsNotAuthorized() {
        // Given
        String orderId = "1";
        String userId = "1";
        
        when(authUtil.getOwner(orderId, ResourceType.ORDERS)).thenReturn(userId);
        doThrow(new UnauthorizedException("You can access to resources of your own"))
            .when(authUtil).canActivate(request, userId, userDetails);

        // When & Then
        UnauthorizedException exception = assertThrows(UnauthorizedException.class, () -> {
            orderController.deleteById(orderId, request, userDetails);
        });
        
        assertEquals("You can access to resources of your own", exception.getMessage());
        verify(authUtil, times(1)).getOwner(orderId, ResourceType.ORDERS);
        verify(authUtil, times(1)).canActivate(request, userId, userDetails);
        verify(orderClientService, times(0)).deleteById(anyString());
    }

    @Test
    @DisplayName("Should handle null userId from getOwner method")
    void findById_ShouldHandleNullUserId_WhenGetOwnerReturnsNull() {
        // Given
        String orderId = "1";
        ResponseEntity<OrderDto> serviceResponse = new ResponseEntity<>(orderDto, HttpStatus.OK);
        
        when(authUtil.getOwner(orderId, ResourceType.ORDERS)).thenReturn(null);
        doNothing().when(authUtil).canActivate(request, null, userDetails);
        when(orderClientService.findById(orderId)).thenReturn(serviceResponse);

        // When
        ResponseEntity<OrderDto> result = orderController.findById(orderId, request, userDetails);

        // Then
        assertNotNull(result);
        assertEquals(HttpStatus.OK, result.getStatusCode());
        assertEquals(orderDto, result.getBody());
        verify(authUtil, times(1)).getOwner(orderId, ResourceType.ORDERS);
        verify(authUtil, times(1)).canActivate(request, null, userDetails);
        verify(orderClientService, times(1)).findById(orderId);
    }

    @Test
    @DisplayName("Should handle service returning null response body")
    void findAll_ShouldHandleNullResponseBody_WhenServiceReturnsNullBody() {
        // Given
        ResponseEntity<OrderOrderServiceDtoCollectionResponse> serviceResponse = 
            new ResponseEntity<>(null, HttpStatus.OK);
        when(orderClientService.findAll()).thenReturn(serviceResponse);

        // When
        ResponseEntity<OrderOrderServiceDtoCollectionResponse> result = orderController.findAll();

        // Then
        assertNotNull(result);
        assertEquals(HttpStatus.OK, result.getStatusCode());
        assertEquals(null, result.getBody());
        verify(orderClientService, times(1)).findAll();
    }

    @Test
    @DisplayName("Should handle empty order collection")
    void findAll_ShouldHandleEmptyCollection_WhenNoOrdersExist() {
        // Given
        Collection<OrderDto> emptyOrders = Arrays.asList();
        OrderOrderServiceDtoCollectionResponse emptyResponse = OrderOrderServiceDtoCollectionResponse.builder()
                .collection(emptyOrders)
                .build();
        ResponseEntity<OrderOrderServiceDtoCollectionResponse> serviceResponse = 
            new ResponseEntity<>(emptyResponse, HttpStatus.OK);
        when(orderClientService.findAll()).thenReturn(serviceResponse);

        // When
        ResponseEntity<OrderOrderServiceDtoCollectionResponse> result = orderController.findAll();

        // Then
        assertNotNull(result);
        assertEquals(HttpStatus.OK, result.getStatusCode());
        assertNotNull(result.getBody());
        assertEquals(0, result.getBody().getCollection().size());
        verify(orderClientService, times(1)).findAll();
    }

    @Test
    @DisplayName("Should handle order with null cart")
    void save_ShouldThrowNullPointerException_WhenCartDtoIsNull() {
        // Given
        OrderDto orderWithNullCart = OrderDto.builder()
                .orderId(1)
                .orderDate(LocalDateTime.now())
                .orderDesc("Test Order")
                .orderStatus("PENDING")
                .orderFee(100.0)
                .cartDto(null)
                .build();

        // When & Then
        assertThrows(NullPointerException.class, () -> {
            orderController.save(orderWithNullCart, request, userDetails);
        });

        verify(orderClientService, times(0)).save(any(OrderDto.class));
    }

    @Test
    @DisplayName("Should handle cart with null cartId")
    void save_ShouldThrowNullPointerException_WhenCartIdIsNull() {
        // Given
        com.selimhorri.app.business.order.model.CartDto cartWithNullId = CartDto.builder()
                .cartId(null)
                .userId(1)
                .build();
        
        OrderDto orderWithNullCartId = OrderDto.builder()
                .orderId(1)
                .orderDate(LocalDateTime.now())
                .orderDesc("Test Order")
                .orderStatus("PENDING")
                .orderFee(100.0)
                .cartDto(cartWithNullId)
                .build();

        // When & Then
        assertThrows(NullPointerException.class, () -> {
            orderController.save(orderWithNullCartId, request, userDetails);
        });

        verify(orderClientService, times(0)).save(any(OrderDto.class));
    }
}