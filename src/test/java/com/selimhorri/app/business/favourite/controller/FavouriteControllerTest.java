package com.selimhorri.app.business.favourite.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collection;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.selimhorri.app.business.favourite.model.FavouriteDto;
import com.selimhorri.app.business.favourite.model.ProductDto;
import com.selimhorri.app.business.favourite.model.UserDto;
import com.selimhorri.app.business.favourite.model.response.FavouriteFavouriteServiceCollectionDtoResponse;
import com.selimhorri.app.business.favourite.service.FavouriteClientService;

@ExtendWith(MockitoExtension.class)
class FavouriteControllerTest {

    @Mock
    private FavouriteClientService favouriteClientService;

    @InjectMocks
    private FavouriteController favouriteController;

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;
    private FavouriteDto favouriteDto;
    private UserDto userDto;
    private ProductDto productDto;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(favouriteController).build();
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        
        // Configurar datos de prueba
        userDto = UserDto.builder()
                .userId(1)
                .firstName("John")
                .lastName("Doe")
                .email("john.doe@example.com")
                .phone("123456789")
                .imageUrl("http://example.com/user1.jpg")
                .build();

        productDto = ProductDto.builder()
                .productId(1)
                .productTitle("Test Product")
                .sku("TEST001")
                .priceUnit(99.99)
                .quantity(10)
                .imageUrl("http://example.com/product1.jpg")
                .build();

        favouriteDto = FavouriteDto.builder()
                .userId(1)
                .productId(1)
                .likeDate(LocalDateTime.now())
                .userDto(userDto)
                .productDto(productDto)
                .build();
    }

    @Test
    void testFindAll_ShouldReturnAllFavourites() throws Exception {
        // Given
        Collection<FavouriteDto> favourites = Arrays.asList(favouriteDto);
        FavouriteFavouriteServiceCollectionDtoResponse response = 
            FavouriteFavouriteServiceCollectionDtoResponse.builder()
                .collection(favourites)
                .build();
        
        when(favouriteClientService.findAll())
            .thenReturn(new ResponseEntity<>(response, HttpStatus.OK));

        // When & Then
        mockMvc.perform(get("/api/favourites")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.collection").isArray())
                .andExpect(jsonPath("$.collection[0].userId").value(1))
                .andExpect(jsonPath("$.collection[0].productId").value(1));
    }

    @Test
    void testSave_ShouldCreateFavourite_WhenValidData() throws Exception {
        // Given
        when(favouriteClientService.save(any(FavouriteDto.class)))
            .thenReturn(new ResponseEntity<>(favouriteDto, HttpStatus.OK));

        // When & Then
        mockMvc.perform(post("/api/favourites")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(favouriteDto)))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.userId").value(1))
                .andExpect(jsonPath("$.productId").value(1));
    }

    @Test
    void testUpdate_ShouldUpdateFavourite_WhenValidData() throws Exception {
        // Given
        FavouriteDto updatedFavourite = FavouriteDto.builder()
                .userId(1)
                .productId(1)
                .likeDate(LocalDateTime.now().plusDays(1))
                .userDto(userDto)
                .productDto(productDto)
                .build();
        
        when(favouriteClientService.update(any(FavouriteDto.class)))
            .thenReturn(new ResponseEntity<>(updatedFavourite, HttpStatus.OK));

        // When & Then
        mockMvc.perform(put("/api/favourites")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updatedFavourite)))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.userId").value(1))
                .andExpect(jsonPath("$.productId").value(1));
    }

    @Test
    void testDeleteById_ShouldReturnTrue_WhenValidIds() throws Exception {
        // Given
        String userId = "1";
        String productId = "1";
        
        when(favouriteClientService.deleteById(userId, productId))
            .thenReturn(new ResponseEntity<>(true, HttpStatus.OK));

        // When & Then
        mockMvc.perform(delete("/api/favourites/{userId}/{productId}", userId, productId)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$").value(true));
    }

    @Test
    void testSave_ShouldHandleNullUserDto() throws Exception {
        // Given
        FavouriteDto favouriteWithoutUser = FavouriteDto.builder()
                .userId(1)
                .productId(1)
                .likeDate(LocalDateTime.now())
                .productDto(productDto)
                .build();
        
        when(favouriteClientService.save(any(FavouriteDto.class)))
            .thenReturn(new ResponseEntity<>(favouriteWithoutUser, HttpStatus.OK));

        // When & Then
        mockMvc.perform(post("/api/favourites")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(favouriteWithoutUser)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value(1))
                .andExpect(jsonPath("$.productId").value(1))
                .andExpect(jsonPath("$.userDto").doesNotExist());
    }

    @Test
    void testSave_ShouldHandleNullProductDto() throws Exception {
        // Given
        FavouriteDto favouriteWithoutProduct = FavouriteDto.builder()
                .userId(1)
                .productId(1)
                .likeDate(LocalDateTime.now())
                .userDto(userDto)
                .build();
        
        when(favouriteClientService.save(any(FavouriteDto.class)))
            .thenReturn(new ResponseEntity<>(favouriteWithoutProduct, HttpStatus.OK));

        // When & Then
        mockMvc.perform(post("/api/favourites")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(favouriteWithoutProduct)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value(1))
                .andExpect(jsonPath("$.productId").value(1))
                .andExpect(jsonPath("$.productDto").doesNotExist());
    }

    @Test
    void testFindById_WithNumericPathVariables() throws Exception {
        // Given
        String userId = "123";
        String productId = "456";
        
        FavouriteDto favourite = FavouriteDto.builder()
                .userId(123)
                .productId(456)
                .likeDate(LocalDateTime.now())
                .build();
        
        when(favouriteClientService.findById(userId, productId))
            .thenReturn(new ResponseEntity<>(favourite, HttpStatus.OK));

        // When & Then
        mockMvc.perform(get("/api/favourites/{userId}/{productId}", userId, productId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value(123))
                .andExpect(jsonPath("$.productId").value(456));
    }

    @Test
    void testDeleteById_WithNumericPathVariables() throws Exception {
        // Given
        String userId = "789";
        String productId = "101";
        
        when(favouriteClientService.deleteById(userId, productId))
            .thenReturn(new ResponseEntity<>(true, HttpStatus.OK));

        // When & Then
        mockMvc.perform(delete("/api/favourites/{userId}/{productId}", userId, productId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").value(true));
    }
}