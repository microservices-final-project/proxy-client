package com.selimhorri.app.integration.businness.user.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.Collections;

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
import com.selimhorri.app.business.user.controller.CredentialController;
import com.selimhorri.app.business.user.model.CredentialDto;
import com.selimhorri.app.business.user.model.RoleBasedAuthority;
import com.selimhorri.app.business.user.model.UserDto;
import com.selimhorri.app.business.user.model.VerificationTokenDto;
import com.selimhorri.app.business.user.model.response.CredentialUserServiceCollectionDtoResponse;
import com.selimhorri.app.business.user.service.CredentialClientService;
import com.selimhorri.app.config.template.TemplateConfig;
import com.selimhorri.app.jwt.service.JwtService;
import com.selimhorri.app.jwt.util.JwtUtil;
import com.selimhorri.app.security.SecurityConfig;

@WebMvcTest(CredentialController.class)
@Import({ TemplateConfig.class, SecurityConfig.class })
@Tag("integration")
class CredentialControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private CredentialClientService credentialClientService;

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

    private CredentialDto testCredential;
    private CredentialUserServiceCollectionDtoResponse collectionResponse;

    @BeforeEach
    void setUp() {
        // Setup test data
        testCredential = createTestCredential();
        collectionResponse = new CredentialUserServiceCollectionDtoResponse();
        collectionResponse.setCollection(Collections.singletonList(testCredential));
    }

    private CredentialDto createTestCredential() {
        UserDto user = new UserDto();
        user.setUserId(1);
        user.setFirstName("John");
        user.setLastName("Doe");
        user.setEmail("john.doe@example.com");

        CredentialDto credential = CredentialDto.builder()
                .credentialId(1)
                .username("johndoe")
                .password("password123")
                .roleBasedAuthority(RoleBasedAuthority.ROLE_USER)
                .isEnabled(true)
                .isAccountNonExpired(true)
                .isAccountNonLocked(true)
                .isCredentialsNonExpired(true)
                .userDto(user)
                .build();

        VerificationTokenDto token = new VerificationTokenDto();
        token.setVerificationTokenId(1);
        token.setToken("test-token");
        token.setExpireDate(LocalDate.now().plusDays(1));
        credential.setVerificationTokenDtos(Collections.singleton(token));

        return credential;
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void testFindAll_Success() throws Exception {
        // Given
        when(credentialClientService.findAll()).thenReturn(ResponseEntity.ok(collectionResponse));

        // When & Then
        mockMvc.perform(get("/api/credentials")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.collection").isArray())
                .andExpect(jsonPath("$.collection[0].credentialId").value(1))
                .andExpect(jsonPath("$.collection[0].username").value("johndoe"))
                .andExpect(jsonPath("$.collection[0].user.userId").value(1));
    }

    @Test
    @WithMockUser(username = "johndoe", roles = "USER")
    void testFindById_Success_SameUser() throws Exception {
        // Given
        String credentialId = "1";
        UserDetails userDetails = new User("johndoe", "password",
                Arrays.asList(new SimpleGrantedAuthority("ROLE_USER")));

        when(authUtil.getOwner(credentialId, ResourceType.CREDENTIALS)).thenReturn("1");
        doNothing().when(authUtil).canActivate(any(HttpServletRequest.class), eq("1"), any(UserDetails.class));
        when(credentialClientService.findById(credentialId)).thenReturn(ResponseEntity.ok(testCredential));

        // When & Then
        mockMvc.perform(get("/api/credentials/{credentialId}", credentialId)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.credentialId").value(1))
                .andExpect(jsonPath("$.username").value("johndoe"))
                .andExpect(jsonPath("$.user.firstName").value("John"));
    }

    @Test
    @WithMockUser(username = "admin", roles = "ADMIN")
    void testFindById_Success_Admin() throws Exception {
        // Given
        String credentialId = "1";
        UserDetails userDetails = new User("admin", "password",
                Arrays.asList(new SimpleGrantedAuthority("ROLE_ADMIN")));

        when(authUtil.getOwner(credentialId, ResourceType.CREDENTIALS)).thenReturn("1");
        doNothing().when(authUtil).canActivate(any(HttpServletRequest.class), eq("1"), any(UserDetails.class));
        when(credentialClientService.findById(credentialId)).thenReturn(ResponseEntity.ok(testCredential));

        // When & Then
        mockMvc.perform(get("/api/credentials/{credentialId}", credentialId)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.credentialId").value(1));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void testFindByUsername_Success() throws Exception {
        // Given
        String username = "johndoe";
        when(credentialClientService.findByUsername(username)).thenReturn(ResponseEntity.ok(testCredential));

        // When & Then
        mockMvc.perform(get("/api/credentials/username/{username}", username)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.credentialId").value(1))
                .andExpect(jsonPath("$.username").value("johndoe"));
    }

    @Test
    @WithMockUser(username = "johndoe", roles = "USER")
    void testSave_Success() throws Exception {
        // Given
        CredentialDto newCredential = CredentialDto.builder()
                .username("newuser")
                .password("newpass")
                .roleBasedAuthority(RoleBasedAuthority.ROLE_USER)
                .isEnabled(true)
                .isAccountNonExpired(true)
                .isAccountNonLocked(true)
                .isCredentialsNonExpired(true)
                .build();

        UserDto user = new UserDto();
        user.setUserId(1);
        newCredential.setUserDto(user);

        CredentialDto savedCredential = CredentialDto.builder()
                .credentialId(2)
                .username("newuser")
                .password("newpass")
                .roleBasedAuthority(RoleBasedAuthority.ROLE_USER)
                .isEnabled(true)
                .userDto(user)
                .build();

        doNothing().when(authUtil).canActivate(any(HttpServletRequest.class), eq("1"), any(UserDetails.class));
        when(credentialClientService.save(any(CredentialDto.class))).thenReturn(ResponseEntity.ok(savedCredential));

        // When & Then
        mockMvc.perform(post("/api/credentials")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(newCredential)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.credentialId").value(2))
                .andExpect(jsonPath("$.username").value("newuser"))
                .andExpect(jsonPath("$.user.userId").value(1));
    }

    @Test
    @WithMockUser(username = "johndoe", roles = "USER")
    void testDeleteById_Success() throws Exception {
        // Given
        String credentialId = "1";
        UserDetails userDetails = new User("johndoe", "password",
                Arrays.asList(new SimpleGrantedAuthority("ROLE_USER")));

        when(authUtil.getOwner(credentialId, ResourceType.CREDENTIALS)).thenReturn("1");
        doNothing().when(authUtil).canActivate(any(HttpServletRequest.class), eq("1"), any(UserDetails.class));
        when(credentialClientService.deleteById(credentialId)).thenReturn(ResponseEntity.ok(true));

        // When & Then
        mockMvc.perform(delete("/api/credentials/{credentialId}", credentialId)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").value(true));
    }

    @Test
    @WithMockUser(roles = "USER")
    void testFindAll_Forbidden_UserRole() throws Exception {
        // When & Then
        mockMvc.perform(get("/api/credentials")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "USER")
    void testFindByUsername_Forbidden_UserRole() throws Exception {
        // When & Then
        mockMvc.perform(get("/api/credentials/username/johndoe")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "admin", roles = "ADMIN")
    void testSave_AsAdmin_Success() throws Exception {
        // Given
        CredentialDto newCredential = CredentialDto.builder()
                .username("adminuser")
                .password("adminpass")
                .roleBasedAuthority(RoleBasedAuthority.ROLE_ADMIN)
                .isEnabled(true)
                .build();

        UserDto user = new UserDto();
        user.setUserId(2);
        newCredential.setUserDto(user);

        CredentialDto savedCredential = CredentialDto.builder()
                .credentialId(3)
                .username("adminuser")
                .password("adminpass")
                .roleBasedAuthority(RoleBasedAuthority.ROLE_ADMIN)
                .isEnabled(true)
                .userDto(user)
                .build();

        doNothing().when(authUtil).canActivate(any(HttpServletRequest.class), eq("2"), any(UserDetails.class));
        when(credentialClientService.save(any(CredentialDto.class))).thenReturn(ResponseEntity.ok(savedCredential));

        // When & Then
        mockMvc.perform(post("/api/credentials")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(newCredential)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.credentialId").value(3))
                .andExpect(jsonPath("$.username").value("adminuser"));
    }

    @Test
    @WithMockUser(username = "admin", roles = "ADMIN")
    void testDeleteById_AsAdmin_Success() throws Exception {
        // Given
        String credentialId = "1";
        UserDetails userDetails = new User("admin", "password",
                Arrays.asList(new SimpleGrantedAuthority("ROLE_ADMIN")));

        when(authUtil.getOwner(credentialId, ResourceType.CREDENTIALS)).thenReturn("1");
        doNothing().when(authUtil).canActivate(any(HttpServletRequest.class), eq("1"), any(UserDetails.class));
        when(credentialClientService.deleteById(credentialId)).thenReturn(ResponseEntity.ok(true));

        // When & Then
        mockMvc.perform(delete("/api/credentials/{credentialId}", credentialId)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").value(true));
    }
}