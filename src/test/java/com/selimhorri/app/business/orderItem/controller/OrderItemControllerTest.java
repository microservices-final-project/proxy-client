package com.selimhorri.app.business.orderItem.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.time.LocalDateTime;
import java.util.Arrays;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.selimhorri.app.business.orderItem.model.OrderItemDto;
import com.selimhorri.app.business.orderItem.model.OrderDto;
import com.selimhorri.app.business.orderItem.model.ProductDto;
import com.selimhorri.app.business.orderItem.model.response.OrderItemOrderItemServiceDtoCollectionResponse;
import com.selimhorri.app.business.orderItem.service.OrderItemClientService;

@ExtendWith(MockitoExtension.class)
class OrderItemControllerTest {

    @Mock
    private OrderItemClientService orderItemClientService;

    @InjectMocks
    private OrderItemController orderItemController;

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;
    private OrderItemDto orderItemDto;
    private OrderItemOrderItemServiceDtoCollectionResponse collectionResponse;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(orderItemController).build();
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        
        setupTestData();
    }

    private void setupTestData() {
        // Crear ProductDto
        ProductDto productDto = ProductDto.builder()
                .productId(1)
                .productTitle("Test Product")
                .imageUrl("http://example.com/product.jpg")
                .sku("SKU123")
                .priceUnit(19.99)
                .quantity(100)
                .build();

        // Crear OrderDto
        OrderDto orderDto = OrderDto.builder()
                .orderId(1)
                .orderDate(LocalDateTime.now())
                .orderDesc("Test Order")
                .orderFee(19.99)
                .orderStatus("PENDING")
                .build();

        // Crear OrderItemDto
        orderItemDto = OrderItemDto.builder()
                .productId(1)
                .orderId(1)
                .orderedQuantity(2)
                .productDto(productDto)
                .orderDto(orderDto)
                .build();

        // Crear respuesta de colección
        collectionResponse = OrderItemOrderItemServiceDtoCollectionResponse.builder()
                .collection(Arrays.asList(orderItemDto))
                .build();
    }

    @Test
    void testFindAll_ShouldReturnAllOrderItems() throws Exception {
        // Given
        when(orderItemClientService.findAll()).thenReturn(ResponseEntity.ok(collectionResponse));

        // When & Then
        mockMvc.perform(get("/api/shippings")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.collection").isArray())
                .andExpect(jsonPath("$.collection[0].productId").value(1))
                .andExpect(jsonPath("$.collection[0].orderId").value(1))
                .andExpect(jsonPath("$.collection[0].orderedQuantity").value(2))
                .andExpect(jsonPath("$.collection[0].product.productTitle").value("Test Product"))
                .andExpect(jsonPath("$.collection[0].order.orderDesc").value("Test Order"));
    }

    @Test
    void testFindById_ShouldReturnOrderItemById() throws Exception {
        // Given
        String orderId = "1";
        when(orderItemClientService.findById(orderId)).thenReturn(ResponseEntity.ok(orderItemDto));

        // When & Then
        mockMvc.perform(get("/api/shippings/{orderId}", orderId)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.productId").value(1))
                .andExpect(jsonPath("$.orderId").value(1))
                .andExpect(jsonPath("$.orderedQuantity").value(2))
                .andExpect(jsonPath("$.product.productTitle").value("Test Product"))
                .andExpect(jsonPath("$.order.orderDesc").value("Test Order"));
    }

    @Test
    void testSave_ShouldCreateNewOrderItem() throws Exception {
        // Given
        OrderItemDto newOrderItem = OrderItemDto.builder()
                .productId(2)
                .orderId(2)
                .orderedQuantity(3)
                .build();

        OrderItemDto savedOrderItem = OrderItemDto.builder()
                .productId(2)
                .orderId(2)
                .orderedQuantity(3)
                .build();

        when(orderItemClientService.save(any(OrderItemDto.class))).thenReturn(ResponseEntity.ok(savedOrderItem));

        // When & Then
        mockMvc.perform(post("/api/shippings")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(newOrderItem)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.productId").value(2))
                .andExpect(jsonPath("$.orderId").value(2))
                .andExpect(jsonPath("$.orderedQuantity").value(3));
    }

    @Test
    void testDeleteById_ShouldDeleteOrderItem() throws Exception {
        // Given
        String orderId = "1";
        when(orderItemClientService.deleteById(orderId)).thenReturn(ResponseEntity.ok(true));

        // When & Then
        mockMvc.perform(delete("/api/shippings/{orderId}", orderId)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().string("true"));
    }

    @Test
    void testFindById_WithNullResponse_ShouldHandleGracefully() throws Exception {
        // Given
        String orderId = "999";
        when(orderItemClientService.findById(orderId)).thenReturn(ResponseEntity.ok(null));

        // When & Then
        mockMvc.perform(get("/api/shippings/{orderId}", orderId)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().string(""));
    }

    @Test
    void testSave_WithCompleteOrderItemData_ShouldCreateOrderItemWithAllFields() throws Exception {
        // Given
        when(orderItemClientService.save(any(OrderItemDto.class))).thenReturn(ResponseEntity.ok(orderItemDto));

        // When & Then
        mockMvc.perform(post("/api/shippings")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(orderItemDto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.productId").value(1))
                .andExpect(jsonPath("$.orderId").value(1))
                .andExpect(jsonPath("$.orderedQuantity").value(2))
                .andExpect(jsonPath("$.product.productTitle").value("Test Product"))
                .andExpect(jsonPath("$.order.orderDesc").value("Test Order"));
    }

    @Test
    void testFindAll_WhenNoOrderItemsExist_ShouldReturnEmptyCollection() throws Exception {
        // Given
        OrderItemOrderItemServiceDtoCollectionResponse emptyResponse = 
                OrderItemOrderItemServiceDtoCollectionResponse.builder()
                .collection(Arrays.asList())
                .build();
        
        when(orderItemClientService.findAll()).thenReturn(ResponseEntity.ok(emptyResponse));

        // When & Then
        mockMvc.perform(get("/api/shippings")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.collection").isArray())
                .andExpect(jsonPath("$.collection").isEmpty());
    }
}