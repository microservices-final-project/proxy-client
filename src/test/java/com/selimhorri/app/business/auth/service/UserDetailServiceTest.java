package com.selimhorri.app.business.auth.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import com.selimhorri.app.business.auth.service.impl.UserDetailsServiceImpl;
import com.selimhorri.app.business.user.model.CredentialDto;
import com.selimhorri.app.business.user.model.RoleBasedAuthority;
import com.selimhorri.app.constant.AppConstant;

@ExtendWith(MockitoExtension.class)
@DisplayName("UserDetailsService Implementation Tests")
class UserDetailsServiceImplTest {

    @Mock
    private RestTemplate restTemplate;

    @InjectMocks
    private UserDetailsServiceImpl userDetailsService;

    private static final String TEST_USERNAME = "testuser";
    private static final String API_URL = AppConstant.DiscoveredDomainsApi.USER_SERVICE_HOST + "/api/credentials/username/" + TEST_USERNAME;
    
    private CredentialDto credentialDto;

    @BeforeEach
    void setUp() {
        credentialDto = CredentialDto.builder()
                .credentialId(1)
                .username("testuser")
                .password("encodedPassword")
                .roleBasedAuthority(RoleBasedAuthority.ROLE_USER)
                .isEnabled(true)
                .isAccountNonExpired(true)
                .isAccountNonLocked(true)
                .isCredentialsNonExpired(true)
                .build();
    }

    @Test
    @DisplayName("Should successfully load user by username when user exists")
    void loadUserByUsername_WhenUserExists_ShouldReturnUserDetails() {
        // Given
        when(restTemplate.getForObject(anyString(), eq(CredentialDto.class)))
                .thenReturn(credentialDto);

        // When
        UserDetails result = userDetailsService.loadUserByUsername(TEST_USERNAME);

        // Then
        assertNotNull(result);
        assertEquals(TEST_USERNAME, result.getUsername());
        assertEquals("encodedPassword", result.getPassword());
        assertTrue(result.isEnabled());
        assertTrue(result.isAccountNonExpired());
        assertTrue(result.isAccountNonLocked());
        assertTrue(result.isCredentialsNonExpired());
        assertEquals(1, result.getAuthorities().size());
        
        verify(restTemplate, times(1)).getForObject(
                AppConstant.DiscoveredDomainsApi.USER_SERVICE_HOST + "/api/credentials/username/" + TEST_USERNAME,
                CredentialDto.class
        );
    }

    @Test
    @DisplayName("Should throw UsernameNotFoundException when user does not exist")
    void loadUserByUsername_WhenUserDoesNotExist_ShouldThrowUsernameNotFoundException() {
        // Given
        when(restTemplate.getForObject(anyString(), eq(CredentialDto.class)))
                .thenReturn(null);

        // When & Then
        UsernameNotFoundException exception = assertThrows(
                UsernameNotFoundException.class,
                () -> userDetailsService.loadUserByUsername(TEST_USERNAME)
        );

        assertEquals("User not found with username: " + TEST_USERNAME, exception.getMessage());
        
        verify(restTemplate, times(1)).getForObject(
                AppConstant.DiscoveredDomainsApi.USER_SERVICE_HOST + "/api/credentials/username/" + TEST_USERNAME,
                CredentialDto.class
        );
    }

    @Test
    @DisplayName("Should throw UsernameNotFoundException when RestTemplate throws RestClientException")
    void loadUserByUsername_WhenRestClientExceptionOccurs_ShouldThrowUsernameNotFoundException() {
        // Given
        RestClientException restClientException = new RestClientException("Connection failed");
        when(restTemplate.getForObject(anyString(), eq(CredentialDto.class)))
                .thenThrow(restClientException);

        // When & Then
        UsernameNotFoundException exception = assertThrows(
                UsernameNotFoundException.class,
                () -> userDetailsService.loadUserByUsername(TEST_USERNAME)
        );

        assertEquals("Failed to load user with username: " + TEST_USERNAME, exception.getMessage());
        assertEquals(restClientException, exception.getCause());
        
        verify(restTemplate, times(1)).getForObject(
                AppConstant.DiscoveredDomainsApi.USER_SERVICE_HOST + "/api/credentials/username/" + TEST_USERNAME,
                CredentialDto.class
        );
    }

    @Test
    @DisplayName("Should handle empty username gracefully")
    void loadUserByUsername_WhenUsernameIsEmpty_ShouldStillCallRestTemplate() {
        // Given
        String emptyUsername = "";
        when(restTemplate.getForObject(anyString(), eq(CredentialDto.class)))
                .thenReturn(null);

        // When & Then
        UsernameNotFoundException exception = assertThrows(
                UsernameNotFoundException.class,
                () -> userDetailsService.loadUserByUsername(emptyUsername)
        );

        assertEquals("User not found with username: " + emptyUsername, exception.getMessage());
        
        verify(restTemplate, times(1)).getForObject(
                AppConstant.DiscoveredDomainsApi.USER_SERVICE_HOST + "/api/credentials/username/" + emptyUsername,
                CredentialDto.class
        );
    }

    @Test
    @DisplayName("Should handle null username")
    void loadUserByUsername_WhenUsernameIsNull_ShouldStillCallRestTemplate() {
        // Given
        String nullUsername = null;
        when(restTemplate.getForObject(anyString(), eq(CredentialDto.class)))
                .thenReturn(null);

        // When & Then
        UsernameNotFoundException exception = assertThrows(
                UsernameNotFoundException.class,
                () -> userDetailsService.loadUserByUsername(nullUsername)
        );

        assertEquals("User not found with username: " + nullUsername, exception.getMessage());
        
        verify(restTemplate, times(1)).getForObject(
                AppConstant.DiscoveredDomainsApi.USER_SERVICE_HOST + "/api/credentials/username/" + nullUsername,
                CredentialDto.class
        );
    }

    @Test
    @DisplayName("Should successfully load admin user")
    void loadUserByUsername_WhenUserIsAdmin_ShouldReturnUserDetailsWithAdminRole() {
        // Given
        CredentialDto adminCredential = CredentialDto.builder()
                .credentialId(2)
                .username("admin")
                .password("adminPassword")
                .roleBasedAuthority(RoleBasedAuthority.ROLE_ADMIN)
                .isEnabled(true)
                .isAccountNonExpired(true)
                .isAccountNonLocked(true)
                .isCredentialsNonExpired(true)
                .build();

        when(restTemplate.getForObject(anyString(), eq(CredentialDto.class)))
                .thenReturn(adminCredential);

        // When
        UserDetails result = userDetailsService.loadUserByUsername("admin");

        // Then
        assertNotNull(result);
        assertEquals("admin", result.getUsername());
        assertEquals("adminPassword", result.getPassword());
        assertTrue(result.isEnabled());
        assertTrue(result.getAuthorities().stream()
                .anyMatch(auth -> auth.getAuthority().equals("ROLE_ADMIN")));
        
        verify(restTemplate, times(1)).getForObject(
                AppConstant.DiscoveredDomainsApi.USER_SERVICE_HOST + "/api/credentials/username/admin",
                CredentialDto.class
        );
    }

    @Test
    @DisplayName("Should handle disabled user account")
    void loadUserByUsername_WhenUserIsDisabled_ShouldReturnUserDetailsWithDisabledStatus() {
        // Given
        CredentialDto disabledCredential = CredentialDto.builder()
                .credentialId(3)
                .username("disableduser")
                .password("password")
                .roleBasedAuthority(RoleBasedAuthority.ROLE_USER)
                .isEnabled(false)
                .isAccountNonExpired(true)
                .isAccountNonLocked(true)
                .isCredentialsNonExpired(true)
                .build();

        when(restTemplate.getForObject(anyString(), eq(CredentialDto.class)))
                .thenReturn(disabledCredential);

        // When
        UserDetails result = userDetailsService.loadUserByUsername("disableduser");

        // Then
        assertNotNull(result);
        assertEquals("disableduser", result.getUsername());
        assertFalse(result.isEnabled());
        assertTrue(result.isAccountNonExpired());
        assertTrue(result.isAccountNonLocked());
        assertTrue(result.isCredentialsNonExpired());
        
        verify(restTemplate, times(1)).getForObject(
                AppConstant.DiscoveredDomainsApi.USER_SERVICE_HOST + "/api/credentials/username/disableduser",
                CredentialDto.class
        );
    }

    @Test
    @DisplayName("Should construct correct API URL")
    void loadUserByUsername_ShouldConstructCorrectApiUrl() {
        // Given
        String testUser = "specialuser";
        when(restTemplate.getForObject(anyString(), eq(CredentialDto.class)))
                .thenReturn(credentialDto);

        // When
        userDetailsService.loadUserByUsername(testUser);

        // Then
        String expectedUrl = AppConstant.DiscoveredDomainsApi.USER_SERVICE_HOST + "/api/credentials/username/" + testUser;
        verify(restTemplate, times(1)).getForObject(expectedUrl, CredentialDto.class);
    }
}