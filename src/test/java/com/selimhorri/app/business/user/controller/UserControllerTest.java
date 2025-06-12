package com.selimhorri.app.business.user.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

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
import com.selimhorri.app.business.user.model.CredentialDto;
import com.selimhorri.app.business.user.model.RoleBasedAuthority;
import com.selimhorri.app.business.user.model.UserDto;
import com.selimhorri.app.business.user.model.response.UserUserServiceCollectionDtoResponse;
import com.selimhorri.app.business.user.service.UserClientService;

@ExtendWith(MockitoExtension.class)
class UserControllerTest {

    @Mock
    private UserClientService userClientService;

    @InjectMocks
    private UserController userController;

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;
    private UserDto userDto;
    private UserUserServiceCollectionDtoResponse collectionResponse;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(userController).build();
        objectMapper = new ObjectMapper();
        
        // Crear datos de prueba
        setupTestData();
    }

    private void setupTestData() {
        // Crear AddressDto
        AddressDto addressDto = AddressDto.builder()
                .addressId(1)
                .fullAddress("123 Main St")
                .postalCode("12345")
                .city("Test City")
                .build();

        Set<AddressDto> addressDtos = new HashSet<>();
        addressDtos.add(addressDto);

        // Crear CredentialDto
        CredentialDto credentialDto = CredentialDto.builder()
                .credentialId(1)
                .username("testuser")
                .password("password123")
                .roleBasedAuthority(RoleBasedAuthority.ROLE_USER)
                .isEnabled(true)
                .isAccountNonExpired(true)
                .isAccountNonLocked(true)
                .isCredentialsNonExpired(true)
                .build();

        // Crear UserDto
        userDto = UserDto.builder()
                .userId(1)
                .firstName("John")
                .lastName("Doe")
                .imageUrl("http://example.com/image.jpg")
                .email("john.doe@example.com")
                .phone("1234567890")
                .addressDtos(addressDtos)
                .credentialDto(credentialDto)
                .build();

        // Crear respuesta de colección
        collectionResponse = UserUserServiceCollectionDtoResponse.builder()
                .collection(Arrays.asList(userDto))
                .build();
    }

    @Test
    void testFindAll_ShouldReturnAllUsers() throws Exception {
        // Given
        when(userClientService.findAll()).thenReturn(ResponseEntity.ok(collectionResponse));

        // When & Then
        mockMvc.perform(get("/api/users")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.collection").isArray())
                .andExpect(jsonPath("$.collection[0].userId").value(1))
                .andExpect(jsonPath("$.collection[0].firstName").value("John"))
                .andExpect(jsonPath("$.collection[0].lastName").value("Doe"))
                .andExpect(jsonPath("$.collection[0].email").value("john.doe@example.com"));
    }

    @Test
    void testFindById_ShouldReturnUserById() throws Exception {
        // Given
        String userId = "1";
        when(userClientService.findById(userId)).thenReturn(ResponseEntity.ok(userDto));

        // When & Then
        mockMvc.perform(get("/api/users/{userId}", userId)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value(1))
                .andExpect(jsonPath("$.firstName").value("John"))
                .andExpect(jsonPath("$.lastName").value("Doe"))
                .andExpect(jsonPath("$.email").value("john.doe@example.com"))
                .andExpect(jsonPath("$.phone").value("1234567890"));
    }

    @Test
    void testFindByUsername_ShouldReturnUserByUsername() throws Exception {
        // Given
        String username = "testuser";
        when(userClientService.findByUsername(username)).thenReturn(ResponseEntity.ok(userDto));

        // When & Then
        mockMvc.perform(get("/api/users/username/{username}", username)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value(1))
                .andExpect(jsonPath("$.firstName").value("John"))
                .andExpect(jsonPath("$.lastName").value("Doe"))
                .andExpect(jsonPath("$.credential.username").value("testuser"));
    }

    @Test
    void testSave_ShouldCreateNewUser() throws Exception {
        // Given
        UserDto newUser = UserDto.builder()
                .firstName("Jane")
                .lastName("Smith")
                .email("jane.smith@example.com")
                .phone("0987654321")
                .build();

        UserDto savedUser = UserDto.builder()
                .userId(2)
                .firstName("Jane")
                .lastName("Smith")
                .email("jane.smith@example.com")
                .phone("0987654321")
                .build();

        when(userClientService.save(any(UserDto.class))).thenReturn(ResponseEntity.ok(savedUser));

        // When & Then
        mockMvc.perform(post("/api/users")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(newUser)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value(2))
                .andExpect(jsonPath("$.firstName").value("Jane"))
                .andExpect(jsonPath("$.lastName").value("Smith"))
                .andExpect(jsonPath("$.email").value("jane.smith@example.com"));
    }

    @Test
    void testUpdate_WithoutPathVariable_ShouldUpdateUser() throws Exception {
        // Given
        UserDto updatedUser = UserDto.builder()
                .userId(1)
                .firstName("John Updated")
                .lastName("Doe Updated")
                .email("john.updated@example.com")
                .phone("1111111111")
                .build();

        when(userClientService.update(any(UserDto.class))).thenReturn(ResponseEntity.ok(updatedUser));

        // When & Then
        mockMvc.perform(put("/api/users")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updatedUser)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value(1))
                .andExpect(jsonPath("$.firstName").value("John Updated"))
                .andExpect(jsonPath("$.lastName").value("Doe Updated"))
                .andExpect(jsonPath("$.email").value("john.updated@example.com"));
    }

    @Test
    void testUpdate_WithPathVariable_ShouldUpdateUser() throws Exception {
        // Given
        String userId = "1";
        UserDto updatedUser = UserDto.builder()
                .userId(1)
                .firstName("John Path Updated")
                .lastName("Doe Path Updated")
                .email("john.path@example.com")
                .build();

        when(userClientService.update(any(UserDto.class))).thenReturn(ResponseEntity.ok(updatedUser));

        // When & Then
        mockMvc.perform(put("/api/users/{userId}", userId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updatedUser)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value(1))
                .andExpect(jsonPath("$.firstName").value("John Path Updated"))
                .andExpect(jsonPath("$.lastName").value("Doe Path Updated"));
    }

    @Test
    void testDeleteById_ShouldDeleteUser() throws Exception {
        // Given
        String userId = "1";
        when(userClientService.deleteById(userId)).thenReturn(ResponseEntity.ok(true));

        // When & Then
        mockMvc.perform(delete("/api/users/{userId}", userId)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().string("true"));
    }

    @Test
    void testDeleteById_WhenDeleteFails_ShouldReturnFalse() throws Exception {
        // Given
        String userId = "999";
        when(userClientService.deleteById(userId)).thenReturn(ResponseEntity.ok(false));

        // When & Then
        mockMvc.perform(delete("/api/users/{userId}", userId)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().string("false"));
    }

    @Test
    void testFindById_WithNullResponse_ShouldHandleGracefully() throws Exception {
        // Given
        String userId = "999";
        when(userClientService.findById(userId)).thenReturn(ResponseEntity.ok(null));

        // When & Then
        mockMvc.perform(get("/api/users/{userId}", userId)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().string(""));
    }

    @Test
    void testFindByUsername_WithNullResponse_ShouldHandleGracefully() throws Exception {
        // Given
        String username = "nonexistent";
        when(userClientService.findByUsername(username)).thenReturn(ResponseEntity.ok(null));

        // When & Then
        mockMvc.perform(get("/api/users/username/{username}", username)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().string(""));
    }

    @Test
    void testSave_WithCompleteUserData_ShouldCreateUserWithAllFields() throws Exception {
        // Given
        when(userClientService.save(any(UserDto.class))).thenReturn(ResponseEntity.ok(userDto));

        // When & Then
        mockMvc.perform(post("/api/users")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(userDto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value(1))
                .andExpect(jsonPath("$.firstName").value("John"))
                .andExpect(jsonPath("$.lastName").value("Doe"))
                .andExpect(jsonPath("$.email").value("john.doe@example.com"))
                .andExpect(jsonPath("$.phone").value("1234567890"))
                .andExpect(jsonPath("$.imageUrl").value("http://example.com/image.jpg"))
                .andExpect(jsonPath("$.addressDtos").isArray())
                .andExpect(jsonPath("$.credential.username").value("testuser"));
    }
}