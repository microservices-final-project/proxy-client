package com.selimhorri.app.business.payment.controller;

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
import com.selimhorri.app.business.payment.model.OrderDto;
import com.selimhorri.app.business.payment.model.PaymentDto;
import com.selimhorri.app.business.payment.model.PaymentStatus;
import com.selimhorri.app.business.payment.model.response.PaymentPaymentServiceDtoCollectionResponse;
import com.selimhorri.app.business.payment.service.PaymentClientService;

@ExtendWith(MockitoExtension.class)
class PaymentControllerTest {

    @Mock
    private PaymentClientService paymentClientService;

    @InjectMocks
    private PaymentController paymentController;

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;
    private PaymentDto paymentDto;
    private PaymentPaymentServiceDtoCollectionResponse collectionResponse;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(paymentController).build();
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        
        setupTestData();
    }

    private void setupTestData() {
        // Crear OrderDto
        OrderDto orderDto = OrderDto.builder()
                .orderId(1)
                .orderDate(LocalDateTime.now())
                .orderDesc("Test Order")
                .orderStatus("PENDING")
                .orderFee(99.99)
                .build();

        // Crear PaymentDto
        paymentDto = PaymentDto.builder()
                .paymentId(1)
                .isPayed(true)
                .paymentStatus(PaymentStatus.COMPLETED)
                .orderDto(orderDto)
                .build();

        // Crear respuesta de colección
        collectionResponse = PaymentPaymentServiceDtoCollectionResponse.builder()
                .collection(Arrays.asList(paymentDto))
                .build();
    }

    @Test
    void testFindAll_ShouldReturnAllPayments() throws Exception {
        // Given
        when(paymentClientService.findAll()).thenReturn(ResponseEntity.ok(collectionResponse));

        // When & Then
        mockMvc.perform(get("/api/payments")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.collection").isArray())
                .andExpect(jsonPath("$.collection[0].paymentId").value(1))
                .andExpect(jsonPath("$.collection[0].isPayed").value(true))
                .andExpect(jsonPath("$.collection[0].paymentStatus").value("COMPLETED"))
                .andExpect(jsonPath("$.collection[0].order.orderId").value(1))
                .andExpect(jsonPath("$.collection[0].order.orderDesc").value("Test Order"));
    }

    @Test
    void testFindById_ShouldReturnPaymentById() throws Exception {
        // Given
        String paymentId = "1";
        when(paymentClientService.findById(paymentId)).thenReturn(ResponseEntity.ok(paymentDto));

        // When & Then
        mockMvc.perform(get("/api/payments/{paymentId}", paymentId)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.paymentId").value(1))
                .andExpect(jsonPath("$.isPayed").value(true))
                .andExpect(jsonPath("$.paymentStatus").value("COMPLETED"))
                .andExpect(jsonPath("$.order.orderId").value(1))
                .andExpect(jsonPath("$.order.orderDesc").value("Test Order"));
    }

    @Test
    void testSave_ShouldCreateNewPayment() throws Exception {
        // Given
        PaymentDto newPayment = PaymentDto.builder()
                .isPayed(false)
                .paymentStatus(PaymentStatus.NOT_STARTED)
                .build();

        PaymentDto savedPayment = PaymentDto.builder()
                .paymentId(2)
                .isPayed(false)
                .paymentStatus(PaymentStatus.NOT_STARTED)
                .build();

        when(paymentClientService.save(any(PaymentDto.class))).thenReturn(ResponseEntity.ok(savedPayment));

        // When & Then
        mockMvc.perform(post("/api/payments")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(newPayment)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.paymentId").value(2))
                .andExpect(jsonPath("$.isPayed").value(false))
                .andExpect(jsonPath("$.paymentStatus").value("NOT_STARTED"));
    }

    @Test
    void testUpdate_ShouldUpdatePayment() throws Exception {
        // Given
        PaymentDto updatedPayment = PaymentDto.builder()
                .paymentId(1)
                .isPayed(true)
                .paymentStatus(PaymentStatus.IN_PROGRESS)
                .build();

        when(paymentClientService.update(any(PaymentDto.class))).thenReturn(ResponseEntity.ok(updatedPayment));

        // When & Then
        mockMvc.perform(put("/api/payments")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updatedPayment)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.paymentId").value(1))
                .andExpect(jsonPath("$.isPayed").value(true))
                .andExpect(jsonPath("$.paymentStatus").value("IN_PROGRESS"));
    }

    @Test
    void testDeleteById_ShouldDeletePayment() throws Exception {
        // Given
        String paymentId = "1";
        when(paymentClientService.deleteById(paymentId)).thenReturn(ResponseEntity.ok(true));

        // When & Then
        mockMvc.perform(delete("/api/payments/{paymentId}", paymentId)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().string("true"));
    }

    @Test
    void testFindById_WithNullResponse_ShouldHandleGracefully() throws Exception {
        // Given
        String paymentId = "999";
        when(paymentClientService.findById(paymentId)).thenReturn(ResponseEntity.ok(null));

        // When & Then
        mockMvc.perform(get("/api/payments/{paymentId}", paymentId)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().string(""));
    }

    @Test
    void testSave_WithCompletePaymentData_ShouldCreatePaymentWithAllFields() throws Exception {
        // Given
        when(paymentClientService.save(any(PaymentDto.class))).thenReturn(ResponseEntity.ok(paymentDto));

        // When & Then
        mockMvc.perform(post("/api/payments")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(paymentDto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.paymentId").value(1))
                .andExpect(jsonPath("$.isPayed").value(true))
                .andExpect(jsonPath("$.paymentStatus").value("COMPLETED"))
                .andExpect(jsonPath("$.order.orderId").value(1))
                .andExpect(jsonPath("$.order.orderDesc").value("Test Order"));
    }

    @Test
    void testFindAll_WhenNoPaymentsExist_ShouldReturnEmptyCollection() throws Exception {
        // Given
        PaymentPaymentServiceDtoCollectionResponse emptyResponse = 
                PaymentPaymentServiceDtoCollectionResponse.builder()
                .collection(Arrays.asList())
                .build();
        
        when(paymentClientService.findAll()).thenReturn(ResponseEntity.ok(emptyResponse));

        // When & Then
        mockMvc.perform(get("/api/payments")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.collection").isArray())
                .andExpect(jsonPath("$.collection").isEmpty());
    }

    @Test
    void testUpdate_WithPartialData_ShouldUpdateOnlyProvidedFields() throws Exception {
        // Given
        PaymentDto partialUpdate = PaymentDto.builder()
                .paymentId(1)
                .isPayed(true)
                .build();

        PaymentDto updatedPayment = PaymentDto.builder()
                .paymentId(1)
                .isPayed(true)
                .paymentStatus(PaymentStatus.COMPLETED) // Mantiene el valor anterior
                .orderDto(paymentDto.getOrderDto()) // Mantiene el valor anterior
                .build();

        when(paymentClientService.update(any(PaymentDto.class))).thenReturn(ResponseEntity.ok(updatedPayment));

        // When & Then
        mockMvc.perform(put("/api/payments")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(partialUpdate)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.paymentId").value(1))
                .andExpect(jsonPath("$.isPayed").value(true))
                .andExpect(jsonPath("$.paymentStatus").value("COMPLETED")) // Verifica que mantuvo el valor
                .andExpect(jsonPath("$.order.orderId").value(1)); // Verifica que mantuvo el order
    }
}