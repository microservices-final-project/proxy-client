package com.selimhorri.app.business.orderItem;

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
import com.selimhorri.app.business.orderItem.controller.OrderItemController;
import com.selimhorri.app.business.orderItem.model.OrderDto;
import com.selimhorri.app.business.orderItem.model.OrderItemDto;
import com.selimhorri.app.business.orderItem.model.ProductDto;
import com.selimhorri.app.business.orderItem.model.response.OrderItemOrderItemServiceDtoCollectionResponse;
import com.selimhorri.app.business.orderItem.service.OrderItemClientService;
import com.selimhorri.app.exception.wrapper.UnauthorizedException;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collection;

@ExtendWith(MockitoExtension.class)
@DisplayName("OrderItemController Unit Tests")
class OrderItemControllerTest {

    @Mock
    private AuthUtil authUtil;

    @Mock
    private OrderItemClientService orderItemClientService;

    @Mock
    private HttpServletRequest request;

    @Mock
    private UserDetails userDetails;

    private OrderItemController orderItemController;
    private OrderItemDto orderItemDto;
    private OrderDto orderDto;
    private ProductDto productDto;
    private OrderItemOrderItemServiceDtoCollectionResponse collectionResponse;

    @BeforeEach
    void setUp() {
        // Create controller and inject mocks
        orderItemController = new OrderItemController(orderItemClientService);
        ReflectionTestUtils.setField(orderItemController, "authUtil", authUtil);
        
        // Setup ProductDto
        productDto = ProductDto.builder()
                .productId(1)
                .productTitle("Test Product")
                .imageUrl("http://test.com/image.jpg")
                .sku("TEST123")
                .priceUnit(99.99)
                .quantity(10)
                .build();

        // Setup OrderDto
        orderDto = OrderDto.builder()
                .orderId(1)
                .orderDate(LocalDateTime.now())
                .orderDesc("Test Order")
                .orderFee(100.0)
                .orderStatus("CREATED")
                .build();

        // Setup OrderItemDto
        orderItemDto = OrderItemDto.builder()
                .productId(1)
                .orderId(1)
                .orderedQuantity(2)
                .productDto(productDto)
                .orderDto(orderDto)
                .build();

        // Setup Collection Response
        Collection<OrderItemDto> orderItems = Arrays.asList(orderItemDto);
        collectionResponse = OrderItemOrderItemServiceDtoCollectionResponse.builder()
                .collection(orderItems)
                .build();
    }

    @Test
    @DisplayName("Should find all order items successfully")
    void findAll_ShouldReturnAllOrderItems_WhenCalled() {
        // Given
        ResponseEntity<OrderItemOrderItemServiceDtoCollectionResponse> serviceResponse = 
            new ResponseEntity<>(collectionResponse, HttpStatus.OK);
        when(orderItemClientService.findAll()).thenReturn(serviceResponse);

        // When
        ResponseEntity<OrderItemOrderItemServiceDtoCollectionResponse> result = orderItemController.findAll();

        // Then
        assertNotNull(result);
        assertEquals(HttpStatus.OK, result.getStatusCode());
        assertEquals(collectionResponse, result.getBody());
        verify(orderItemClientService, times(1)).findAll();
    }

    @Test
    @DisplayName("Should find order item by order ID successfully when user is authorized")
    void findById_ShouldReturnOrderItem_WhenUserIsAuthorized() {
        // Given
        String orderId = "1";
        String userId = "1";
        ResponseEntity<OrderItemDto> serviceResponse = new ResponseEntity<>(orderItemDto, HttpStatus.OK);
        
        when(authUtil.getOwner(orderId, ResourceType.ORDERS)).thenReturn(userId);
        doNothing().when(authUtil).canActivate(request, userId, userDetails);
        when(orderItemClientService.findById(orderId)).thenReturn(serviceResponse);

        // When
        ResponseEntity<OrderItemDto> result = orderItemController.findById(orderId, request, userDetails);

        // Then
        assertNotNull(result);
        assertEquals(HttpStatus.OK, result.getStatusCode());
        assertEquals(orderItemDto, result.getBody());
        verify(authUtil, times(1)).getOwner(orderId, ResourceType.ORDERS);
        verify(authUtil, times(1)).canActivate(request, userId, userDetails);
        verify(orderItemClientService, times(1)).findById(orderId);
    }

    @Test
    @DisplayName("Should throw UnauthorizedException when user is not authorized to find order item")
    void findById_ShouldThrowUnauthorizedException_WhenUserIsNotAuthorized() {
        // Given
        String orderId = "1";
        String userId = "1";
        
        when(authUtil.getOwner(orderId, ResourceType.ORDERS)).thenReturn(userId);
        doThrow(new UnauthorizedException("You can access to resources of your own"))
            .when(authUtil).canActivate(request, userId, userDetails);

        // When & Then
        UnauthorizedException exception = assertThrows(UnauthorizedException.class, () -> {
            orderItemController.findById(orderId, request, userDetails);
        });
        
        assertEquals("You can access to resources of your own", exception.getMessage());
        verify(authUtil, times(1)).getOwner(orderId, ResourceType.ORDERS);
        verify(authUtil, times(1)).canActivate(request, userId, userDetails);
        verify(orderItemClientService, times(0)).findById(anyString());
    }

    @Test
    @DisplayName("Should save order item successfully when user is authorized")
    void save_ShouldReturnSavedOrderItem_WhenUserIsAuthorized() {
        // Given
        String orderId = "1";
        String userId = "1";
        ResponseEntity<OrderItemDto> serviceResponse = new ResponseEntity<>(orderItemDto, HttpStatus.OK);
        
        when(authUtil.getOwner(orderId, ResourceType.ORDERS)).thenReturn(userId);
        doNothing().when(authUtil).canActivate(request, userId, userDetails);
        when(orderItemClientService.save(orderItemDto)).thenReturn(serviceResponse);

        // When
        ResponseEntity<OrderItemDto> result = orderItemController.save(orderItemDto, request, userDetails);

        // Then
        assertNotNull(result);
        assertEquals(HttpStatus.OK, result.getStatusCode());
        assertEquals(orderItemDto, result.getBody());
        verify(authUtil, times(1)).getOwner(orderId, ResourceType.ORDERS);
        verify(authUtil, times(1)).canActivate(request, userId, userDetails);
        verify(orderItemClientService, times(1)).save(orderItemDto);
    }

    @Test
    @DisplayName("Should throw UnauthorizedException when user is not authorized to save order item")
    void save_ShouldThrowUnauthorizedException_WhenUserIsNotAuthorized() {
        // Given
        String orderId = "1";
        String userId = "1";
        
        when(authUtil.getOwner(orderId, ResourceType.ORDERS)).thenReturn(userId);
        doThrow(new UnauthorizedException("You can access to resources of your own"))
            .when(authUtil).canActivate(request, userId, userDetails);

        // When & Then
        UnauthorizedException exception = assertThrows(UnauthorizedException.class, () -> {
            orderItemController.save(orderItemDto, request, userDetails);
        });
        
        assertEquals("You can access to resources of your own", exception.getMessage());
        verify(authUtil, times(1)).getOwner(orderId, ResourceType.ORDERS);
        verify(authUtil, times(1)).canActivate(request, userId, userDetails);
        verify(orderItemClientService, times(0)).save(any(OrderItemDto.class));
    }

    @Test
    @DisplayName("Should delete order item by order ID successfully when user is authorized")
    void deleteById_ShouldReturnTrue_WhenUserIsAuthorizedAndDeletionSuccessful() {
        // Given
        String orderId = "1";
        String userId = "1";
        ResponseEntity<Boolean> serviceResponse = new ResponseEntity<>(true, HttpStatus.OK);
        
        when(authUtil.getOwner(orderId, ResourceType.ORDERS)).thenReturn(userId);
        doNothing().when(authUtil).canActivate(request, userId, userDetails);
        when(orderItemClientService.deleteById(orderId)).thenReturn(serviceResponse);

        // When
        ResponseEntity<Boolean> result = orderItemController.deleteById(orderId, request, userDetails);

        // Then
        assertNotNull(result);
        assertEquals(HttpStatus.OK, result.getStatusCode());
        assertEquals(true, result.getBody());
        verify(authUtil, times(1)).getOwner(orderId, ResourceType.ORDERS);
        verify(authUtil, times(1)).canActivate(request, userId, userDetails);
        verify(orderItemClientService, times(1)).deleteById(orderId);
    }

    @Test
    @DisplayName("Should throw UnauthorizedException when user is not authorized to delete order item")
    void deleteById_ShouldThrowUnauthorizedException_WhenUserIsNotAuthorized() {
        // Given
        String orderId = "1";
        String userId = "1";
        
        when(authUtil.getOwner(orderId, ResourceType.ORDERS)).thenReturn(userId);
        doThrow(new UnauthorizedException("You can access to resources of your own"))
            .when(authUtil).canActivate(request, userId, userDetails);

        // When & Then
        UnauthorizedException exception = assertThrows(UnauthorizedException.class, () -> {
            orderItemController.deleteById(orderId, request, userDetails);
        });
        
        assertEquals("You can access to resources of your own", exception.getMessage());
        verify(authUtil, times(1)).getOwner(orderId, ResourceType.ORDERS);
        verify(authUtil, times(1)).canActivate(request, userId, userDetails);
        verify(orderItemClientService, times(0)).deleteById(anyString());
    }

    @Test
    @DisplayName("Should handle null userId from getOwner method")
    void findById_ShouldHandleNullUserId_WhenGetOwnerReturnsNull() {
        // Given
        String orderId = "1";
        ResponseEntity<OrderItemDto> serviceResponse = new ResponseEntity<>(orderItemDto, HttpStatus.OK);
        
        when(authUtil.getOwner(orderId, ResourceType.ORDERS)).thenReturn(null);
        doNothing().when(authUtil).canActivate(request, null, userDetails);
        when(orderItemClientService.findById(orderId)).thenReturn(serviceResponse);

        // When
        ResponseEntity<OrderItemDto> result = orderItemController.findById(orderId, request, userDetails);

        // Then
        assertNotNull(result);
        assertEquals(HttpStatus.OK, result.getStatusCode());
        assertEquals(orderItemDto, result.getBody());
        verify(authUtil, times(1)).getOwner(orderId, ResourceType.ORDERS);
        verify(authUtil, times(1)).canActivate(request, null, userDetails);
        verify(orderItemClientService, times(1)).findById(orderId);
    }

    @Test
    @DisplayName("Should handle service returning null response body")
    void findAll_ShouldHandleNullResponseBody_WhenServiceReturnsNullBody() {
        // Given
        ResponseEntity<OrderItemOrderItemServiceDtoCollectionResponse> serviceResponse = 
            new ResponseEntity<>(null, HttpStatus.OK);
        when(orderItemClientService.findAll()).thenReturn(serviceResponse);

        // When
        ResponseEntity<OrderItemOrderItemServiceDtoCollectionResponse> result = orderItemController.findAll();

        // Then
        assertNotNull(result);
        assertEquals(HttpStatus.OK, result.getStatusCode());
        assertEquals(null, result.getBody());
        verify(orderItemClientService, times(1)).findAll();
    }

    @Test
    @DisplayName("Should handle empty order item collection")
    void findAll_ShouldHandleEmptyCollection_WhenNoOrderItemsExist() {
        // Given
        Collection<OrderItemDto> emptyOrderItems = Arrays.asList();
        OrderItemOrderItemServiceDtoCollectionResponse emptyResponse = OrderItemOrderItemServiceDtoCollectionResponse.builder()
                .collection(emptyOrderItems)
                .build();
        ResponseEntity<OrderItemOrderItemServiceDtoCollectionResponse> serviceResponse = 
            new ResponseEntity<>(emptyResponse, HttpStatus.OK);
        when(orderItemClientService.findAll()).thenReturn(serviceResponse);

        // When
        ResponseEntity<OrderItemOrderItemServiceDtoCollectionResponse> result = orderItemController.findAll();

        // Then
        assertNotNull(result);
        assertEquals(HttpStatus.OK, result.getStatusCode());
        assertNotNull(result.getBody());
        assertEquals(0, result.getBody().getCollection().size());
        verify(orderItemClientService, times(1)).findAll();
    }
}