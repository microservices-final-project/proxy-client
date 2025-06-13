package com.selimhorri.app.business.auth.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;

import com.selimhorri.app.business.auth.model.request.AuthenticationRequest;
import com.selimhorri.app.business.auth.model.response.AuthenticationResponse;
import com.selimhorri.app.business.auth.service.impl.AuthenticationServiceImpl;
import com.selimhorri.app.exception.wrapper.IllegalAuthenticationCredentialsException;
import com.selimhorri.app.jwt.service.JwtService;

@ExtendWith(MockitoExtension.class)
class AuthenticationServiceImplTest {

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private UserDetailsService userDetailsService;

    @Mock
    private JwtService jwtService;

    @Mock
    private UserDetails userDetails;

    @InjectMocks
    private AuthenticationServiceImpl authenticationService;

    private AuthenticationRequest validRequest;
    private AuthenticationRequest invalidRequest;

    @BeforeEach
    void setUp() {
        validRequest = new AuthenticationRequest("validUser", "validPass");
        invalidRequest = new AuthenticationRequest("invalidUser", "invalidPass");
    }

    @Test
    void authenticate_ShouldThrowIllegalAuthenticationCredentialsException_WhenCredentialsAreInvalid() {
        // Arrange
        doThrow(BadCredentialsException.class)
            .when(authenticationManager)
            .authenticate(new UsernamePasswordAuthenticationToken(
                invalidRequest.getUsername(), 
                invalidRequest.getPassword()));

        // Act & Assert
        assertThrows(IllegalAuthenticationCredentialsException.class, () -> {
            authenticationService.authenticate(invalidRequest);
        });

        verify(authenticationManager).authenticate(any(UsernamePasswordAuthenticationToken.class));

    }

}