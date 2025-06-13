package com.selimhorri.app.integration.businness.shipping.controller;

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
import com.selimhorri.app.business.orderItem.controller.OrderItemController;
import com.selimhorri.app.business.orderItem.model.OrderDto;
import com.selimhorri.app.business.orderItem.model.OrderItemDto;
import com.selimhorri.app.business.orderItem.model.ProductDto;
import com.selimhorri.app.business.orderItem.model.response.OrderItemOrderItemServiceDtoCollectionResponse;
import com.selimhorri.app.business.orderItem.service.OrderItemClientService;
import com.selimhorri.app.config.template.TemplateConfig;
import com.selimhorri.app.jwt.service.JwtService;
import com.selimhorri.app.jwt.util.JwtUtil;
import com.selimhorri.app.security.SecurityConfig;

@WebMvcTest(OrderItemController.class)
@Import({ TemplateConfig.class, SecurityConfig.class })
@Tag("integration")
class OrderItemControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private OrderItemClientService orderItemClientService;

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

    private OrderItemDto testOrderItem;
    private OrderItemOrderItemServiceDtoCollectionResponse collectionResponse;

    @BeforeEach
    void setUp() {
        // Setup test data
        testOrderItem = createTestOrderItem();
        collectionResponse = new OrderItemOrderItemServiceDtoCollectionResponse();
        collectionResponse.setCollection(Arrays.asList(testOrderItem));
    }

    private OrderItemDto createTestOrderItem() {
        OrderItemDto orderItem = new OrderItemDto();
        orderItem.setProductId(1);
        orderItem.setOrderId(1);
        orderItem.setOrderedQuantity(2);

        // Create product
        ProductDto product = new ProductDto();
        product.setProductId(1);
        product.setProductTitle("Test Product");
        product.setPriceUnit(99.99);
        orderItem.setProductDto(product);

        // Create order
        OrderDto order = new OrderDto();
        order.setOrderId(1);
        order.setOrderDate(LocalDateTime.now());
        order.setOrderStatus("PENDING");
        orderItem.setOrderDto(order);

        return orderItem;
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void testFindAll_Success() throws Exception {
        // Given
        when(orderItemClientService.findAll()).thenReturn(ResponseEntity.ok(collectionResponse));

        // When & Then
        mockMvc.perform(get("/api/shippings")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.collection").isArray())
                .andExpect(jsonPath("$.collection[0].productId").value(1))
                .andExpect(jsonPath("$.collection[0].orderId").value(1))
                .andExpect(jsonPath("$.collection[0].product.productTitle").value("Test Product"))
                .andExpect(jsonPath("$.collection[0].order.orderStatus").value("PENDING"));
    }

    @Test
    @WithMockUser(roles = "USER")
    void testFindAll_Forbidden_UserRole() throws Exception {
        // When & Then
        mockMvc.perform(get("/api/shippings")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "user1", roles = "USER")
    void testFindById_Success_SameUser() throws Exception {
        // Given
        String orderId = "1";
        UserDetails userDetails = new User("user1", "password",
                Arrays.asList(new SimpleGrantedAuthority("ROLE_USER")));

        // Mock AuthUtil to return the same user ID as the order owner
        when(authUtil.getOwner(eq(orderId), any(ResourceType.class))).thenReturn("1");
        doNothing().when(authUtil).canActivate(any(HttpServletRequest.class), eq("1"), any(UserDetails.class));
        when(orderItemClientService.findById(orderId)).thenReturn(ResponseEntity.ok(testOrderItem));

        // When & Then
        mockMvc.perform(get("/api/shippings/{orderId}", orderId)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.productId").value(1))
                .andExpect(jsonPath("$.orderId").value(1))
                .andExpect(jsonPath("$.product.productTitle").value("Test Product"));
    }

    @Test
    @WithMockUser(username = "admin", roles = "ADMIN")
    void testFindById_Success_Admin() throws Exception {
        // Given
        String orderId = "1";
        UserDetails userDetails = new User("admin", "password",
                Arrays.asList(new SimpleGrantedAuthority("ROLE_ADMIN")));

        // Mock AuthUtil to return any user ID (admin can access all)
        when(authUtil.getOwner(eq(orderId), any(ResourceType.class))).thenReturn("1");
        doNothing().when(authUtil).canActivate(any(HttpServletRequest.class), eq("1"), any(UserDetails.class));
        when(orderItemClientService.findById(orderId)).thenReturn(ResponseEntity.ok(testOrderItem));

        // When & Then
        mockMvc.perform(get("/api/shippings/{orderId}", orderId)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.productId").value(1))
                .andExpect(jsonPath("$.orderId").value(1));
    }


    @Test
    @WithMockUser(username = "user1", roles = "USER")
    void testSave_Success() throws Exception {
        // Given
        OrderItemDto newOrderItem = new OrderItemDto();
        newOrderItem.setProductId(2);
        newOrderItem.setOrderId(1);
        newOrderItem.setOrderedQuantity(3);

        OrderItemDto savedOrderItem = new OrderItemDto();
        savedOrderItem.setProductId(2);
        savedOrderItem.setOrderId(1);
        savedOrderItem.setOrderedQuantity(3);

        // Mock AuthUtil to verify user can only save order item for their own order
        when(authUtil.getOwner(eq("1"), any(ResourceType.class))).thenReturn("1");
        doNothing().when(authUtil).canActivate(any(HttpServletRequest.class), eq("1"), any(UserDetails.class));
        when(orderItemClientService.save(any(OrderItemDto.class))).thenReturn(ResponseEntity.ok(savedOrderItem));

        // When & Then
        mockMvc.perform(post("/api/shippings")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(newOrderItem)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.productId").value(2))
                .andExpect(jsonPath("$.orderId").value(1))
                .andExpect(jsonPath("$.orderedQuantity").value(3));
    }

    @Test
    @WithMockUser(username = "user1", roles = "USER")
    void testDeleteById_Success() throws Exception {
        // Given
        String orderId = "1";
        
        // Mock AuthUtil to return the same user ID as the order owner
        when(authUtil.getOwner(eq(orderId), any(ResourceType.class))).thenReturn("1");
        doNothing().when(authUtil).canActivate(any(HttpServletRequest.class), eq("1"), any(UserDetails.class));
        when(orderItemClientService.deleteById(orderId)).thenReturn(ResponseEntity.ok(true));

        // When & Then
        mockMvc.perform(delete("/api/shippings/{orderId}", orderId)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").value(true));
    }

    @Test
    @WithMockUser(username = "admin", roles = "ADMIN")
    void testDeleteById_Success_Admin() throws Exception {
        // Given
        String orderId = "1";
        
        // Mock AuthUtil to return any user ID (admin can access all)
        when(authUtil.getOwner(eq(orderId), any(ResourceType.class))).thenReturn("2");
        doNothing().when(authUtil).canActivate(any(HttpServletRequest.class), eq("2"), any(UserDetails.class));
        when(orderItemClientService.deleteById(orderId)).thenReturn(ResponseEntity.ok(true));

        // When & Then
        mockMvc.perform(delete("/api/shippings/{orderId}", orderId)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").value(true));
    }


}