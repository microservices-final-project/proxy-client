package com.selimhorri.app.integration.businness.payment.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.time.LocalDateTime;
import java.util.Arrays;

import javax.servlet.http.HttpServletRequest;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.selimhorri.app.business.auth.enums.ResourceType;
import com.selimhorri.app.business.auth.util.AuthUtil;
import com.selimhorri.app.business.payment.controller.PaymentController;
import com.selimhorri.app.business.payment.model.OrderDto;
import com.selimhorri.app.business.payment.model.PaymentDto;
import com.selimhorri.app.business.payment.model.PaymentStatus;
import com.selimhorri.app.business.payment.model.response.PaymentPaymentServiceDtoCollectionResponse;
import com.selimhorri.app.business.payment.service.PaymentClientService;
import com.selimhorri.app.config.template.TemplateConfig;
import com.selimhorri.app.jwt.service.JwtService;
import com.selimhorri.app.jwt.util.JwtUtil;
import com.selimhorri.app.security.SecurityConfig;

@WebMvcTest(PaymentController.class)
@Import({ TemplateConfig.class, SecurityConfig.class })
@Tag("integration")
class PaymentControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private PaymentClientService paymentClientService;

    @MockBean
    private PasswordEncoder passwordEncoder;

    @MockBean
    private JwtUtil jwtUtil;

    @MockBean
    private AuthUtil authUtil;

    @MockBean
    private UserDetailsService userDetailsService;

    @MockBean
    private JwtService jwtService;

    @Autowired
    private ObjectMapper objectMapper;

    private PaymentDto testPayment;
    private PaymentPaymentServiceDtoCollectionResponse collectionResponse;

    @BeforeEach
    void setUp() {
        // Setup test data
        testPayment = createTestPayment();
        collectionResponse = new PaymentPaymentServiceDtoCollectionResponse();
        collectionResponse.setCollection(Arrays.asList(testPayment));
    }

    private PaymentDto createTestPayment() {
        PaymentDto payment = new PaymentDto();
        payment.setPaymentId(1);
        payment.setIsPayed(true);
        payment.setPaymentStatus(PaymentStatus.COMPLETED);

        // Create order
        OrderDto order = new OrderDto();
        order.setOrderId(1);
        order.setOrderDate(LocalDateTime.now());
        order.setOrderStatus("PROCESSING");
        order.setOrderFee(99.99);
        payment.setOrderDto(order);

        return payment;
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void testFindAll_Success() throws Exception {
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
                .andExpect(jsonPath("$.collection[0].order.orderId").value(1));
    }

    @Test
    @WithMockUser(roles = "USER")
    void testFindAll_Forbidden_UserRole() throws Exception {
        // When & Then
        mockMvc.perform(get("/api/payments")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "user1", roles = "USER")
    void testFindById_Success_SameUser() throws Exception {
        // Given
        String paymentId = "1";
        UserDetails userDetails = new User("user1", "password",
                Arrays.asList(new SimpleGrantedAuthority("ROLE_USER")));

        // Mock AuthUtil to return the same user ID as the payment owner
        when(authUtil.getOwner(eq(paymentId), any(ResourceType.class))).thenReturn("1");
        doNothing().when(authUtil).canActivate(any(HttpServletRequest.class), eq("1"), any(UserDetails.class));
        when(paymentClientService.findById(paymentId)).thenReturn(ResponseEntity.ok(testPayment));

        // When & Then
        mockMvc.perform(get("/api/payments/{paymentId}", paymentId)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.paymentId").value(1))
                .andExpect(jsonPath("$.isPayed").value(true))
                .andExpect(jsonPath("$.order.orderId").value(1));
    }

    @Test
    @WithMockUser(username = "admin", roles = "ADMIN")
    void testFindById_Success_Admin() throws Exception {
        // Given
        String paymentId = "1";
        UserDetails userDetails = new User("admin", "password",
                Arrays.asList(new SimpleGrantedAuthority("ROLE_ADMIN")));

        // Mock AuthUtil to return any user ID (admin can access all)
        when(authUtil.getOwner(eq(paymentId), any(ResourceType.class))).thenReturn("1");
        doNothing().when(authUtil).canActivate(any(HttpServletRequest.class), eq("1"), any(UserDetails.class));
        when(paymentClientService.findById(paymentId)).thenReturn(ResponseEntity.ok(testPayment));

        // When & Then
        mockMvc.perform(get("/api/payments/{paymentId}", paymentId)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.paymentId").value(1))
                .andExpect(jsonPath("$.isPayed").value(true));
    }

    @Test
    @WithMockUser(username = "user1", roles = "USER")
    void testSave_Success() throws Exception {
        // Given
        PaymentDto newPayment = new PaymentDto();
        newPayment.setIsPayed(false);
        newPayment.setPaymentStatus(PaymentStatus.NOT_STARTED);

        OrderDto order = new OrderDto();
        order.setOrderId(1); // Order owned by user1
        newPayment.setOrderDto(order);

        PaymentDto savedPayment = new PaymentDto();
        savedPayment.setPaymentId(2);
        savedPayment.setIsPayed(false);
        savedPayment.setPaymentStatus(PaymentStatus.NOT_STARTED);
        savedPayment.setOrderDto(order);

        // Mock AuthUtil to verify user can only save payment for their own order
        when(authUtil.getOwner(eq("1"), any(ResourceType.class))).thenReturn("1");
        doNothing().when(authUtil).canActivate(any(HttpServletRequest.class), eq("1"), any(UserDetails.class));
        when(paymentClientService.save(any(PaymentDto.class))).thenReturn(ResponseEntity.ok(savedPayment));

        // When & Then
        mockMvc.perform(post("/api/payments")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(newPayment)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.paymentId").value(2))
                .andExpect(jsonPath("$.isPayed").value(false))
                .andExpect(jsonPath("$.order.orderId").value(1));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void testUpdateStatus_Success() throws Exception {
        // Given
        String paymentId = "1";
        PaymentDto updatedPayment = new PaymentDto();
        updatedPayment.setPaymentId(1);
        updatedPayment.setIsPayed(true);
        updatedPayment.setPaymentStatus(PaymentStatus.COMPLETED);

        when(paymentClientService.updateStatus(paymentId)).thenReturn(ResponseEntity.ok(updatedPayment));

        // When & Then
        mockMvc.perform(put("/api/payments/{paymentId}", paymentId)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.paymentId").value(1))
                .andExpect(jsonPath("$.isPayed").value(true))
                .andExpect(jsonPath("$.paymentStatus").value("COMPLETED"));
    }

    @Test
    @WithMockUser(roles = "USER")
    void testUpdateStatus_Forbidden_UserRole() throws Exception {
        // When & Then
        mockMvc.perform(put("/api/payments/1")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "user1", roles = "USER")
    void testDeleteById_Success() throws Exception {
        // Given
        String paymentId = "1";
        
        // Mock AuthUtil to return the same user ID as the payment owner
        when(authUtil.getOwner(eq(paymentId), any(ResourceType.class))).thenReturn("1");
        doNothing().when(authUtil).canActivate(any(HttpServletRequest.class), eq("1"), any(UserDetails.class));
        when(paymentClientService.deleteById(paymentId)).thenReturn(ResponseEntity.ok(true));

        // When & Then
        mockMvc.perform(delete("/api/payments/{paymentId}", paymentId)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").value(true));
    }

    @Test
    @WithMockUser(username = "admin", roles = "ADMIN")
    void testDeleteById_Success_Admin() throws Exception {
        // Given
        String paymentId = "1";
        
        // Mock AuthUtil to return any user ID (admin can access all)
        when(authUtil.getOwner(eq(paymentId), any(ResourceType.class))).thenReturn("2");
        doNothing().when(authUtil).canActivate(any(HttpServletRequest.class), eq("2"), any(UserDetails.class));
        when(paymentClientService.deleteById(paymentId)).thenReturn(ResponseEntity.ok(true));

        // When & Then
        mockMvc.perform(delete("/api/payments/{paymentId}", paymentId)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").value(true));
    }
}