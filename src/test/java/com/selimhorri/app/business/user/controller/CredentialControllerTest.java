package com.selimhorri.app.business.user.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.util.Arrays;
import java.util.Collection;

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
import com.selimhorri.app.business.user.model.CredentialDto;
import com.selimhorri.app.business.user.model.RoleBasedAuthority;
import com.selimhorri.app.business.user.model.UserDto;
import com.selimhorri.app.business.user.model.response.CredentialUserServiceCollectionDtoResponse;
import com.selimhorri.app.business.user.service.CredentialClientService;

@ExtendWith(MockitoExtension.class)
class CredentialControllerTest {

    @Mock
    private CredentialClientService credentialClientService;

    @InjectMocks
    private CredentialController credentialController;

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;
    private CredentialDto credentialDto;
    private CredentialUserServiceCollectionDtoResponse collectionResponse;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(credentialController).build();
        objectMapper = new ObjectMapper();
        
        // Create test data
        setupTestData();
    }

    private void setupTestData() {
        // Create UserDto
        UserDto userDto = UserDto.builder()
                .userId(1)
                .firstName("John")
                .lastName("Doe")
                .email("john.doe@example.com")
                .build();

        // Create CredentialDto
        credentialDto = CredentialDto.builder()
                .credentialId(1)
                .username("testuser")
                .password("password123")
                .roleBasedAuthority(RoleBasedAuthority.ROLE_USER)
                .isEnabled(true)
                .isAccountNonExpired(true)
                .isAccountNonLocked(true)
                .isCredentialsNonExpired(true)
                .userDto(userDto)
                .build();

        // Create collection response
        collectionResponse = CredentialUserServiceCollectionDtoResponse.builder()
                .collection(Arrays.asList(credentialDto))
                .build();
    }

    @Test
    void testFindAll_ShouldReturnAllCredentials() throws Exception {
        // Given
        when(credentialClientService.findAll()).thenReturn(ResponseEntity.ok(collectionResponse));

        // When & Then
        mockMvc.perform(get("/api/credentials")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.collection").isArray())
                .andExpect(jsonPath("$.collection[0].credentialId").value(1))
                .andExpect(jsonPath("$.collection[0].username").value("testuser"))
                .andExpect(jsonPath("$.collection[0].roleBasedAuthority").value("ROLE_USER"))
                .andExpect(jsonPath("$.collection[0].user.userId").value(1));
    }

    @Test
    void testFindById_ShouldReturnCredentialById() throws Exception {
        // Given
        String credentialId = "1";
        when(credentialClientService.findById(credentialId)).thenReturn(ResponseEntity.ok(credentialDto));

        // When & Then
        mockMvc.perform(get("/api/credentials/{credentialId}", credentialId)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.credentialId").value(1))
                .andExpect(jsonPath("$.username").value("testuser"))
                .andExpect(jsonPath("$.roleBasedAuthority").value("ROLE_USER"))
                .andExpect(jsonPath("$.isEnabled").value(true))
                .andExpect(jsonPath("$.user.firstName").value("John"));
    }

    @Test
    void testFindByUsername_ShouldReturnCredentialByUsername() throws Exception {
        // Given
        String username = "testuser";
        when(credentialClientService.findByUsername(username)).thenReturn(ResponseEntity.ok(credentialDto));

        // When & Then
        mockMvc.perform(get("/api/credentials/username/{username}", username)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.credentialId").value(1))
                .andExpect(jsonPath("$.username").value("testuser"))
                .andExpect(jsonPath("$.user.email").value("john.doe@example.com"));
    }

    @Test
    void testSave_ShouldCreateNewCredential() throws Exception {
        // Given
        CredentialDto newCredential = CredentialDto.builder()
                .username("newuser")
                .password("newpass123")
                .roleBasedAuthority(RoleBasedAuthority.ROLE_ADMIN)
                .build();

        CredentialDto savedCredential = CredentialDto.builder()
                .credentialId(2)
                .username("newuser")
                .password("newpass123")
                .roleBasedAuthority(RoleBasedAuthority.ROLE_ADMIN)
                .isEnabled(true)
                .isAccountNonExpired(true)
                .isAccountNonLocked(true)
                .isCredentialsNonExpired(true)
                .build();

        when(credentialClientService.save(any(CredentialDto.class))).thenReturn(ResponseEntity.ok(savedCredential));

        // When & Then
        mockMvc.perform(post("/api/credentials")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(newCredential)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.credentialId").value(2))
                .andExpect(jsonPath("$.username").value("newuser"))
                .andExpect(jsonPath("$.roleBasedAuthority").value("ROLE_ADMIN"));
    }

    @Test
    void testUpdate_WithoutPathVariable_ShouldUpdateCredential() throws Exception {
        // Given
        CredentialDto updatedCredential = CredentialDto.builder()
                .credentialId(1)
                .username("updateduser")
                .password("updatedpass123")
                .roleBasedAuthority(RoleBasedAuthority.ROLE_ADMIN)
                .build();

        when(credentialClientService.update(any(CredentialDto.class))).thenReturn(ResponseEntity.ok(updatedCredential));

        // When & Then
        mockMvc.perform(put("/api/credentials")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updatedCredential)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.credentialId").value(1))
                .andExpect(jsonPath("$.username").value("updateduser"))
                .andExpect(jsonPath("$.roleBasedAuthority").value("ROLE_ADMIN"));
    }

    @Test
    void testUpdate_WithPathVariable_ShouldUpdateCredential() throws Exception {
        // Given
        String credentialId = "1";
        CredentialDto updatedCredential = CredentialDto.builder()
                .credentialId(1)
                .username("pathupdated")
                .password("pathupdated123")
                .roleBasedAuthority(RoleBasedAuthority.ROLE_USER)
                .build();

        when(credentialClientService.update(any(CredentialDto.class))).thenReturn(ResponseEntity.ok(updatedCredential));

        // When & Then
        mockMvc.perform(put("/api/credentials/{credentialId}", credentialId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updatedCredential)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.credentialId").value(1))
                .andExpect(jsonPath("$.username").value("pathupdated"))
                .andExpect(jsonPath("$.roleBasedAuthority").value("ROLE_USER"));
    }

    @Test
    void testDeleteById_ShouldDeleteCredential() throws Exception {
        // Given
        String credentialId = "1";
        when(credentialClientService.deleteById(credentialId)).thenReturn(ResponseEntity.ok(true));

        // When & Then
        mockMvc.perform(delete("/api/credentials/{credentialId}", credentialId)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().string("true"));
    }

    @Test
    void testDeleteById_WhenDeleteFails_ShouldReturnFalse() throws Exception {
        // Given
        String credentialId = "999";
        when(credentialClientService.deleteById(credentialId)).thenReturn(ResponseEntity.ok(false));

        // When & Then
        mockMvc.perform(delete("/api/credentials/{credentialId}", credentialId)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().string("false"));
    }

    @Test
    void testFindById_WithNullResponse_ShouldHandleGracefully() throws Exception {
        // Given
        String credentialId = "999";
        when(credentialClientService.findById(credentialId)).thenReturn(ResponseEntity.ok(null));

        // When & Then
        mockMvc.perform(get("/api/credentials/{credentialId}", credentialId)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().string(""));
    }

    @Test
    void testFindByUsername_WithNullResponse_ShouldHandleGracefully() throws Exception {
        // Given
        String username = "nonexistent";
        when(credentialClientService.findByUsername(username)).thenReturn(ResponseEntity.ok(null));

        // When & Then
        mockMvc.perform(get("/api/credentials/username/{username}", username)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().string(""));
    }

    @Test
    void testSave_WithCompleteCredentialData_ShouldCreateCredentialWithAllFields() throws Exception {
        // Given
        when(credentialClientService.save(any(CredentialDto.class))).thenReturn(ResponseEntity.ok(credentialDto));

        // When & Then
        mockMvc.perform(post("/api/credentials")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(credentialDto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.credentialId").value(1))
                .andExpect(jsonPath("$.username").value("testuser"))
                .andExpect(jsonPath("$.roleBasedAuthority").value("ROLE_USER"))
                .andExpect(jsonPath("$.isEnabled").value(true))
                .andExpect(jsonPath("$.isAccountNonExpired").value(true))
                .andExpect(jsonPath("$.isAccountNonLocked").value(true))
                .andExpect(jsonPath("$.isCredentialsNonExpired").value(true))
                .andExpect(jsonPath("$.user.userId").value(1));
    }
}