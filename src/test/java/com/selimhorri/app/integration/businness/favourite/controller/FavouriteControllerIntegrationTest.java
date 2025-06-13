package com.selimhorri.app.integration.businness.favourite.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collection;

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
import com.selimhorri.app.business.auth.util.AuthUtil;
import com.selimhorri.app.business.favourite.controller.FavouriteController;
import com.selimhorri.app.business.favourite.model.FavouriteDto;
import com.selimhorri.app.business.favourite.model.ProductDto;
import com.selimhorri.app.business.favourite.model.UserDto;
import com.selimhorri.app.business.favourite.model.response.FavouriteFavouriteServiceCollectionDtoResponse;
import com.selimhorri.app.business.favourite.service.FavouriteClientService;
import com.selimhorri.app.config.template.TemplateConfig;
import com.selimhorri.app.jwt.service.JwtService;
import com.selimhorri.app.jwt.util.JwtUtil;
import com.selimhorri.app.security.SecurityConfig;

@WebMvcTest(FavouriteController.class)
@Import({ TemplateConfig.class, SecurityConfig.class })
@Tag("integration")
class FavouriteControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private FavouriteClientService favouriteClientService;

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

    private FavouriteDto testFavourite;
    private FavouriteFavouriteServiceCollectionDtoResponse collectionResponse;

    @BeforeEach
    void setUp() {
        // Setup test data
        testFavourite = createTestFavourite();
        collectionResponse = FavouriteFavouriteServiceCollectionDtoResponse.builder()
                .collection(Arrays.asList(testFavourite))
                .build();
    }

    private FavouriteDto createTestFavourite() {
        FavouriteDto favourite = new FavouriteDto();
        favourite.setUserId(1);
        favourite.setProductId(1);
        favourite.setLikeDate(LocalDateTime.now());

        // Create user
        UserDto user = new UserDto();
        user.setUserId(1);
        user.setFirstName("John");
        user.setLastName("Doe");
        favourite.setUserDto(user);

        // Create product
        ProductDto product = new ProductDto();
        product.setProductId(1);
        product.setProductTitle("Test Product");
        product.setPriceUnit(99.99);
        favourite.setProductDto(product);

        return favourite;
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void testFindAll_Success() throws Exception {
        // Given
        when(favouriteClientService.findAll()).thenReturn(ResponseEntity.ok(collectionResponse));

        // When & Then
        mockMvc.perform(get("/api/favourites")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.collection").isArray())
                .andExpect(jsonPath("$.collection[0].userId").value(1))
                .andExpect(jsonPath("$.collection[0].productId").value(1))
                .andExpect(jsonPath("$.collection[0].user.firstName").value("John"))
                .andExpect(jsonPath("$.collection[0].product.productTitle").value("Test Product"));
    }

    @Test
    @WithMockUser(roles = "USER")
    void testFindAll_Forbidden_UserRole() throws Exception {
        // When & Then
        mockMvc.perform(get("/api/favourites")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "user1", roles = "USER")
    void testFindById_Success_SameUser() throws Exception {
        // Given
        String userId = "1";
        String productId = "1";
        UserDetails userDetails = new User("user1", "password",
                Arrays.asList(new SimpleGrantedAuthority("ROLE_USER")));

        doNothing().when(authUtil).canActivate(any(HttpServletRequest.class), eq(userId), any(UserDetails.class));
        when(favouriteClientService.findById(userId, productId)).thenReturn(ResponseEntity.ok(testFavourite));

        // When & Then
        mockMvc.perform(get("/api/favourites/{userId}/{productId}", userId, productId)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value(1))
                .andExpect(jsonPath("$.productId").value(1))
                .andExpect(jsonPath("$.user.firstName").value("John"))
                .andExpect(jsonPath("$.product.productTitle").value("Test Product"));
    }

    @Test
    @WithMockUser(username = "admin", roles = "ADMIN")
    void testFindById_Success_Admin() throws Exception {
        // Given
        String userId = "1";
        String productId = "1";
        UserDetails userDetails = new User("admin", "password",
                Arrays.asList(new SimpleGrantedAuthority("ROLE_ADMIN")));

        doNothing().when(authUtil).canActivate(any(HttpServletRequest.class), eq(userId), any(UserDetails.class));
        when(favouriteClientService.findById(userId, productId)).thenReturn(ResponseEntity.ok(testFavourite));

        // When & Then
        mockMvc.perform(get("/api/favourites/{userId}/{productId}", userId, productId)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value(1))
                .andExpect(jsonPath("$.productId").value(1));
    }

    @Test
    @WithMockUser(username = "user1", roles = "USER")
    void testSave_Success() throws Exception {
        // Given
        FavouriteDto newFavourite = new FavouriteDto();
        newFavourite.setUserId(1); // Same as authenticated user
        newFavourite.setProductId(2);
        newFavourite.setLikeDate(LocalDateTime.now());

        FavouriteDto savedFavourite = new FavouriteDto();
        savedFavourite.setUserId(1);
        savedFavourite.setProductId(2);
        savedFavourite.setLikeDate(LocalDateTime.now());

        // Mock AuthUtil to verify user can only save favourite for themselves
        doNothing().when(authUtil).canActivate(any(HttpServletRequest.class), eq("1"), any(UserDetails.class));
        when(favouriteClientService.save(any(FavouriteDto.class))).thenReturn(ResponseEntity.ok(savedFavourite));

        // When & Then
        mockMvc.perform(post("/api/favourites")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(newFavourite)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value(1))
                .andExpect(jsonPath("$.productId").value(2));
    }

    @Test
    @WithMockUser(username = "user1", roles = "USER")
    void testDeleteById_Success() throws Exception {
        // Given
        String userId = "1"; // Same as authenticated user
        String productId = "1";

        doNothing().when(authUtil).canActivate(any(HttpServletRequest.class), eq(userId), any(UserDetails.class));
        when(favouriteClientService.deleteById(userId, productId)).thenReturn(ResponseEntity.ok(true));

        // When & Then
        mockMvc.perform(delete("/api/favourites/{userId}/{productId}", userId, productId)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").value(true));
    }

    @Test
    @WithMockUser(username = "admin", roles = "ADMIN")
    void testDeleteById_Success_Admin() throws Exception {
        // Given
        String userId = "1"; // Different from admin user but allowed for admin
        String productId = "1";

        doNothing().when(authUtil).canActivate(any(HttpServletRequest.class), eq(userId), any(UserDetails.class));
        when(favouriteClientService.deleteById(userId, productId)).thenReturn(ResponseEntity.ok(true));

        // When & Then
        mockMvc.perform(delete("/api/favourites/{userId}/{productId}", userId, productId)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").value(true));
    }

}