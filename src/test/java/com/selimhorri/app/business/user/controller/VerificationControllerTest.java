package com.selimhorri.app.business.user.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.time.LocalDate;
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
import com.selimhorri.app.business.user.model.CredentialDto;
import com.selimhorri.app.business.user.model.VerificationTokenDto;
import com.selimhorri.app.business.user.model.response.VerificationUserTokenServiceCollectionDtoResponse;
import com.selimhorri.app.business.user.service.VerificationTokenClientService;

@ExtendWith(MockitoExtension.class)
class VerificationTokenControllerTest {

    @Mock
    private VerificationTokenClientService verificationTokenClientService;

    @InjectMocks
    private VerificationTokenController verificationTokenController;

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;
    private VerificationTokenDto verificationTokenDto;
    private VerificationUserTokenServiceCollectionDtoResponse collectionResponse;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(verificationTokenController).build();
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule()); // For LocalDate serialization
        
        // Create test data
        setupTestData();
    }

    private void setupTestData() {
        // Create CredentialDto
        CredentialDto credentialDto = CredentialDto.builder()
                .credentialId(1)
                .username("testuser")
                .password("password123")
                .build();

        // Create VerificationTokenDto
        verificationTokenDto = VerificationTokenDto.builder()
                .verificationTokenId(1)
                .token("test-token-123")
                .expireDate(LocalDate.now().plusDays(1))
                .credentialDto(credentialDto)
                .build();

        // Create collection response
        collectionResponse = VerificationUserTokenServiceCollectionDtoResponse.builder()
                .collection(Arrays.asList(verificationTokenDto))
                .build();
    }

    @Test
    void testFindAll_ShouldReturnAllVerificationTokens() throws Exception {
        // Given
        when(verificationTokenClientService.findAll()).thenReturn(ResponseEntity.ok(collectionResponse));

        // When & Then
        mockMvc.perform(get("/api/verificationTokens")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.collection").isArray())
                .andExpect(jsonPath("$.collection[0].verificationTokenId").value(1))
                .andExpect(jsonPath("$.collection[0].token").value("test-token-123"))
                .andExpect(jsonPath("$.collection[0].credential.credentialId").value(1))
                .andExpect(jsonPath("$.collection[0].credential.username").value("testuser"));
    }

    @Test
    void testFindById_ShouldReturnVerificationTokenById() throws Exception {
        // Given
        String verificationTokenId = "1";
        when(verificationTokenClientService.findById(verificationTokenId)).thenReturn(ResponseEntity.ok(verificationTokenDto));

        // When & Then
        mockMvc.perform(get("/api/verificationTokens/{verificationTokenId}", verificationTokenId)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.verificationTokenId").value(1))
                .andExpect(jsonPath("$.token").value("test-token-123"))
                .andExpect(jsonPath("$.credential.credentialId").value(1))
                .andExpect(jsonPath("$.credential.username").value("testuser"));
    }

    @Test
    void testSave_ShouldCreateNewVerificationToken() throws Exception {
        // Given
        VerificationTokenDto newToken = VerificationTokenDto.builder()
                .token("new-token-456")
                .expireDate(LocalDate.now().plusDays(2))
                .build();

        VerificationTokenDto savedToken = VerificationTokenDto.builder()
                .verificationTokenId(2)
                .token("new-token-456")
                .expireDate(LocalDate.now().plusDays(2))
                .build();

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
    void testUpdate_WithoutPathVariable_ShouldUpdateVerificationToken() throws Exception {
        // Given
        VerificationTokenDto updatedToken = VerificationTokenDto.builder()
                .verificationTokenId(1)
                .token("updated-token-789")
                .expireDate(LocalDate.now().plusDays(3))
                .build();

        when(verificationTokenClientService.update(any(VerificationTokenDto.class))).thenReturn(ResponseEntity.ok(updatedToken));

        // When & Then
        mockMvc.perform(put("/api/verificationTokens")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updatedToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.verificationTokenId").value(1))
                .andExpect(jsonPath("$.token").value("updated-token-789"));
    }

    @Test
    void testUpdate_WithPathVariable_ShouldUpdateVerificationToken() throws Exception {
        // Given
        String verificationTokenId = "1";
        VerificationTokenDto updatedToken = VerificationTokenDto.builder()
                .verificationTokenId(1)
                .token("path-updated-token")
                .expireDate(LocalDate.now().plusDays(4))
                .build();

        when(verificationTokenClientService.update(any(VerificationTokenDto.class))).thenReturn(ResponseEntity.ok(updatedToken));

        // When & Then
        mockMvc.perform(put("/api/verificationTokens/{verificationTokenId}", verificationTokenId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updatedToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.verificationTokenId").value(1))
                .andExpect(jsonPath("$.token").value("path-updated-token"));
    }

    @Test
    void testDeleteById_ShouldDeleteVerificationToken() throws Exception {
        // Given
        String verificationTokenId = "1";
        when(verificationTokenClientService.deleteById(verificationTokenId)).thenReturn(ResponseEntity.ok(true));

        // When & Then
        mockMvc.perform(delete("/api/verificationTokens/{verificationTokenId}", verificationTokenId)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().string("true"));
    }

    @Test
    void testDeleteById_WhenDeleteFails_ShouldReturnFalse() throws Exception {
        // Given
        String verificationTokenId = "999";
        when(verificationTokenClientService.deleteById(verificationTokenId)).thenReturn(ResponseEntity.ok(false));

        // When & Then
        mockMvc.perform(delete("/api/verificationTokens/{verificationTokenId}", verificationTokenId)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().string("false"));
    }

    @Test
    void testFindById_WithNullResponse_ShouldHandleGracefully() throws Exception {
        // Given
        String verificationTokenId = "999";
        when(verificationTokenClientService.findById(verificationTokenId)).thenReturn(ResponseEntity.ok(null));

        // When & Then
        mockMvc.perform(get("/api/verificationTokens/{verificationTokenId}", verificationTokenId)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().string(""));
    }

    @Test
    void testSave_WithCompleteVerificationTokenData_ShouldCreateTokenWithAllFields() throws Exception {
        // Given
        when(verificationTokenClientService.save(any(VerificationTokenDto.class))).thenReturn(ResponseEntity.ok(verificationTokenDto));

        // When & Then
        mockMvc.perform(post("/api/verificationTokens")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(verificationTokenDto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.verificationTokenId").value(1))
                .andExpect(jsonPath("$.token").value("test-token-123"))
                .andExpect(jsonPath("$.credential.credentialId").value(1))
                .andExpect(jsonPath("$.credential.username").value("testuser"));
    }
}