package com.selimhorri.app.integration.businness.auth.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.selimhorri.app.business.auth.controller.AuthenticationController;
import com.selimhorri.app.business.auth.model.request.AuthenticationRequest;
import com.selimhorri.app.business.auth.model.response.AuthenticationResponse;
import com.selimhorri.app.business.auth.service.AuthenticationService;

@WebMvcTest(AuthenticationController.class)
public class AuthenticationControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AuthenticationService authenticationService;

    @Autowired
    private ObjectMapper objectMapper;

    private AuthenticationRequest validRequest;
    private AuthenticationRequest invalidRequest;
    private AuthenticationResponse successResponse;

    @BeforeEach
    void setUp() {
        // Setup test data
        validRequest = new AuthenticationRequest();
        validRequest.setUsername("testuser");
        validRequest.setPassword("testpass");

        invalidRequest = new AuthenticationRequest();
        invalidRequest.setUsername("");
        invalidRequest.setPassword(null);

        successResponse = new AuthenticationResponse();
        successResponse.setJwtToken("test.jwt.token");
    }

    @Test
    void testAuthenticate_Success() throws Exception {
        // Given
        when(authenticationService.authenticate(any(AuthenticationRequest.class)))
            .thenReturn(successResponse);

        // When & Then
        mockMvc.perform(post("/api/authenticate")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(validRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.jwtToken").value("test.jwt.token"));
    }

    @Test
    void testAuthenticate_InvalidUsername_Blank() throws Exception {
        // Given invalid request with blank username
        AuthenticationRequest request = new AuthenticationRequest();
        request.setUsername("");
        request.setPassword("testpass");

        // When & Then
        mockMvc.perform(post("/api/authenticate")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void testAuthenticate_InvalidPassword_Null() throws Exception {
        // Given invalid request with null password
        AuthenticationRequest request = new AuthenticationRequest();
        request.setUsername("testuser");
        request.setPassword(null);

        // When & Then
        mockMvc.perform(post("/api/authenticate")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void testAuthenticate_InvalidRequest_EmptyBody() throws Exception {
        // When & Then
        mockMvc.perform(post("/api/authenticate")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void testAuthenticate_InvalidRequest_MissingFields() throws Exception {
        // When & Then
        mockMvc.perform(post("/api/authenticate")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"username\":\"testuser\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void testAuthenticate_Unauthorized_InvalidCredentials() throws Exception {
        // Given
        when(authenticationService.authenticate(any(AuthenticationRequest.class)))
            .thenThrow(new RuntimeException("Invalid credentials"));

        // When & Then
        mockMvc.perform(post("/api/authenticate")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(validRequest)))
                .andExpect(status().isUnauthorized());
    }
}