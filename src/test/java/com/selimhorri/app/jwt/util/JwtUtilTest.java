package com.selimhorri.app.jwt.util;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.Date;
import java.util.function.Function;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UserDetails;

import com.selimhorri.app.jwt.util.impl.JwtUtilImpl;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.SignatureException;

@ExtendWith(MockitoExtension.class)
class JwtUtilImplTest {

    @InjectMocks
    private JwtUtilImpl jwtUtil;

    @Mock
    private UserDetails userDetails;

    private String validToken;
    private String username = "testuser";

    @BeforeEach
    void setUp() {
        when(userDetails.getUsername()).thenReturn(username);
        validToken = jwtUtil.generateToken(userDetails, "2");
    }

    @Test
    void testGenerateToken() {
        // Given
        when(userDetails.getUsername()).thenReturn(username);

        // When
        String token = jwtUtil.generateToken(userDetails, "2");

        // Then
        assertNotNull(token);
        assertFalse(token.isEmpty());
        assertTrue(token.contains("."));
    }

    @Test
    void testExtractUsername() {
        // Given
        String token = jwtUtil.generateToken(userDetails, "2");

        // When
        String extractedUsername = jwtUtil.extractUsername(token);

        // Then
        assertEquals(username, extractedUsername);
    }

    @Test
    void testExtractExpiration() {
        // Given
        String token = jwtUtil.generateToken(userDetails, "2");
        Date beforeGeneration = new Date();

        // When
        Date expiration = jwtUtil.extractExpiration(token);

        // Then
        assertNotNull(expiration);
        assertTrue(expiration.after(beforeGeneration));
        // El token debería expirar en 10 horas (1000 * 60 * 60 * 10 ms)
        long expectedExpirationTime = System.currentTimeMillis() + (1000 * 60 * 60 * 10);
        long actualExpirationTime = expiration.getTime();
        // Permitimos una diferencia de 5 segundos para el tiempo de procesamiento
        assertTrue(Math.abs(expectedExpirationTime - actualExpirationTime) < 5000);
    }

    @Test
    void testExtractClaims() {
        // Given
        String token = jwtUtil.generateToken(userDetails, "2");
        Function<Claims, String> claimsResolver = Claims::getSubject;

        // When
        String subject = jwtUtil.extractClaims(token, claimsResolver);

        // Then
        assertEquals(username, subject);
    }

    @Test
    void testExtractClaimsWithExpiration() {
        // Given
        String token = jwtUtil.generateToken(userDetails, "2");
        Function<Claims, Date> claimsResolver = Claims::getExpiration;

        // When
        Date expiration = jwtUtil.extractClaims(token, claimsResolver);

        // Then
        assertNotNull(expiration);
        assertTrue(expiration.after(new Date()));
    }

    @Test
    void testValidateTokenWithValidToken() {
        // Given
        String token = jwtUtil.generateToken(userDetails, "2");

        // When
        Boolean isValid = jwtUtil.validateToken(token, userDetails);

        // Then
        assertTrue(isValid);
    }

    @Test
    void testValidateTokenWithWrongUsername() {
        // Given
        String token = jwtUtil.generateToken(userDetails, "2");
        UserDetails wrongUserDetails = mock(UserDetails.class);
        when(wrongUserDetails.getUsername()).thenReturn("wronguser");

        // When
        Boolean isValid = jwtUtil.validateToken(token, wrongUserDetails);

        // Then
        assertFalse(isValid);
    }

    @Test
    void testExtractExpirationWithExpiredTokenThrowsException() {
        // Given - Token expirado que debería lanzar ExpiredJwtException al intentar parsearlo
        String expiredToken = io.jsonwebtoken.Jwts.builder()
                .setSubject(username)
                .setIssuedAt(new Date(System.currentTimeMillis() - 10000))
                .setExpiration(new Date(System.currentTimeMillis() - 5000))
                .signWith(io.jsonwebtoken.SignatureAlgorithm.HS256, "secret")
                .compact();

        // When & Then
        assertThrows(ExpiredJwtException.class, () -> {
            jwtUtil.extractExpiration(expiredToken);
        });
    }

    @Test
    void testExtractUsernameWithInvalidToken() {
        // Given
        String invalidToken = "invalid.token.here";

        // When & Then
        assertThrows(MalformedJwtException.class, () -> {
            jwtUtil.extractUsername(invalidToken);
        });
    }

    @Test
    void testExtractUsernameWithMalformedToken() {
        // Given
        String malformedToken = "eyJhbGciOiJIUzI1NiJ9.malformed";

        // When & Then
        assertThrows(MalformedJwtException.class, () -> {
            jwtUtil.extractUsername(malformedToken);
        });
    }

    @Test
    void testExtractExpirationWithInvalidToken() {
        // Given
        String invalidToken = "invalid.token.here";

        // When & Then
        assertThrows(MalformedJwtException.class, () -> {
            jwtUtil.extractExpiration(invalidToken);
        });
    }

    @Test
    void testValidateTokenWithInvalidToken() {
        // Given
        String invalidToken = "invalid.token.here";

        // When & Then
        assertThrows(MalformedJwtException.class, () -> {
            jwtUtil.validateToken(invalidToken, userDetails);
        });
    }

    @Test
    void testTokenContainsCorrectClaims() {
        // Given
        String token = jwtUtil.generateToken(userDetails, "2");

        // When
        String extractedUsername = jwtUtil.extractUsername(token);
        Date extractedExpiration = jwtUtil.extractExpiration(token);

        // Then
        assertEquals(username, extractedUsername);
        assertNotNull(extractedExpiration);
        assertTrue(extractedExpiration.after(new Date()));
    }

    @Test
    void testGenerateTokenWithNullUserDetails() {
        // When & Then
        assertThrows(NullPointerException.class, () -> {
            jwtUtil.generateToken(null, null);
        });
    }

    @Test
    void testValidateTokenWithNullToken() {
        // When & Then
        assertThrows(IllegalArgumentException.class, () -> {
            jwtUtil.validateToken(null, userDetails);
        });
    }

    @Test
    void testValidateTokenWithNullUserDetails() {
        // Given
        String token = jwtUtil.generateToken(userDetails, "2");

        // When & Then
        assertThrows(NullPointerException.class, () -> {
            jwtUtil.validateToken(token, null);
        });
    }

    @Test
    void testExtractClaimsWithNullToken() {
        // Given
        Function<Claims, String> claimsResolver = Claims::getSubject;

        // When & Then
        assertThrows(IllegalArgumentException.class, () -> {
            jwtUtil.extractClaims(null, claimsResolver);
        });
    }

    @Test
    void testConsistentTokenGeneration() {
        // Given
        UserDetails userDetails1 = mock(UserDetails.class);
        UserDetails userDetails2 = mock(UserDetails.class);
        when(userDetails1.getUsername()).thenReturn("user1");
        when(userDetails2.getUsername()).thenReturn("user1");

        // When
        String token1 = jwtUtil.generateToken(userDetails1, "2");
        String token2 = jwtUtil.generateToken(userDetails2, "2");

        // Then
        String username1 = jwtUtil.extractUsername(token1);
        String username2 = jwtUtil.extractUsername(token2);
        assertEquals(username1, username2);
    }
}