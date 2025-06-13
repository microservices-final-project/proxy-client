package com.selimhorri.app.integration.businness.user.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.time.LocalDate;
import java.util.Arrays;
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
import org.springframework.security.core.userdetails.UserDetailsService; // Add this import
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.selimhorri.app.business.auth.util.AuthUtil;
import com.selimhorri.app.business.user.controller.UserController;
import com.selimhorri.app.business.user.model.AddressDto;
import com.selimhorri.app.business.user.model.CredentialDto;
import com.selimhorri.app.business.user.model.RoleBasedAuthority;
import com.selimhorri.app.business.user.model.UserDto;
import com.selimhorri.app.business.user.model.VerificationTokenDto;
import com.selimhorri.app.business.user.model.response.UserUserServiceCollectionDtoResponse;
import com.selimhorri.app.business.user.service.UserClientService;
import com.selimhorri.app.config.template.TemplateConfig;
import com.selimhorri.app.jwt.service.JwtService; // Add this import
import com.selimhorri.app.jwt.util.JwtUtil;
import com.selimhorri.app.security.SecurityConfig;

@WebMvcTest(UserController.class)
@Import({ TemplateConfig.class, SecurityConfig.class })
@Tag("integration")
class UserControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private UserClientService userClientService;

    @MockBean
    private PasswordEncoder passwordEncoder;

    @MockBean
    private JwtUtil jwtUtil;

    @MockBean
    private AuthUtil authUtil;

    @MockBean
    private UserDetailsService userDetailsService;

    // Add this MockBean to resolve the dependency injection issue for JwtService
    @MockBean
    private JwtService jwtService;

    @Autowired
    private ObjectMapper objectMapper;

    private UserDto testUser;
    private UserUserServiceCollectionDtoResponse collectionResponse;

    @BeforeEach
    void setUp() {
        // Setup test data
        testUser = createTestUser();
        collectionResponse = new UserUserServiceCollectionDtoResponse();
        collectionResponse.setCollection(Arrays.asList(testUser));
    }

    private UserDto createTestUser() {
        UserDto user = new UserDto();
        user.setUserId(1);
        user.setFirstName("John");
        user.setLastName("Doe");
        user.setEmail("john.doe@example.com");
        user.setPhone("1234567890");
        user.setImageUrl("http://example.com/image.jpg");

        // Create address
        AddressDto address = new AddressDto();
        address.setAddressId(1);
        address.setFullAddress("123 Main St");
        address.setPostalCode("12345");
        address.setCity("Anytown");
        Set<AddressDto> addresses = new HashSet<>();
        addresses.add(address);
        user.setAddressDtos(addresses);

        // Create credential
        CredentialDto credential = new CredentialDto();
        credential.setCredentialId(1);
        credential.setUsername("johndoe");
        credential.setPassword("password123");
        credential.setRoleBasedAuthority(RoleBasedAuthority.ROLE_USER);
        credential.setIsEnabled(true);
        credential.setIsAccountNonExpired(true);
        credential.setIsAccountNonLocked(true);
        credential.setIsCredentialsNonExpired(true);

        // Create verification token
        VerificationTokenDto token = new VerificationTokenDto();
        token.setVerificationTokenId(1);
        token.setToken("test-token");
        token.setExpireDate(LocalDate.now().plusDays(1));
        Set<VerificationTokenDto> tokens = new HashSet<>();
        tokens.add(token);
        credential.setVerificationTokenDtos(tokens);

        user.setCredentialDto(credential);

        return user;
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void testFindAll_Success() throws Exception {
        // Given
        when(userClientService.findAll()).thenReturn(ResponseEntity.ok(collectionResponse));

        // When & Then
        mockMvc.perform(get("/api/users")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.collection").isArray())
                .andExpect(jsonPath("$.collection[0].userId").value(1))
                .andExpect(jsonPath("$.collection[0].firstName").value("John"))
                .andExpect(jsonPath("$.collection[0].lastName").value("Doe"))
                .andExpect(jsonPath("$.collection[0].email").value("john.doe@example.com"));
    }

    @Test
    @WithMockUser(username = "johndoe", roles = "USER")
    void testFindById_Success_SameUser() throws Exception {
        // Given
        String userId = "1";
        UserDetails userDetails = new User("johndoe", "password",
                Arrays.asList(new SimpleGrantedAuthority("ROLE_USER")));

        doNothing().when(authUtil).canActivate(any(HttpServletRequest.class), eq(userId), any(UserDetails.class));
        when(userClientService.findById(userId)).thenReturn(ResponseEntity.ok(testUser));

        // When & Then
        mockMvc.perform(get("/api/users/{userId}", userId)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value(1))
                .andExpect(jsonPath("$.firstName").value("John"))
                .andExpect(jsonPath("$.lastName").value("Doe"))
                .andExpect(jsonPath("$.email").value("john.doe@example.com"))
                .andExpect(jsonPath("$.credential.username").value("johndoe"));
    }

    @Test
    @WithMockUser(username = "admin", roles = "ADMIN")
    void testFindById_Success_Admin() throws Exception {
        // Given
        String userId = "1";
        UserDetails userDetails = new User("admin", "password",
                Arrays.asList(new SimpleGrantedAuthority("ROLE_ADMIN")));

        doNothing().when(authUtil).canActivate(any(HttpServletRequest.class), eq(userId), any(UserDetails.class));
        when(userClientService.findById(userId)).thenReturn(ResponseEntity.ok(testUser));

        // When & Then
        mockMvc.perform(get("/api/users/{userId}", userId)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value(1))
                .andExpect(jsonPath("$.firstName").value("John"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void testFindByUsername_Success() throws Exception {
        // Given
        String username = "johndoe";
        when(userClientService.findByUsername(username)).thenReturn(ResponseEntity.ok(testUser));

        // When & Then
        mockMvc.perform(get("/api/users/username/{username}", username)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value(1))
                .andExpect(jsonPath("$.credential.username").value("johndoe"));
    }

    @Test
    void testSave_Success() throws Exception {
        // Given
        UserDto newUser = new UserDto();
        newUser.setFirstName("Jane");
        newUser.setLastName("Smith");
        newUser.setEmail("jane.smith@example.com");
        newUser.setPhone("0987654321");

        UserDto savedUser = new UserDto();
        savedUser.setUserId(2);
        savedUser.setFirstName("Jane");
        savedUser.setLastName("Smith");
        savedUser.setEmail("jane.smith@example.com");
        savedUser.setPhone("0987654321");

        when(userClientService.save(any(UserDto.class))).thenReturn(ResponseEntity.ok(savedUser));

        // When & Then
        mockMvc.perform(post("/api/users")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(newUser)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value(2))
                .andExpect(jsonPath("$.firstName").value("Jane"))
                .andExpect(jsonPath("$.lastName").value("Smith"))
                .andExpect(jsonPath("$.email").value("jane.smith@example.com"));
    }

    @Test
    @WithMockUser(username = "johndoe", roles = "USER")
    void testUpdate_Success() throws Exception {
        // Given
        String userId = "1";
        UserDto updatedUser = new UserDto();
        updatedUser.setUserId(1);
        updatedUser.setFirstName("John Updated");
        updatedUser.setLastName("Doe Updated");
        updatedUser.setEmail("john.updated@example.com");
        updatedUser.setPhone("1111111111");

        UserDetails userDetails = new User("johndoe", "password",
                Arrays.asList(new SimpleGrantedAuthority("ROLE_USER")));

        doNothing().when(authUtil).canActivate(any(HttpServletRequest.class), eq(userId), any(UserDetails.class));
        when(userClientService.update(eq(userId), any(UserDto.class))).thenReturn(ResponseEntity.ok(updatedUser));

        // When & Then
        mockMvc.perform(put("/api/users/{userId}", userId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updatedUser)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value(1))
                .andExpect(jsonPath("$.firstName").value("John Updated"))
                .andExpect(jsonPath("$.lastName").value("Doe Updated"))
                .andExpect(jsonPath("$.email").value("john.updated@example.com"));
    }

    @Test
    @WithMockUser(username = "johndoe", roles = "USER")
    void testDeleteById_Success() throws Exception {
        // Given
        String userId = "1";
        UserDetails userDetails = new User("johndoe", "password",
                Arrays.asList(new SimpleGrantedAuthority("ROLE_USER")));

        doNothing().when(authUtil).canActivate(any(HttpServletRequest.class), eq(userId), any(UserDetails.class));
        when(userClientService.deleteById(userId)).thenReturn(ResponseEntity.ok(true));

        // When & Then
        mockMvc.perform(delete("/api/users/{userId}", userId)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").value(true));
    }

    @Test
    @WithMockUser(roles = "USER")
    void testFindAll_Forbidden_UserRole() throws Exception {
        // When & Then
        mockMvc.perform(get("/api/users")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "USER")
    void testFindByUsername_Forbidden_UserRole() throws Exception {
        // When & Then
        mockMvc.perform(get("/api/users/username/johndoe")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden());
    }

    @Test
    void testSave_WithCredential_Success() throws Exception {
        // Given
        UserDto newUser = new UserDto();
        newUser.setFirstName("Test");
        newUser.setLastName("User");
        newUser.setEmail("test@example.com");

        CredentialDto credential = new CredentialDto();
        credential.setUsername("testuser");
        credential.setPassword("testpass");
        credential.setRoleBasedAuthority(RoleBasedAuthority.ROLE_USER);
        credential.setIsEnabled(true);
        credential.setIsAccountNonExpired(true);
        credential.setIsAccountNonLocked(true);
        credential.setIsCredentialsNonExpired(true);

        newUser.setCredentialDto(credential);

        UserDto savedUser = new UserDto();
        savedUser.setUserId(3);
        savedUser.setFirstName("Test");
        savedUser.setLastName("User");
        savedUser.setEmail("test@example.com");
        savedUser.setCredentialDto(credential);

        when(userClientService.save(any(UserDto.class))).thenReturn(ResponseEntity.ok(savedUser));

        // When & Then
        mockMvc.perform(post("/api/users")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(newUser)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value(3))
                .andExpect(jsonPath("$.firstName").value("Test"))
                .andExpect(jsonPath("$.credential.username").value("testuser"));
    }

    @Test
    @WithMockUser(username = "admin", roles = "ADMIN")
    void testUpdate_AsAdmin_Success() throws Exception {
        // Given
        String userId = "1";
        UserDto updatedUser = new UserDto();
        updatedUser.setUserId(1);
        updatedUser.setFirstName("Admin Updated");
        updatedUser.setLastName("User");
        updatedUser.setEmail("admin.updated@example.com");

        UserDetails userDetails = new User("admin", "password",
                Arrays.asList(new SimpleGrantedAuthority("ROLE_ADMIN")));

        doNothing().when(authUtil).canActivate(any(HttpServletRequest.class), eq(userId), any(UserDetails.class));
        when(userClientService.update(eq(userId), any(UserDto.class))).thenReturn(ResponseEntity.ok(updatedUser));

        // When & Then
        mockMvc.perform(put("/api/users/{userId}", userId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updatedUser)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value(1))
                .andExpect(jsonPath("$.firstName").value("Admin Updated"));
    }

    @Test
    @WithMockUser(username = "admin", roles = "ADMIN")
    void testDeleteById_AsAdmin_Success() throws Exception {
        // Given
        String userId = "1";
        UserDetails userDetails = new User("admin", "password",
                Arrays.asList(new SimpleGrantedAuthority("ROLE_ADMIN")));

        doNothing().when(authUtil).canActivate(any(HttpServletRequest.class), eq(userId), any(UserDetails.class));
        when(userClientService.deleteById(userId)).thenReturn(ResponseEntity.ok(true));

        // When & Then
        mockMvc.perform(delete("/api/users/{userId}", userId)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").value(true));
    }
}