package com.selimhorri.app.integration.businness.user.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.time.LocalDate;
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
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.selimhorri.app.business.auth.enums.ResourceType;
import com.selimhorri.app.business.auth.util.AuthUtil;
import com.selimhorri.app.business.user.controller.VerificationTokenController;
import com.selimhorri.app.business.user.model.CredentialDto;
import com.selimhorri.app.business.user.model.RoleBasedAuthority;
import com.selimhorri.app.business.user.model.VerificationTokenDto;
import com.selimhorri.app.business.user.model.response.VerificationUserTokenServiceCollectionDtoResponse;
import com.selimhorri.app.business.user.service.VerificationTokenClientService;
import com.selimhorri.app.config.template.TemplateConfig;
import com.selimhorri.app.jwt.service.JwtService;
import com.selimhorri.app.jwt.util.JwtUtil;
import com.selimhorri.app.security.SecurityConfig;

@WebMvcTest(VerificationTokenController.class)
@Import({ TemplateConfig.class, SecurityConfig.class })
@Tag("integration")
class VerificationTokenControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private VerificationTokenClientService verificationTokenClientService;

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

    private VerificationTokenDto testToken;
    private VerificationUserTokenServiceCollectionDtoResponse collectionResponse;

    @BeforeEach
    void setUp() {
        // Setup test data
        testToken = createTestToken();
        collectionResponse = new VerificationUserTokenServiceCollectionDtoResponse();
        collectionResponse.setCollection(Collections.singletonList(testToken));
    }

    private VerificationTokenDto createTestToken() {
        CredentialDto credential = CredentialDto.builder()
                .credentialId(1)
                .username("testuser")
                .password("password")
                .roleBasedAuthority(RoleBasedAuthority.ROLE_USER)
                .isEnabled(true)
                .isAccountNonExpired(true)
                .isAccountNonLocked(true)
                .isCredentialsNonExpired(true)
                .build();

        VerificationTokenDto token = new VerificationTokenDto();
        token.setVerificationTokenId(1);
        token.setToken("test-token-123");
        token.setExpireDate(LocalDate.now().plusDays(1));
        token.setCredentialDto(credential);

        return token;
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void testFindAll_Success() throws Exception {
        // Given
        when(verificationTokenClientService.findAll()).thenReturn(ResponseEntity.ok(collectionResponse));

        // When & Then
        mockMvc.perform(get("/api/verificationTokens")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.collection").isArray())
                .andExpect(jsonPath("$.collection[0].verificationTokenId").value(1))
                .andExpect(jsonPath("$.collection[0].token").value("test-token-123"))
                .andExpect(jsonPath("$.collection[0].credential.username").value("testuser"));
    }

    @Test
    @WithMockUser(roles = "USER")
    void testFindAll_Forbidden_UserRole() throws Exception {
        // When & Then
        mockMvc.perform(get("/api/verificationTokens")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void testFindById_Success() throws Exception {
        // Given
        String tokenId = "1";
        when(verificationTokenClientService.findById(tokenId)).thenReturn(ResponseEntity.ok(testToken));

        // When & Then
        mockMvc.perform(get("/api/verificationTokens/{verificationTokenId}", tokenId)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.verificationTokenId").value(1))
                .andExpect(jsonPath("$.token").value("test-token-123"));
    }

    @Test
    @WithMockUser(username = "testuser", roles = "USER")
    void testSave_Success() throws Exception {
        // Given
        VerificationTokenDto newToken = new VerificationTokenDto();
        newToken.setToken("new-token-456");
        newToken.setExpireDate(LocalDate.now().plusDays(2));

        CredentialDto credential = CredentialDto.builder()
                .credentialId(1)
                .username("testuser")
                .build();
        newToken.setCredentialDto(credential);

        VerificationTokenDto savedToken = new VerificationTokenDto();
        savedToken.setVerificationTokenId(2);
        savedToken.setToken("new-token-456");
        savedToken.setExpireDate(LocalDate.now().plusDays(2));
        savedToken.setCredentialDto(credential);

        when(authUtil.getOwner(eq("1"), eq(ResourceType.CREDENTIALS))).thenReturn("1");
        doNothing().when(authUtil).canActivate(any(HttpServletRequest.class), eq("1"), any(UserDetails.class));
        when(verificationTokenClientService.save(any(VerificationTokenDto.class))).thenReturn(ResponseEntity.ok(savedToken));

        // When & Then
        mockMvc.perform(post("/api/verificationTokens")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(newToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.verificationTokenId").value(2))
                .andExpect(jsonPath("$.token").value("new-token-456"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void testUpdate_Success() throws Exception {
        // Given
        String tokenId = "1";
        VerificationTokenDto updatedToken = new VerificationTokenDto();
        updatedToken.setVerificationTokenId(1);
        updatedToken.setToken("updated-token");
        updatedToken.setExpireDate(LocalDate.now().plusDays(3));

        when(verificationTokenClientService.update(any(VerificationTokenDto.class))).thenReturn(ResponseEntity.ok(updatedToken));

        // When & Then
        mockMvc.perform(put("/api/verificationTokens/{verificationTokenId}", tokenId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updatedToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.verificationTokenId").value(1))
                .andExpect(jsonPath("$.token").value("updated-token"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void testDeleteById_Success() throws Exception {
        // Given
        String tokenId = "1";
        when(verificationTokenClientService.deleteById(tokenId)).thenReturn(ResponseEntity.ok(true));

        // When & Then
        mockMvc.perform(delete("/api/verificationTokens/{verificationTokenId}", tokenId)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").value(true));
    }

    @Test
    @WithMockUser(roles = "USER")
    void testUpdate_Forbidden_UserRole() throws Exception {
        // Given
        String tokenId = "1";
        VerificationTokenDto updatedToken = new VerificationTokenDto();
        updatedToken.setVerificationTokenId(1);
        updatedToken.setToken("updated-token");

        // When & Then
        mockMvc.perform(put("/api/verificationTokens/{verificationTokenId}", tokenId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updatedToken)))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "USER")
    void testDeleteById_Forbidden_UserRole() throws Exception {
        // Given
        String tokenId = "1";

        // When & Then
        mockMvc.perform(delete("/api/verificationTokens/{verificationTokenId}", tokenId)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden());
    }
}