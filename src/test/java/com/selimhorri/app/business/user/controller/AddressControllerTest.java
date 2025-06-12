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
import com.selimhorri.app.business.user.model.AddressDto;
import com.selimhorri.app.business.user.model.UserDto;
import com.selimhorri.app.business.user.model.response.AddressUserServiceCollectionDtoResponse;
import com.selimhorri.app.business.user.service.AddressClientService;

@ExtendWith(MockitoExtension.class)
class AddressControllerTest {

    @Mock
    private AddressClientService addressClientService;

    @InjectMocks
    private AddressController addressController;

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;
    private AddressDto addressDto;
    private AddressUserServiceCollectionDtoResponse collectionResponse;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(addressController).build();
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

        // Create AddressDto
        addressDto = AddressDto.builder()
                .addressId(1)
                .fullAddress("123 Main St")
                .postalCode("12345")
                .city("Test City")
                .userDto(userDto)
                .build();

        // Create collection response
        collectionResponse = AddressUserServiceCollectionDtoResponse.builder()
                .collection(Arrays.asList(addressDto))
                .build();
    }

    @Test
    void testFindAll_ShouldReturnAllAddresses() throws Exception {
        // Given
        when(addressClientService.findAll()).thenReturn(ResponseEntity.ok(collectionResponse));

        // When & Then
        mockMvc.perform(get("/api/address")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.collection").isArray())
                .andExpect(jsonPath("$.collection[0].addressId").value(1))
                .andExpect(jsonPath("$.collection[0].fullAddress").value("123 Main St"))
                .andExpect(jsonPath("$.collection[0].postalCode").value("12345"))
                .andExpect(jsonPath("$.collection[0].city").value("Test City"));
    }

    @Test
    void testFindById_ShouldReturnAddressById() throws Exception {
        // Given
        String addressId = "1";
        when(addressClientService.findById(addressId)).thenReturn(ResponseEntity.ok(addressDto));

        // When & Then
        mockMvc.perform(get("/api/address/{addressId}", addressId)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.addressId").value(1))
                .andExpect(jsonPath("$.fullAddress").value("123 Main St"))
                .andExpect(jsonPath("$.postalCode").value("12345"))
                .andExpect(jsonPath("$.city").value("Test City"))
                .andExpect(jsonPath("$.user.userId").value(1))
                .andExpect(jsonPath("$.user.firstName").value("John"));
    }

    @Test
    void testSave_ShouldCreateNewAddress() throws Exception {
        // Given
        AddressDto newAddress = AddressDto.builder()
                .fullAddress("456 Oak Ave")
                .postalCode("67890")
                .city("Another City")
                .build();

        AddressDto savedAddress = AddressDto.builder()
                .addressId(2)
                .fullAddress("456 Oak Ave")
                .postalCode("67890")
                .city("Another City")
                .build();

        when(addressClientService.save(any(AddressDto.class))).thenReturn(ResponseEntity.ok(savedAddress));

        // When & Then
        mockMvc.perform(post("/api/address")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(newAddress)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.addressId").value(2))
                .andExpect(jsonPath("$.fullAddress").value("456 Oak Ave"))
                .andExpect(jsonPath("$.postalCode").value("67890"))
                .andExpect(jsonPath("$.city").value("Another City"));
    }

    @Test
    void testUpdate_WithoutPathVariable_ShouldUpdateAddress() throws Exception {
        // Given
        AddressDto updatedAddress = AddressDto.builder()
                .addressId(1)
                .fullAddress("123 Main St Updated")
                .postalCode("54321")
                .city("Updated City")
                .build();

        when(addressClientService.update(any(AddressDto.class))).thenReturn(ResponseEntity.ok(updatedAddress));

        // When & Then
        mockMvc.perform(put("/api/address")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updatedAddress)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.addressId").value(1))
                .andExpect(jsonPath("$.fullAddress").value("123 Main St Updated"))
                .andExpect(jsonPath("$.postalCode").value("54321"))
                .andExpect(jsonPath("$.city").value("Updated City"));
    }

    @Test
    void testUpdate_WithPathVariable_ShouldUpdateAddress() throws Exception {
        // Given
        String addressId = "1";
        AddressDto updatedAddress = AddressDto.builder()
                .addressId(1)
                .fullAddress("123 Main St Path Updated")
                .postalCode("99999")
                .city("Path Updated City")
                .build();

        when(addressClientService.update(any(AddressDto.class))).thenReturn(ResponseEntity.ok(updatedAddress));

        // When & Then
        mockMvc.perform(put("/api/address/{addressId}", addressId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updatedAddress)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.addressId").value(1))
                .andExpect(jsonPath("$.fullAddress").value("123 Main St Path Updated"))
                .andExpect(jsonPath("$.postalCode").value("99999"))
                .andExpect(jsonPath("$.city").value("Path Updated City"));
    }

    @Test
    void testDeleteById_ShouldDeleteAddress() throws Exception {
        // Given
        String addressId = "1";
        when(addressClientService.deleteById(addressId)).thenReturn(ResponseEntity.ok(true));

        // When & Then
        mockMvc.perform(delete("/api/address/{addressId}", addressId)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().string("true"));
    }

    @Test
    void testDeleteById_WhenDeleteFails_ShouldReturnFalse() throws Exception {
        // Given
        String addressId = "999";
        when(addressClientService.deleteById(addressId)).thenReturn(ResponseEntity.ok(false));

        // When & Then
        mockMvc.perform(delete("/api/address/{addressId}", addressId)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().string("false"));
    }

    @Test
    void testFindById_WithNullResponse_ShouldHandleGracefully() throws Exception {
        // Given
        String addressId = "999";
        when(addressClientService.findById(addressId)).thenReturn(ResponseEntity.ok(null));

        // When & Then
        mockMvc.perform(get("/api/address/{addressId}", addressId)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().string(""));
    }

    @Test
    void testSave_WithCompleteAddressData_ShouldCreateAddressWithAllFields() throws Exception {
        // Given
        when(addressClientService.save(any(AddressDto.class))).thenReturn(ResponseEntity.ok(addressDto));

        // When & Then
        mockMvc.perform(post("/api/address")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(addressDto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.addressId").value(1))
                .andExpect(jsonPath("$.fullAddress").value("123 Main St"))
                .andExpect(jsonPath("$.postalCode").value("12345"))
                .andExpect(jsonPath("$.city").value("Test City"))
                .andExpect(jsonPath("$.user.userId").value(1))
                .andExpect(jsonPath("$.user.firstName").value("John"));
    }
}