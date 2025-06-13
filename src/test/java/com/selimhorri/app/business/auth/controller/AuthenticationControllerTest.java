package com.selimhorri.app.business.auth.controller;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.selimhorri.app.business.auth.model.request.AuthenticationRequest;
import com.selimhorri.app.business.auth.model.response.AuthenticationResponse;
import com.selimhorri.app.business.auth.service.AuthenticationService;

@ExtendWith(MockitoExtension.class)
class AuthenticationControllerTest {

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    @Mock
    private AuthenticationService authenticationService;

    @InjectMocks
    private AuthenticationController authenticationController;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(authenticationController).build();
        objectMapper = new ObjectMapper();
    }

    @Test
    void authenticate_ShouldReturnJwtToken_WhenValidRequest() throws Exception {
        // Arrange
        AuthenticationRequest request = new AuthenticationRequest("username", "password");
        AuthenticationResponse response = AuthenticationResponse.builder()
                .jwtToken("test.jwt.token")
                .build();

        when(authenticationService.authenticate(request)).thenReturn(response);

        // Act & Assert
        mockMvc.perform(post("/api/authenticate")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.jwtToken").value("test.jwt.token"));
    }

    @Test
    void authenticate_ShouldReturnBadRequest_WhenInvalidRequest() throws Exception {
        // Arrange - empty request
        AuthenticationRequest request = new AuthenticationRequest("", "");

        // Act & Assert
        mockMvc.perform(post("/api/authenticate")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }
}