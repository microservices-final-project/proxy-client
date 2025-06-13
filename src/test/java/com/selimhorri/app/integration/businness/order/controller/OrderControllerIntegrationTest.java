package com.selimhorri.app.integration.businness.order.controller;

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
import com.selimhorri.app.business.order.controller.OrderController;
import com.selimhorri.app.business.order.model.CartDto;
import com.selimhorri.app.business.order.model.OrderDto;
import com.selimhorri.app.business.order.model.UserDto;
import com.selimhorri.app.business.order.model.response.OrderOrderServiceDtoCollectionResponse;
import com.selimhorri.app.business.order.service.OrderClientService;
import com.selimhorri.app.config.template.TemplateConfig;
import com.selimhorri.app.jwt.service.JwtService;
import com.selimhorri.app.jwt.util.JwtUtil;
import com.selimhorri.app.security.SecurityConfig;

@WebMvcTest(OrderController.class)
@Import({ TemplateConfig.class, SecurityConfig.class })
@Tag("integration")
class OrderControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private OrderClientService orderClientService;

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

    private OrderDto testOrder;
    private OrderOrderServiceDtoCollectionResponse collectionResponse;

    @BeforeEach
    void setUp() {
        // Setup test data
        testOrder = createTestOrder();
        collectionResponse = new OrderOrderServiceDtoCollectionResponse();
        collectionResponse.setCollection(Arrays.asList(testOrder));
    }

    private OrderDto createTestOrder() {
        OrderDto order = new OrderDto();
        order.setOrderId(1);
        order.setOrderDate(LocalDateTime.now());
        order.setOrderDesc("Test order description");
        order.setOrderStatus("PENDING");
        order.setOrderFee(99.99);

        // Create cart
        CartDto cart = new CartDto();
        cart.setCartId(1);
        
        // Create user for cart
        UserDto user = new UserDto();
        user.setUserId(1);
        user.setFirstName("John");
        user.setLastName("Doe");
        cart.setUserDto(user);
        
        order.setCartDto(cart);

        return order;
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void testFindAll_Success() throws Exception {
        // Given
        when(orderClientService.findAll()).thenReturn(ResponseEntity.ok(collectionResponse));

        // When & Then
        mockMvc.perform(get("/api/orders")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.collection").isArray())
                .andExpect(jsonPath("$.collection[0].orderId").value(1))
                .andExpect(jsonPath("$.collection[0].orderStatus").value("PENDING"))
                .andExpect(jsonPath("$.collection[0].orderFee").value(99.99));
    }

    @Test
    @WithMockUser(roles = "USER")
    void testFindAll_Forbidden_UserRole() throws Exception {
        // When & Then
        mockMvc.perform(get("/api/orders")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "user1", roles = "USER")
    void testFindById_Success_SameUser() throws Exception {
        // Given
        String orderId = "1";
        new User("user1", "password",
                Arrays.asList(new SimpleGrantedAuthority("ROLE_USER")));

        // Mock AuthUtil to return the same user ID as the cart owner
        when(authUtil.getOwner(eq(orderId), any(ResourceType.class))).thenReturn("1");
        doNothing().when(authUtil).canActivate(any(HttpServletRequest.class), eq("1"), any(UserDetails.class));
        when(orderClientService.findById(orderId)).thenReturn(ResponseEntity.ok(testOrder));

        // When & Then
        mockMvc.perform(get("/api/orders/{orderId}", orderId)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.orderId").value(1))
                .andExpect(jsonPath("$.orderStatus").value("PENDING"))
                .andExpect(jsonPath("$.cart.user.userId").value(1));
    }

    @Test
    @WithMockUser(username = "admin", roles = "ADMIN")
    void testFindById_Success_Admin() throws Exception {
        // Given
        String orderId = "1";
        new User("admin", "password",
                Arrays.asList(new SimpleGrantedAuthority("ROLE_ADMIN")));

        // Mock AuthUtil to return any user ID (admin can access all)
        when(authUtil.getOwner(eq(orderId), any(ResourceType.class))).thenReturn("1");
        doNothing().when(authUtil).canActivate(any(HttpServletRequest.class), eq("1"), any(UserDetails.class));
        when(orderClientService.findById(orderId)).thenReturn(ResponseEntity.ok(testOrder));

        // When & Then
        mockMvc.perform(get("/api/orders/{orderId}", orderId)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.orderId").value(1));
    }

    @Test
    @WithMockUser(username = "user1", roles = "USER")
    void testSave_Success() throws Exception {
        // Given
        OrderDto newOrder = new OrderDto();
        newOrder.setOrderDesc("New order");
        newOrder.setOrderStatus("CREATED");
        newOrder.setOrderFee(49.99);

        CartDto cart = new CartDto();
        cart.setCartId(2);
        UserDto user = new UserDto();
        user.setUserId(1); // Same as authenticated user
        cart.setUserDto(user);
        newOrder.setCartDto(cart);

        OrderDto savedOrder = new OrderDto();
        savedOrder.setOrderId(2);
        savedOrder.setOrderDesc("New order");
        savedOrder.setOrderStatus("CREATED");
        savedOrder.setOrderFee(49.99);
        savedOrder.setCartDto(cart);

        // Mock AuthUtil to return the same user ID as the cart owner
        when(authUtil.getOwner(eq("2"), any(ResourceType.class))).thenReturn("1");
        doNothing().when(authUtil).canActivate(any(HttpServletRequest.class), eq("1"), any(UserDetails.class));
        when(orderClientService.save(any(OrderDto.class))).thenReturn(ResponseEntity.ok(savedOrder));

        // When & Then
        mockMvc.perform(post("/api/orders")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(newOrder)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.orderId").value(2))
                .andExpect(jsonPath("$.orderStatus").value("CREATED"))
                .andExpect(jsonPath("$.cart.user.userId").value(1));
    }

    @Test
    @WithMockUser(username = "admin", roles = "ADMIN")
    void testUpdateStatus_Success() throws Exception {
        // Given
        int orderId = 1;
        OrderDto updatedOrder = new OrderDto();
        updatedOrder.setOrderId(1);
        updatedOrder.setOrderStatus("SHIPPED");

        when(orderClientService.updateStatus(orderId)).thenReturn(ResponseEntity.ok(updatedOrder));

        // When & Then
        mockMvc.perform(patch("/api/orders/{orderId}/status", orderId)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.orderId").value(1))
                .andExpect(jsonPath("$.orderStatus").value("SHIPPED"));
    }

    @Test
    @WithMockUser(roles = "USER")
    void testUpdateStatus_Forbidden_UserRole() throws Exception {
        // When & Then
        mockMvc.perform(patch("/api/orders/1/status")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "user1", roles = "USER")
    void testUpdate_Success() throws Exception {
        // Given
        String orderId = "1";
        OrderDto updatedOrder = new OrderDto();
        updatedOrder.setOrderId(1);
        updatedOrder.setOrderDesc("Updated description");
        updatedOrder.setOrderStatus("PROCESSING");

        CartDto cart = new CartDto();
        cart.setCartId(1);
        UserDto user = new UserDto();
        user.setUserId(1); // Same as authenticated user
        cart.setUserDto(user);
        updatedOrder.setCartDto(cart);

        // Mock AuthUtil to return the same user ID as the cart owner
        when(authUtil.getOwner(eq(orderId), any(ResourceType.class))).thenReturn("1");
        doNothing().when(authUtil).canActivate(any(HttpServletRequest.class), eq("1"), any(UserDetails.class));
        when(orderClientService.update(eq(orderId), any(OrderDto.class))).thenReturn(ResponseEntity.ok(updatedOrder));

        // When & Then
        mockMvc.perform(put("/api/orders/{orderId}", orderId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updatedOrder)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.orderId").value(1))
                .andExpect(jsonPath("$.orderDesc").value("Updated description"));
    }

    @Test
    @WithMockUser(username = "user1", roles = "USER")
    void testDeleteById_Success() throws Exception {
        // Given
        String orderId = "1";
        
        // Mock AuthUtil to return the same user ID as the cart owner
        when(authUtil.getOwner(eq(orderId), any(ResourceType.class))).thenReturn("1");
        doNothing().when(authUtil).canActivate(any(HttpServletRequest.class), eq("1"), any(UserDetails.class));
        when(orderClientService.deleteById(orderId)).thenReturn(ResponseEntity.ok(true));

        // When & Then
        mockMvc.perform(delete("/api/orders/{orderId}", orderId)
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
        when(orderClientService.deleteById(orderId)).thenReturn(ResponseEntity.ok(true));

        // When & Then
        mockMvc.perform(delete("/api/orders/{orderId}", orderId)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").value(true));
    }
}