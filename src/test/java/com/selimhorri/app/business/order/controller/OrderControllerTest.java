package com.selimhorri.app.business.order.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collection;

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
import com.selimhorri.app.business.order.model.response.OrderOrderServiceDtoCollectionResponse;
import com.selimhorri.app.business.order.service.OrderClientService;

@ExtendWith(MockitoExtension.class)
@DisplayName("OrderController - Unit Tests")
class OrderControllerTest {

    @Mock
    private OrderClientService orderClientService;

    @InjectMocks
    private OrderController orderController;

    private OrderDto orderDto;
    private OrderOrderServiceDtoCollectionResponse collectionResponse;
    private CartDto cartDto;

    @BeforeEach
    void setUp() {
        // Setup CartDto
        cartDto = CartDto.builder()
                .cartId(1)
                .userId(1)
                .build();

        // Setup OrderDto
        orderDto = OrderDto.builder()
                .orderId(1)
                .orderDate(LocalDateTime.now())
                .orderDesc("Test Order Description")
                .orderStatus("PENDING")
                .orderFee(99.99)
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
    void testFindAll_Success() {
        // Given
        ResponseEntity<OrderOrderServiceDtoCollectionResponse> mockResponse = 
            new ResponseEntity<>(collectionResponse, HttpStatus.OK);
        when(orderClientService.findAll()).thenReturn(mockResponse);

        // When
        ResponseEntity<OrderOrderServiceDtoCollectionResponse> result = orderController.findAll();

        // Then
        assertNotNull(result);
        assertEquals(HttpStatus.OK, result.getStatusCode());
        assertNotNull(result.getBody());
        assertEquals(1, result.getBody().getCollection().size());
        
        OrderDto firstOrder = result.getBody().getCollection().iterator().next();
        assertEquals(1, firstOrder.getOrderId());
        assertEquals("PENDING", firstOrder.getOrderStatus());
        assertEquals(99.99, firstOrder.getOrderFee());
        
        verify(orderClientService, times(1)).findAll();
    }

    @Test
    @DisplayName("Should find all orders with empty collection")
    void testFindAll_EmptyCollection() {
        // Given
        OrderOrderServiceDtoCollectionResponse emptyResponse = 
            OrderOrderServiceDtoCollectionResponse.builder()
                .collection(Arrays.asList())
                .build();
        ResponseEntity<OrderOrderServiceDtoCollectionResponse> mockResponse = 
            new ResponseEntity<>(emptyResponse, HttpStatus.OK);
        when(orderClientService.findAll()).thenReturn(mockResponse);

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
    @DisplayName("Should find order by ID successfully")
    void testFindById_Success() {
        // Given
        String orderId = "1";
        ResponseEntity<OrderDto> mockResponse = new ResponseEntity<>(orderDto, HttpStatus.OK);
        when(orderClientService.findById(orderId)).thenReturn(mockResponse);

        // When
        ResponseEntity<OrderDto> result = orderController.findById(orderId);

        // Then
        assertNotNull(result);
        assertEquals(HttpStatus.OK, result.getStatusCode());
        assertNotNull(result.getBody());
        assertEquals(1, result.getBody().getOrderId());
        assertEquals("Test Order Description", result.getBody().getOrderDesc());
        assertEquals("PENDING", result.getBody().getOrderStatus());
        assertEquals(99.99, result.getBody().getOrderFee());
        assertNotNull(result.getBody().getCartDto());
        assertEquals(1, result.getBody().getCartDto().getCartId());
        
        verify(orderClientService, times(1)).findById(orderId);
    }

    @Test
    @DisplayName("Should find order by ID with different order ID")
    void testFindById_DifferentOrderId() {
        // Given
        String orderId = "999";
        OrderDto differentOrder = OrderDto.builder()
                .orderId(999)
                .orderDesc("Different Order")
                .orderStatus("COMPLETED")
                .orderFee(150.0)
                .build();
        ResponseEntity<OrderDto> mockResponse = new ResponseEntity<>(differentOrder, HttpStatus.OK);
        when(orderClientService.findById(orderId)).thenReturn(mockResponse);

        // When
        ResponseEntity<OrderDto> result = orderController.findById(orderId);

        // Then
        assertNotNull(result);
        assertEquals(HttpStatus.OK, result.getStatusCode());
        assertEquals(999, result.getBody().getOrderId());
        assertEquals("Different Order", result.getBody().getOrderDesc());
        assertEquals("COMPLETED", result.getBody().getOrderStatus());
        assertEquals(150.0, result.getBody().getOrderFee());
        
        verify(orderClientService, times(1)).findById(orderId);
    }

    @Test
    @DisplayName("Should save order successfully")
    void testSave_Success() {
        // Given
        ResponseEntity<OrderDto> mockResponse = new ResponseEntity<>(orderDto, HttpStatus.OK);
        when(orderClientService.save(any(OrderDto.class))).thenReturn(mockResponse);

        // When
        ResponseEntity<OrderDto> result = orderController.save(orderDto);

        // Then
        assertNotNull(result);
        assertEquals(HttpStatus.OK, result.getStatusCode());
        assertNotNull(result.getBody());
        assertEquals(1, result.getBody().getOrderId());
        assertEquals("Test Order Description", result.getBody().getOrderDesc());
        assertEquals("PENDING", result.getBody().getOrderStatus());
        assertEquals(99.99, result.getBody().getOrderFee());
        
        verify(orderClientService, times(1)).save(orderDto);
    }

    @Test
    @DisplayName("Should save order with minimal data")
    void testSave_MinimalData() {
        // Given
        OrderDto minimalOrder = OrderDto.builder()
                .orderDesc("Minimal Order")
                .orderFee(25.0)
                .build();
        
        OrderDto savedOrder = OrderDto.builder()
                .orderId(2)
                .orderDesc("Minimal Order")
                .orderFee(25.0)
                .orderStatus("CREATED")
                .build();
        
        ResponseEntity<OrderDto> mockResponse = new ResponseEntity<>(savedOrder, HttpStatus.OK);
        when(orderClientService.save(any(OrderDto.class))).thenReturn(mockResponse);

        // When
        ResponseEntity<OrderDto> result = orderController.save(minimalOrder);

        // Then
        assertNotNull(result);
        assertEquals(HttpStatus.OK, result.getStatusCode());
        assertEquals(2, result.getBody().getOrderId());
        assertEquals("Minimal Order", result.getBody().getOrderDesc());
        assertEquals("CREATED", result.getBody().getOrderStatus());
        assertEquals(25.0, result.getBody().getOrderFee());
        
        verify(orderClientService, times(1)).save(minimalOrder);
    }

    @Test
    @DisplayName("Should update order status successfully")
    void testUpdateStatus_Success() {
        // Given
        int orderId = 1;
        OrderDto updatedOrder = OrderDto.builder()
                .orderId(1)
                .orderDesc("Test Order Description")
                .orderStatus("SHIPPED")
                .orderFee(99.99)
                .build();
        ResponseEntity<OrderDto> mockResponse = new ResponseEntity<>(updatedOrder, HttpStatus.OK);
        when(orderClientService.updateStatus(orderId)).thenReturn(mockResponse);

        // When
        ResponseEntity<OrderDto> result = orderController.update(orderId);

        // Then
        assertNotNull(result);
        assertEquals(HttpStatus.OK, result.getStatusCode());
        assertNotNull(result.getBody());
        assertEquals(1, result.getBody().getOrderId());
        assertEquals("SHIPPED", result.getBody().getOrderStatus());
        assertEquals(99.99, result.getBody().getOrderFee());
        
        verify(orderClientService, times(1)).updateStatus(orderId);
    }

    @Test
    @DisplayName("Should update order status with different order ID")
    void testUpdateStatus_DifferentOrderId() {
        // Given
        int orderId = 999;
        OrderDto updatedOrder = OrderDto.builder()
                .orderId(999)
                .orderStatus("DELIVERED")
                .build();
        ResponseEntity<OrderDto> mockResponse = new ResponseEntity<>(updatedOrder, HttpStatus.OK);
        when(orderClientService.updateStatus(orderId)).thenReturn(mockResponse);

        // When
        ResponseEntity<OrderDto> result = orderController.update(orderId);

        // Then
        assertNotNull(result);
        assertEquals(HttpStatus.OK, result.getStatusCode());
        assertEquals(999, result.getBody().getOrderId());
        assertEquals("DELIVERED", result.getBody().getOrderStatus());
        
        verify(orderClientService, times(1)).updateStatus(orderId);
    }

    @Test
    @DisplayName("Should update order completely successfully")
    void testUpdate_Success() {
        // Given
        String orderId = "1";
        OrderDto updatedOrder = OrderDto.builder()
                .orderId(1)
                .orderDesc("Updated Order Description")
                .orderStatus("PROCESSING")
                .orderFee(149.99)
                .build();
        ResponseEntity<OrderDto> mockResponse = new ResponseEntity<>(updatedOrder, HttpStatus.OK);
        when(orderClientService.update(orderId, orderDto)).thenReturn(mockResponse);

        // When
        ResponseEntity<OrderDto> result = orderController.update(orderId, orderDto);

        // Then
        assertNotNull(result);
        assertEquals(HttpStatus.OK, result.getStatusCode());
        assertNotNull(result.getBody());
        assertEquals(1, result.getBody().getOrderId());
        assertEquals("Updated Order Description", result.getBody().getOrderDesc());
        assertEquals("PROCESSING", result.getBody().getOrderStatus());
        assertEquals(149.99, result.getBody().getOrderFee());
        
        verify(orderClientService, times(1)).update(orderId, orderDto);
    }

    @Test
    @DisplayName("Should update order with different data")
    void testUpdate_DifferentData() {
        // Given
        String orderId = "2";
        OrderDto inputOrder = OrderDto.builder()
                .orderDesc("New Description")
                .orderFee(200.0)
                .build();
        
        OrderDto updatedOrder = OrderDto.builder()
                .orderId(2)
                .orderDesc("New Description")
                .orderStatus("UPDATED")
                .orderFee(200.0)
                .build();
        
        ResponseEntity<OrderDto> mockResponse = new ResponseEntity<>(updatedOrder, HttpStatus.OK);
        when(orderClientService.update(orderId, inputOrder)).thenReturn(mockResponse);

        // When
        ResponseEntity<OrderDto> result = orderController.update(orderId, inputOrder);

        // Then
        assertNotNull(result);
        assertEquals(HttpStatus.OK, result.getStatusCode());
        assertEquals(2, result.getBody().getOrderId());
        assertEquals("New Description", result.getBody().getOrderDesc());
        assertEquals("UPDATED", result.getBody().getOrderStatus());
        assertEquals(200.0, result.getBody().getOrderFee());
        
        verify(orderClientService, times(1)).update(orderId, inputOrder);
    }

    @Test
    @DisplayName("Should delete order by ID successfully")
    void testDeleteById_Success() {
        // Given
        String orderId = "1";
        ResponseEntity<Boolean> mockResponse = new ResponseEntity<>(true, HttpStatus.OK);
        when(orderClientService.deleteById(orderId)).thenReturn(mockResponse);

        // When
        ResponseEntity<Boolean> result = orderController.deleteById(orderId);

        // Then
        assertNotNull(result);
        assertEquals(HttpStatus.OK, result.getStatusCode());
        assertTrue(result.getBody());
        
        verify(orderClientService, times(1)).deleteById(orderId);
    }

    @Test
    @DisplayName("Should delete order by ID and return true regardless of service response")
    void testDeleteById_AlwaysReturnsTrue() {
        // Given
        String orderId = "999";
        ResponseEntity<Boolean> mockResponse = new ResponseEntity<>(false, HttpStatus.OK);
        when(orderClientService.deleteById(orderId)).thenReturn(mockResponse);

        // When
        ResponseEntity<Boolean> result = orderController.deleteById(orderId);

        // Then
        assertNotNull(result);
        assertEquals(HttpStatus.OK, result.getStatusCode());
        assertTrue(result.getBody()); // Controller always returns true
        
        verify(orderClientService, times(1)).deleteById(orderId);
    }

    @Test
    @DisplayName("Should handle order with all status transitions")
    void testOrderStatusTransitions() {
        // Given
        String[] statuses = {"CREATED", "PENDING", "PROCESSING", "SHIPPED", "DELIVERED", "CANCELLED"};
        
        for (int i = 0; i < statuses.length; i++) {
            int orderId = i + 1;
            OrderDto statusOrder = OrderDto.builder()
                    .orderId(orderId)
                    .orderStatus(statuses[i])
                    .build();
            ResponseEntity<OrderDto> mockResponse = new ResponseEntity<>(statusOrder, HttpStatus.OK);
            when(orderClientService.updateStatus(orderId)).thenReturn(mockResponse);

            // When
            ResponseEntity<OrderDto> result = orderController.update(orderId);

            // Then
            assertNotNull(result);
            assertEquals(HttpStatus.OK, result.getStatusCode());
            assertEquals(statuses[i], result.getBody().getOrderStatus());
            
            verify(orderClientService, times(1)).updateStatus(orderId);
        }
    }

    @Test
    @DisplayName("Should verify service method calls with correct parameters")
    void testServiceMethodCalls() {
        // Given
        String orderId = "123";
        int orderIdInt = 123;
        ResponseEntity<OrderDto> findByIdResponse = new ResponseEntity<>(orderDto, HttpStatus.OK);
        ResponseEntity<OrderDto> saveResponse = new ResponseEntity<>(orderDto, HttpStatus.OK);
        ResponseEntity<OrderDto> updateStatusResponse = new ResponseEntity<>(orderDto, HttpStatus.OK);
        ResponseEntity<OrderDto> updateResponse = new ResponseEntity<>(orderDto, HttpStatus.OK);
        ResponseEntity<Boolean> deleteResponse = new ResponseEntity<>(true, HttpStatus.OK);
        ResponseEntity<OrderOrderServiceDtoCollectionResponse> findAllResponse = 
            new ResponseEntity<>(collectionResponse, HttpStatus.OK);

        when(orderClientService.findById(orderId)).thenReturn(findByIdResponse);
        when(orderClientService.save(orderDto)).thenReturn(saveResponse);
        when(orderClientService.updateStatus(orderIdInt)).thenReturn(updateStatusResponse);
        when(orderClientService.update(orderId, orderDto)).thenReturn(updateResponse);
        when(orderClientService.deleteById(orderId)).thenReturn(deleteResponse);
        when(orderClientService.findAll()).thenReturn(findAllResponse);

        // When
        orderController.findById(orderId);
        orderController.save(orderDto);
        orderController.update(orderIdInt);
        orderController.update(orderId, orderDto);
        orderController.deleteById(orderId);
        orderController.findAll();

        // Then
        verify(orderClientService, times(1)).findById(eq(orderId));
        verify(orderClientService, times(1)).save(eq(orderDto));
        verify(orderClientService, times(1)).updateStatus(eq(orderIdInt));
        verify(orderClientService, times(1)).update(eq(orderId), eq(orderDto));
        verify(orderClientService, times(1)).deleteById(eq(orderId));
        verify(orderClientService, times(1)).findAll();
    }

    @Test
    @DisplayName("Should handle multiple orders in collection")
    void testFindAll_MultipleOrders() {
        // Given
        OrderDto order1 = OrderDto.builder()
                .orderId(1)
                .orderDesc("First Order")
                .orderStatus("PENDING")
                .orderFee(50.0)
                .build();
        
        OrderDto order2 = OrderDto.builder()
                .orderId(2)
                .orderDesc("Second Order")
                .orderStatus("COMPLETED")
                .orderFee(75.0)
                .build();
        
        OrderDto order3 = OrderDto.builder()
                .orderId(3)
                .orderDesc("Third Order")
                .orderStatus("SHIPPED")
                .orderFee(100.0)
                .build();

        Collection<OrderDto> orders = Arrays.asList(order1, order2, order3);
        OrderOrderServiceDtoCollectionResponse multipleOrdersResponse = 
            OrderOrderServiceDtoCollectionResponse.builder()
                .collection(orders)
                .build();
        
        ResponseEntity<OrderOrderServiceDtoCollectionResponse> mockResponse = 
            new ResponseEntity<>(multipleOrdersResponse, HttpStatus.OK);
        when(orderClientService.findAll()).thenReturn(mockResponse);

        // When
        ResponseEntity<OrderOrderServiceDtoCollectionResponse> result = orderController.findAll();

        // Then
        assertNotNull(result);
        assertEquals(HttpStatus.OK, result.getStatusCode());
        assertEquals(3, result.getBody().getCollection().size());
        
        verify(orderClientService, times(1)).findAll();
    }
}