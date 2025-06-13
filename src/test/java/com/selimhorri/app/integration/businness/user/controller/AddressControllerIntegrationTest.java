package com.selimhorri.app.integration.businness.user.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

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
import com.selimhorri.app.business.user.controller.AddressController;
import com.selimhorri.app.business.user.model.AddressDto;
import com.selimhorri.app.business.user.model.UserDto;
import com.selimhorri.app.business.user.model.response.AddressUserServiceCollectionDtoResponse;
import com.selimhorri.app.business.user.service.AddressClientService;
import com.selimhorri.app.config.template.TemplateConfig;
import com.selimhorri.app.jwt.service.JwtService;
import com.selimhorri.app.jwt.util.JwtUtil;
import com.selimhorri.app.security.SecurityConfig;

@WebMvcTest(AddressController.class)
@Import({ TemplateConfig.class, SecurityConfig.class })
@Tag("integration")
class AddressControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AddressClientService addressClientService;

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

    private AddressDto testAddress;
    private AddressUserServiceCollectionDtoResponse collectionResponse;

    @BeforeEach
    void setUp() {
        // Setup test data
        testAddress = createTestAddress();
        collectionResponse = new AddressUserServiceCollectionDtoResponse();
        collectionResponse.setCollection(Collections.singletonList(testAddress));
    }

    private AddressDto createTestAddress() {
        UserDto user = new UserDto();
        user.setUserId(1);
        user.setFirstName("John");
        user.setLastName("Doe");
        user.setEmail("john.doe@example.com");

        AddressDto address = new AddressDto();
        address.setAddressId(1);
        address.setFullAddress("123 Main St");
        address.setPostalCode("12345");
        address.setCity("Anytown");
        address.setUserDto(user);

        return address;
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void testFindAll_Success() throws Exception {
        // Given
        when(addressClientService.findAll()).thenReturn(ResponseEntity.ok(collectionResponse));

        // When & Then
        mockMvc.perform(get("/api/address")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.collection").isArray())
                .andExpect(jsonPath("$.collection[0].addressId").value(1))
                .andExpect(jsonPath("$.collection[0].fullAddress").value("123 Main St"))
                .andExpect(jsonPath("$.collection[0].user.userId").value(1));
    }

    @Test
    @WithMockUser(username = "johndoe", roles = "USER")
    void testFindById_Success_SameUser() throws Exception {
        // Given
        String addressId = "1";
        UserDetails userDetails = new User("johndoe", "password",
                Arrays.asList(new SimpleGrantedAuthority("ROLE_USER")));

        when(authUtil.getOwner(addressId, ResourceType.ADDRESSES)).thenReturn("1");
        doNothing().when(authUtil).canActivate(any(HttpServletRequest.class), eq("1"), any(UserDetails.class));
        when(addressClientService.findById(addressId)).thenReturn(ResponseEntity.ok(testAddress));

        // When & Then
        mockMvc.perform(get("/api/address/{addressId}", addressId)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.addressId").value(1))
                .andExpect(jsonPath("$.fullAddress").value("123 Main St"))
                .andExpect(jsonPath("$.user.firstName").value("John"));
    }

    @Test
    @WithMockUser(username = "admin", roles = "ADMIN")
    void testFindById_Success_Admin() throws Exception {
        // Given
        String addressId = "1";
        UserDetails userDetails = new User("admin", "password",
                Arrays.asList(new SimpleGrantedAuthority("ROLE_ADMIN")));

        when(authUtil.getOwner(addressId, ResourceType.ADDRESSES)).thenReturn("1");
        doNothing().when(authUtil).canActivate(any(HttpServletRequest.class), eq("1"), any(UserDetails.class));
        when(addressClientService.findById(addressId)).thenReturn(ResponseEntity.ok(testAddress));

        // When & Then
        mockMvc.perform(get("/api/address/{addressId}", addressId)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.addressId").value(1));
    }

    @Test
    @WithMockUser(username = "johndoe", roles = "USER")
    void testSave_Success() throws Exception {
        // Given
        AddressDto newAddress = new AddressDto();
        newAddress.setFullAddress("456 Oak Ave");
        newAddress.setPostalCode("67890");
        newAddress.setCity("Othertown");

        UserDto user = new UserDto();
        user.setUserId(1);
        newAddress.setUserDto(user);

        AddressDto savedAddress = new AddressDto();
        savedAddress.setAddressId(2);
        savedAddress.setFullAddress("456 Oak Ave");
        savedAddress.setPostalCode("67890");
        savedAddress.setCity("Othertown");
        savedAddress.setUserDto(user);

        doNothing().when(authUtil).canActivate(any(HttpServletRequest.class), eq("1"), any(UserDetails.class));
        when(addressClientService.save(any(AddressDto.class))).thenReturn(ResponseEntity.ok(savedAddress));

        // When & Then
        mockMvc.perform(post("/api/address")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(newAddress)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.addressId").value(2))
                .andExpect(jsonPath("$.fullAddress").value("456 Oak Ave"))
                .andExpect(jsonPath("$.user.userId").value(1));
    }

    @Test
    @WithMockUser(username = "johndoe", roles = "USER")
    void testUpdate_Success() throws Exception {
        // Given
        String addressId = "1";
        AddressDto updatedAddress = new AddressDto();
        updatedAddress.setFullAddress("Updated Address");
        updatedAddress.setPostalCode("54321");
        updatedAddress.setCity("Updated City");

        UserDto user = new UserDto();
        user.setUserId(1);
        updatedAddress.setUserDto(user);

        AddressDto savedAddress = new AddressDto();
        savedAddress.setAddressId(1);
        savedAddress.setFullAddress("Updated Address");
        savedAddress.setPostalCode("54321");
        savedAddress.setCity("Updated City");
        savedAddress.setUserDto(user);

        when(authUtil.getOwner(addressId, ResourceType.ADDRESSES)).thenReturn("1");
        doNothing().when(authUtil).canActivate(any(HttpServletRequest.class), eq("1"), any(UserDetails.class));
        when(addressClientService.update(eq(addressId), any(AddressDto.class))).thenReturn(ResponseEntity.ok(savedAddress));

        // When & Then
        mockMvc.perform(put("/api/address/{addressId}", addressId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updatedAddress)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.addressId").value(1))
                .andExpect(jsonPath("$.fullAddress").value("Updated Address"));
    }

    @Test
    @WithMockUser(username = "johndoe", roles = "USER")
    void testDeleteById_Success() throws Exception {
        // Given
        String addressId = "1";
        UserDetails userDetails = new User("johndoe", "password",
                Arrays.asList(new SimpleGrantedAuthority("ROLE_USER")));

        when(authUtil.getOwner(addressId, ResourceType.ADDRESSES)).thenReturn("1");
        doNothing().when(authUtil).canActivate(any(HttpServletRequest.class), eq("1"), any(UserDetails.class));
        when(addressClientService.deleteById(addressId)).thenReturn(ResponseEntity.ok(true));

        // When & Then
        mockMvc.perform(delete("/api/address/{addressId}", addressId)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").value(true));
    }

    @Test
    @WithMockUser(roles = "USER")
    void testFindAll_Forbidden_UserRole() throws Exception {
        // When & Then
        mockMvc.perform(get("/api/address")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "admin", roles = "ADMIN")
    void testSave_AsAdmin_Success() throws Exception {
        // Given
        AddressDto newAddress = new AddressDto();
        newAddress.setFullAddress("Admin Address");
        newAddress.setPostalCode("99999");
        newAddress.setCity("Admin City");

        UserDto user = new UserDto();
        user.setUserId(2);
        newAddress.setUserDto(user);

        AddressDto savedAddress = new AddressDto();
        savedAddress.setAddressId(3);
        savedAddress.setFullAddress("Admin Address");
        savedAddress.setPostalCode("99999");
        savedAddress.setCity("Admin City");
        savedAddress.setUserDto(user);

        doNothing().when(authUtil).canActivate(any(HttpServletRequest.class), eq("2"), any(UserDetails.class));
        when(addressClientService.save(any(AddressDto.class))).thenReturn(ResponseEntity.ok(savedAddress));

        // When & Then
        mockMvc.perform(post("/api/address")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(newAddress)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.addressId").value(3))
                .andExpect(jsonPath("$.fullAddress").value("Admin Address"));
    }

    @Test
    @WithMockUser(username = "admin", roles = "ADMIN")
    void testUpdate_AsAdmin_Success() throws Exception {
        // Given
        String addressId = "1";
        AddressDto updatedAddress = new AddressDto();
        updatedAddress.setFullAddress("Admin Updated Address");
        updatedAddress.setPostalCode("88888");
        updatedAddress.setCity("Admin Updated City");

        UserDto user = new UserDto();
        user.setUserId(1);
        updatedAddress.setUserDto(user);

        AddressDto savedAddress = new AddressDto();
        savedAddress.setAddressId(1);
        savedAddress.setFullAddress("Admin Updated Address");
        savedAddress.setPostalCode("88888");
        savedAddress.setCity("Admin Updated City");
        savedAddress.setUserDto(user);

        when(authUtil.getOwner(addressId, ResourceType.ADDRESSES)).thenReturn("1");
        doNothing().when(authUtil).canActivate(any(HttpServletRequest.class), eq("1"), any(UserDetails.class));
        when(addressClientService.update(eq(addressId), any(AddressDto.class))).thenReturn(ResponseEntity.ok(savedAddress));

        // When & Then
        mockMvc.perform(put("/api/address/{addressId}", addressId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updatedAddress)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.addressId").value(1))
                .andExpect(jsonPath("$.fullAddress").value("Admin Updated Address"));
    }

    @Test
    @WithMockUser(username = "admin", roles = "ADMIN")
    void testDeleteById_AsAdmin_Success() throws Exception {
        // Given
        String addressId = "1";
        UserDetails userDetails = new User("admin", "password",
                Arrays.asList(new SimpleGrantedAuthority("ROLE_ADMIN")));

        when(authUtil.getOwner(addressId, ResourceType.ADDRESSES)).thenReturn("1");
        doNothing().when(authUtil).canActivate(any(HttpServletRequest.class), eq("1"), any(UserDetails.class));
        when(addressClientService.deleteById(addressId)).thenReturn(ResponseEntity.ok(true));

        // When & Then
        mockMvc.perform(delete("/api/address/{addressId}", addressId)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").value(true));
    }
}