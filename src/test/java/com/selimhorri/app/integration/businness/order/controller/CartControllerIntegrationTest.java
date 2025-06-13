package com.selimhorri.app.integration.businness.order.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

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
import com.selimhorri.app.business.order.controller.CartController;
import com.selimhorri.app.business.order.model.CartDto;
import com.selimhorri.app.business.order.model.OrderDto;
import com.selimhorri.app.business.order.model.UserDto;
import com.selimhorri.app.business.order.model.response.CartOrderServiceDtoCollectionResponse;
import com.selimhorri.app.business.order.service.CartClientService;
import com.selimhorri.app.config.template.TemplateConfig;
import com.selimhorri.app.jwt.service.JwtService;
import com.selimhorri.app.jwt.util.JwtUtil;
import com.selimhorri.app.security.SecurityConfig;

@WebMvcTest(CartController.class)
@Import({ TemplateConfig.class, SecurityConfig.class })
@Tag("integration")
class CartControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private CartClientService cartClientService;

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

    private CartDto testCart;
    private CartOrderServiceDtoCollectionResponse collectionResponse;

    @BeforeEach
    void setUp() {
        // Setup test data
        testCart = createTestCart();
        collectionResponse = new CartOrderServiceDtoCollectionResponse();
        collectionResponse.setCollection(Arrays.asList(testCart));
    }

    private CartDto createTestCart() {
        CartDto cart = new CartDto();
        cart.setCartId(1);
        cart.setUserId(1); // User ID for the cart owner

        // Create user for cart
        UserDto user = new UserDto();
        user.setUserId(1);
        user.setFirstName("John");
        user.setLastName("Doe");
        cart.setUserDto(user);

        // Create orders for cart
        OrderDto order = new OrderDto();
        order.setOrderId(1);
        order.setOrderDesc("Test order");
        Set<OrderDto> orders = new HashSet<>();
        orders.add(order);
        cart.setOrderDtos(orders);

        return cart;
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void testFindAll_Success() throws Exception {
        // Given
        when(cartClientService.findAll()).thenReturn(ResponseEntity.ok(collectionResponse));

        // When & Then
        mockMvc.perform(get("/api/carts")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.collection").isArray())
                .andExpect(jsonPath("$.collection[0].cartId").value(1))
                .andExpect(jsonPath("$.collection[0].user.userId").value(1))
                .andExpect(jsonPath("$.collection[0].orderDtos").isArray());
    }

    @Test
    @WithMockUser(roles = "USER")
    void testFindAll_Forbidden_UserRole() throws Exception {
        // When & Then
        mockMvc.perform(get("/api/carts")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "user1", roles = "USER")
    void testFindById_Success_SameUser() throws Exception {
        // Given
        String cartId = "1";
        UserDetails userDetails = new User("user1", "password",
                Arrays.asList(new SimpleGrantedAuthority("ROLE_USER")));

        // Mock AuthUtil to return the same user ID as the cart owner
        when(authUtil.getOwner(eq(cartId), any(ResourceType.class))).thenReturn("1");
        doNothing().when(authUtil).canActivate(any(HttpServletRequest.class), eq("1"), any(UserDetails.class));
        when(cartClientService.findById(cartId)).thenReturn(ResponseEntity.ok(testCart));

        // When & Then
        mockMvc.perform(get("/api/carts/{cartId}", cartId)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cartId").value(1))
                .andExpect(jsonPath("$.user.userId").value(1))
                .andExpect(jsonPath("$.orderDtos[0].orderId").value(1));
    }

    @Test
    @WithMockUser(username = "admin", roles = "ADMIN")
    void testFindById_Success_Admin() throws Exception {
        // Given
        String cartId = "1";
        UserDetails userDetails = new User("admin", "password",
                Arrays.asList(new SimpleGrantedAuthority("ROLE_ADMIN")));

        // Mock AuthUtil to return any user ID (admin can access all)
        when(authUtil.getOwner(eq(cartId), any(ResourceType.class))).thenReturn("1");
        doNothing().when(authUtil).canActivate(any(HttpServletRequest.class), eq("1"), any(UserDetails.class));
        when(cartClientService.findById(cartId)).thenReturn(ResponseEntity.ok(testCart));

        // When & Then
        mockMvc.perform(get("/api/carts/{cartId}", cartId)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cartId").value(1));
    }


    @Test
    @WithMockUser(username = "user1", roles = "USER")
    void testSave_Success() throws Exception {
        // Given
        CartDto newCart = new CartDto();
        newCart.setUserId(1); // Same as authenticated user

        CartDto savedCart = new CartDto();
        savedCart.setCartId(2);
        savedCart.setUserId(1);

        UserDto user = new UserDto();
        user.setUserId(1);
        savedCart.setUserDto(user);

        // Mock AuthUtil to verify user can only save cart for themselves
        doNothing().when(authUtil).canActivate(any(HttpServletRequest.class), eq("1"), any(UserDetails.class));
        when(cartClientService.save(any(CartDto.class))).thenReturn(ResponseEntity.ok(savedCart));

        // When & Then
        mockMvc.perform(post("/api/carts")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(newCart)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cartId").value(2))
                .andExpect(jsonPath("$.user.userId").value(1));
    }

    @Test
    @WithMockUser(username = "user1", roles = "USER")
    void testDeleteById_Success() throws Exception {
        // Given
        String cartId = "1";
        
        // Mock AuthUtil to return the same user ID as the cart owner
        when(authUtil.getOwner(eq(cartId), any(ResourceType.class))).thenReturn("1");
        doNothing().when(authUtil).canActivate(any(HttpServletRequest.class), eq("1"), any(UserDetails.class));
        when(cartClientService.deleteById(cartId)).thenReturn(ResponseEntity.ok(true));

        // When & Then
        mockMvc.perform(delete("/api/carts/{cartId}", cartId)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").value(true));
    }

    @Test
    @WithMockUser(username = "admin", roles = "ADMIN")
    void testDeleteById_Success_Admin() throws Exception {
        // Given
        String cartId = "1";
        
        // Mock AuthUtil to return any user ID (admin can access all)
        when(authUtil.getOwner(eq(cartId), any(ResourceType.class))).thenReturn("2");
        doNothing().when(authUtil).canActivate(any(HttpServletRequest.class), eq("2"), any(UserDetails.class));
        when(cartClientService.deleteById(cartId)).thenReturn(ResponseEntity.ok(true));

        // When & Then
        mockMvc.perform(delete("/api/carts/{cartId}", cartId)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").value(true));
    }
}