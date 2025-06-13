package com.selimhorri.app.jwt.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Date;
import java.util.function.Function;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.security.core.userdetails.UserDetails;

import com.selimhorri.app.jwt.service.impl.JwtServiceImpl;
import com.selimhorri.app.jwt.util.JwtUtil;

import io.jsonwebtoken.Claims;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("JwtServiceImpl - Unit Tests")
class JwtServiceImplTest {

    @Mock
    private JwtUtil jwtUtil;

    @Mock
    private UserDetails userDetails;

    @Mock
    private Claims claims;

    @InjectMocks
    private JwtServiceImpl jwtServiceImpl;

    // Testing against the interface
    private JwtService jwtService;

    private String validToken;
    private String username;
    private Date expirationDate;

    @BeforeEach
    void setUp() {
        // Initialize the service interface reference
        jwtService = jwtServiceImpl;
        
        // Setup test data
        validToken = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.test.token";
        username = "testuser@example.com";
        expirationDate = new Date(System.currentTimeMillis() + 3600000); // 1 hour from now
        
        // Setup UserDetails mock
        when(userDetails.getUsername()).thenReturn(username);
    }

    @Test
    @DisplayName("Should extract username from token successfully")
    void testExtractUsername_Success() {
        // Given
        when(jwtUtil.extractUsername(validToken)).thenReturn(username);

        // When
        String result = jwtService.extractUsername(validToken);

        // Then
        assertNotNull(result);
        assertEquals(username, result);
        verify(jwtUtil, times(1)).extractUsername(validToken);
    }

    @Test
    @DisplayName("Should extract username from different token")
    void testExtractUsername_DifferentToken() {
        // Given
        String differentToken = "different.jwt.token";
        String differentUsername = "differentuser@example.com";
        when(jwtUtil.extractUsername(differentToken)).thenReturn(differentUsername);

        // When
        String result = jwtService.extractUsername(differentToken);

        // Then
        assertNotNull(result);
        assertEquals(differentUsername, result);
        verify(jwtUtil, times(1)).extractUsername(differentToken);
    }

    @Test
    @DisplayName("Should extract expiration date from token successfully")
    void testExtractExpiration_Success() {
        // Given
        when(jwtUtil.extractExpiration(validToken)).thenReturn(expirationDate);

        // When
        Date result = jwtService.extractExpiration(validToken);

        // Then
        assertNotNull(result);
        assertEquals(expirationDate, result);
        verify(jwtUtil, times(1)).extractExpiration(validToken);
    }

    @Test
    @DisplayName("Should extract expiration date from different token")
    void testExtractExpiration_DifferentToken() {
        // Given
        String differentToken = "another.jwt.token";
        Date differentExpirationDate = new Date(System.currentTimeMillis() + 7200000); // 2 hours from now
        when(jwtUtil.extractExpiration(differentToken)).thenReturn(differentExpirationDate);

        // When
        Date result = jwtService.extractExpiration(differentToken);

        // Then
        assertNotNull(result);
        assertEquals(differentExpirationDate, result);
        verify(jwtUtil, times(1)).extractExpiration(differentToken);
    }

    @Test
    @DisplayName("Should extract claims using function resolver successfully")
    void testExtractClaims_Success() {
        // Given
        Function<Claims, String> claimsResolver = Claims::getSubject;
        String expectedSubject = "test-subject";
        when(jwtUtil.extractClaims(eq(validToken), any(Function.class))).thenReturn(expectedSubject);

        // When
        String result = jwtService.extractClaims(validToken, claimsResolver);

        // Then
        assertNotNull(result);
        assertEquals(expectedSubject, result);
        verify(jwtUtil, times(1)).extractClaims(eq(validToken), eq(claimsResolver));
    }

    @Test
    @DisplayName("Should extract claims with different resolver function")
    void testExtractClaims_DifferentResolver() {
        // Given
        Function<Claims, Date> claimsResolver = Claims::getExpiration;
        Date expectedExpiration = new Date();
        when(jwtUtil.extractClaims(eq(validToken), any(Function.class))).thenReturn(expectedExpiration);

        // When
        Date result = jwtService.extractClaims(validToken, claimsResolver);

        // Then
        assertNotNull(result);
        assertEquals(expectedExpiration, result);
        verify(jwtUtil, times(1)).extractClaims(eq(validToken), eq(claimsResolver));
    }

    @Test
    @DisplayName("Should extract claims returning Integer type")
    void testExtractClaims_IntegerReturn() {
        // Given
        Function<Claims, Integer> claimsResolver = claims -> 12345;
        Integer expectedValue = 12345;
        when(jwtUtil.extractClaims(eq(validToken), any(Function.class))).thenReturn(expectedValue);

        // When
        Integer result = jwtService.extractClaims(validToken, claimsResolver);

        // Then
        assertNotNull(result);
        assertEquals(expectedValue, result);
        verify(jwtUtil, times(1)).extractClaims(eq(validToken), eq(claimsResolver));
    }

    @Test
    @DisplayName("Should generate token from user details successfully")
    void testGenerateToken_Success() {
        // Given
        String expectedToken = "generated.jwt.token.for.user";
        when(jwtUtil.generateToken(userDetails, "2")).thenReturn(expectedToken);

        // When
        String result = jwtService.generateToken(userDetails, "2");

        // Then
        assertNotNull(result);
        assertEquals(expectedToken, result);
        verify(jwtUtil, times(1)).generateToken(userDetails, "2");
    }

    @Test
    @DisplayName("Should generate token for different user details")
    void testGenerateToken_DifferentUserDetails() {
        // Given
        UserDetails differentUser = org.mockito.Mockito.mock(UserDetails.class);
        when(differentUser.getUsername()).thenReturn("anotheruser@example.com");
        String expectedToken = "different.generated.jwt.token";
        when(jwtUtil.generateToken(differentUser, "2")).thenReturn(expectedToken);

        // When
        String result = jwtService.generateToken(differentUser, "2");

        // Then
        assertNotNull(result);
        assertEquals(expectedToken, result);
        verify(jwtUtil, times(1)).generateToken(differentUser, "2");
    }

    @Test
    @DisplayName("Should validate token successfully when token is valid")
    void testValidateToken_ValidToken() {
        // Given
        when(jwtUtil.validateToken(validToken, userDetails)).thenReturn(true);

        // When
        Boolean result = jwtService.validateToken(validToken, userDetails);

        // Then
        assertNotNull(result);
        assertTrue(result);
        verify(jwtUtil, times(1)).validateToken(validToken, userDetails);
    }

    @Test
    @DisplayName("Should validate token and return false when token is invalid")
    void testValidateToken_InvalidToken() {
        // Given
        String invalidToken = "invalid.jwt.token";
        when(jwtUtil.validateToken(invalidToken, userDetails)).thenReturn(false);

        // When
        Boolean result = jwtService.validateToken(invalidToken, userDetails);

        // Then
        assertNotNull(result);
        assertFalse(result);
        verify(jwtUtil, times(1)).validateToken(invalidToken, userDetails);
    }

    @Test
    @DisplayName("Should validate token with different user details")
    void testValidateToken_DifferentUserDetails() {
        // Given
        UserDetails differentUser = org.mockito.Mockito.mock(UserDetails.class);
        when(differentUser.getUsername()).thenReturn("wronguser@example.com");
        when(jwtUtil.validateToken(validToken, differentUser)).thenReturn(false);

        // When
        Boolean result = jwtService.validateToken(validToken, differentUser);

        // Then
        assertNotNull(result);
        assertFalse(result);
        verify(jwtUtil, times(1)).validateToken(validToken, differentUser);
    }

    @Test
    @DisplayName("Should handle null token gracefully in extractUsername")
    void testExtractUsername_NullToken() {
        // Given
        String nullToken = null;
        when(jwtUtil.extractUsername(nullToken)).thenReturn(null);

        // When
        String result = jwtService.extractUsername(nullToken);

        // Then
        assertEquals(null, result);
        verify(jwtUtil, times(1)).extractUsername(nullToken);
    }

    @Test
    @DisplayName("Should handle empty token in extractUsername")
    void testExtractUsername_EmptyToken() {
        // Given
        String emptyToken = "";
        when(jwtUtil.extractUsername(emptyToken)).thenReturn("");

        // When
        String result = jwtService.extractUsername(emptyToken);

        // Then
        assertEquals("", result);
        verify(jwtUtil, times(1)).extractUsername(emptyToken);
    }

    @Test
    @DisplayName("Should handle expired token in extractExpiration")
    void testExtractExpiration_ExpiredToken() {
        // Given
        Date pastDate = new Date(System.currentTimeMillis() - 3600000); // 1 hour ago
        when(jwtUtil.extractExpiration(validToken)).thenReturn(pastDate);

        // When
        Date result = jwtService.extractExpiration(validToken);

        // Then
        assertNotNull(result);
        assertEquals(pastDate, result);
        assertTrue(result.before(new Date())); // Verify it's in the past
        verify(jwtUtil, times(1)).extractExpiration(validToken);
    }

    @Test
    @DisplayName("Should verify all service methods delegate to JwtUtil correctly")
    void testServiceDelegation() {
        // Given
        Function<Claims, String> claimsResolver = Claims::getSubject;
        String expectedUsername = "delegationtest@example.com";
        Date expectedExpiration = new Date();
        String expectedClaim = "test-claim";
        String expectedToken = "delegation.test.token";
        Boolean expectedValidation = true;

        when(jwtUtil.extractUsername(validToken)).thenReturn(expectedUsername);
        when(jwtUtil.extractExpiration(validToken)).thenReturn(expectedExpiration);
        when(jwtUtil.extractClaims(eq(validToken), any(Function.class))).thenReturn(expectedClaim);
        when(jwtUtil.generateToken(userDetails, "2")).thenReturn(expectedToken);
        when(jwtUtil.validateToken(validToken, userDetails)).thenReturn(expectedValidation);

        // When - Call all service methods
        String usernameResult = jwtService.extractUsername(validToken);
        Date expirationResult = jwtService.extractExpiration(validToken);
        String claimResult = jwtService.extractClaims(validToken, claimsResolver);
        String tokenResult = jwtService.generateToken(userDetails, "2");
        Boolean validationResult = jwtService.validateToken(validToken, userDetails);

        // Then - Verify all results and delegations
        assertEquals(expectedUsername, usernameResult);
        assertEquals(expectedExpiration, expirationResult);
        assertEquals(expectedClaim, claimResult);
        assertEquals(expectedToken, tokenResult);
        assertEquals(expectedValidation, validationResult);

        verify(jwtUtil, times(1)).extractUsername(validToken);
        verify(jwtUtil, times(1)).extractExpiration(validToken);
        verify(jwtUtil, times(1)).extractClaims(eq(validToken), eq(claimsResolver));
        verify(jwtUtil, times(1)).generateToken(userDetails, "2");
        verify(jwtUtil, times(1)).validateToken(validToken, userDetails);
    }

    @Test
    @DisplayName("Should handle complex claims extraction scenarios")
    void testComplexClaimsExtraction() {
        // Given
        String complexToken = "complex.jwt.token.with.multiple.claims";
        
        // Test extracting different claim types
        Function<Claims, String> stringResolver = Claims::getIssuer;
        Function<Claims, Date> dateResolver = Claims::getIssuedAt;
        Function<Claims, Object> objectResolver = claims -> claims.get("customClaim");
        
        when(jwtUtil.extractClaims(eq(complexToken), eq(stringResolver))).thenReturn("test-issuer");
        when(jwtUtil.extractClaims(eq(complexToken), eq(dateResolver))).thenReturn(new Date());
        when(jwtUtil.extractClaims(eq(complexToken), eq(objectResolver))).thenReturn("custom-value");

        // When
        String issuer = jwtService.extractClaims(complexToken, stringResolver);
        Date issuedAt = jwtService.extractClaims(complexToken, dateResolver);
        Object customClaim = jwtService.extractClaims(complexToken, objectResolver);

        // Then
        assertNotNull(issuer);
        assertNotNull(issuedAt);
        assertNotNull(customClaim);
        assertEquals("test-issuer", issuer);
        assertEquals("custom-value", customClaim);
        
        verify(jwtUtil, times(1)).extractClaims(eq(complexToken), eq(stringResolver));
        verify(jwtUtil, times(1)).extractClaims(eq(complexToken), eq(dateResolver));
        verify(jwtUtil, times(1)).extractClaims(eq(complexToken), eq(objectResolver));
    }
}