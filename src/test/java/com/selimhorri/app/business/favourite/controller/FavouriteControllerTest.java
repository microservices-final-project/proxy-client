package com.selimhorri.app.business.favourite.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collection;

import javax.servlet.http.HttpServletRequest;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.bind.MethodArgumentNotValidException;

import com.selimhorri.app.business.auth.util.AuthUtil;
import com.selimhorri.app.business.favourite.model.FavouriteDto;
import com.selimhorri.app.business.favourite.model.ProductDto;
import com.selimhorri.app.business.favourite.model.UserDto;
import com.selimhorri.app.business.favourite.model.response.FavouriteFavouriteServiceCollectionDtoResponse;
import com.selimhorri.app.business.favourite.service.FavouriteClientService;
import com.selimhorri.app.exception.wrapper.UnauthorizedException;

@ExtendWith(MockitoExtension.class)
@DisplayName("FavouriteController Unit Tests")
class FavouriteControllerTest {

    @Mock
    private AuthUtil authUtil;

    @Mock
    private FavouriteClientService favouriteClientService;

    @Mock
    private HttpServletRequest request;

    @Mock
    private UserDetails userDetails;

    private FavouriteController favouriteController;
    private FavouriteDto favouriteDto;
    private FavouriteFavouriteServiceCollectionDtoResponse collectionResponse;

    @BeforeEach
    void setUp() {
        // Create controller and inject mocks
        favouriteController = new FavouriteController(favouriteClientService);
        ReflectionTestUtils.setField(favouriteController, "authUtil", authUtil);
        
        // Setup UserDto
        UserDto userDto = UserDto.builder()
                .userId(1)
                .firstName("John")
                .lastName("Doe")
                .email("john.doe@example.com")
                .phone("123456789")
                .build();

        // Setup ProductDto
        ProductDto productDto = ProductDto.builder()
                .productId(1)
                .productTitle("Test Product")
                .sku("TEST123")
                .priceUnit(99.99)
                .quantity(10)
                .build();

        // Setup FavouriteDto
        favouriteDto = FavouriteDto.builder()
                .userId(1)
                .productId(1)
                .likeDate(LocalDateTime.now())
                .userDto(userDto)
                .productDto(productDto)
                .build();

        // Setup Collection Response
        Collection<FavouriteDto> favourites = Arrays.asList(favouriteDto);
        collectionResponse = FavouriteFavouriteServiceCollectionDtoResponse.builder()
                .collection(favourites)
                .build();
    }

    @Test
    @DisplayName("Should find all favourites successfully")
    void findAll_ShouldReturnAllFavourites_WhenCalled() {
        // Given
        ResponseEntity<FavouriteFavouriteServiceCollectionDtoResponse> serviceResponse = 
            new ResponseEntity<>(collectionResponse, HttpStatus.OK);
        when(favouriteClientService.findAll()).thenReturn(serviceResponse);

        // When
        ResponseEntity<FavouriteFavouriteServiceCollectionDtoResponse> result = favouriteController.findAll();

        // Then
        assertNotNull(result);
        assertEquals(HttpStatus.OK, result.getStatusCode());
        assertEquals(collectionResponse, result.getBody());
        verify(favouriteClientService, times(1)).findAll();
    }

    @Test
    @DisplayName("Should find favourite by composite ID successfully when user is authorized")
    void findById_ShouldReturnFavourite_WhenUserIsAuthorized() {
        // Given
        String userId = "1";
        String productId = "1";
        ResponseEntity<FavouriteDto> serviceResponse = new ResponseEntity<>(favouriteDto, HttpStatus.OK);
        
        doNothing().when(authUtil).canActivate(request, userId, userDetails);
        when(favouriteClientService.findById(userId, productId)).thenReturn(serviceResponse);

        // When
        ResponseEntity<FavouriteDto> result = favouriteController.findById(userId, productId, request, userDetails);

        // Then
        assertNotNull(result);
        assertEquals(HttpStatus.OK, result.getStatusCode());
        assertEquals(favouriteDto, result.getBody());
        verify(authUtil, times(1)).canActivate(request, userId, userDetails);
        verify(favouriteClientService, times(1)).findById(userId, productId);
    }

    @Test
    @DisplayName("Should throw UnauthorizedException when user is not authorized to find favourite")
    void findById_ShouldThrowUnauthorizedException_WhenUserIsNotAuthorized() {
        // Given
        String userId = "1";
        String productId = "1";
        
        doThrow(new UnauthorizedException("You can access to resources of your own"))
            .when(authUtil).canActivate(request, userId, userDetails);

        // When & Then
        UnauthorizedException exception = assertThrows(UnauthorizedException.class, () -> {
            favouriteController.findById(userId, productId, request, userDetails);
        });
        
        assertEquals("You can access to resources of your own", exception.getMessage());
        verify(authUtil, times(1)).canActivate(request, userId, userDetails);
        verify(favouriteClientService, times(0)).findById(anyString(), anyString());
    }

    @Test
    @DisplayName("Should save favourite successfully when user is authorized")
    void save_ShouldReturnSavedFavourite_WhenUserIsAuthorized() {
        // Given
        String userId = "1";
        ResponseEntity<FavouriteDto> serviceResponse = new ResponseEntity<>(favouriteDto, HttpStatus.OK);
        
        doNothing().when(authUtil).canActivate(request, userId, userDetails);
        when(favouriteClientService.save(favouriteDto)).thenReturn(serviceResponse);

        // When
        ResponseEntity<FavouriteDto> result = favouriteController.save(favouriteDto, request, userDetails);

        // Then
        assertNotNull(result);
        assertEquals(HttpStatus.OK, result.getStatusCode());
        assertEquals(favouriteDto, result.getBody());
        verify(authUtil, times(1)).canActivate(request, userId, userDetails);
        verify(favouriteClientService, times(1)).save(favouriteDto);
    }

    @Test
    @DisplayName("Should throw UnauthorizedException when user is not authorized to save favourite")
    void save_ShouldThrowUnauthorizedException_WhenUserIsNotAuthorized() {
        // Given
        String userId = "1";
        
        doThrow(new UnauthorizedException("You can access to resources of your own"))
            .when(authUtil).canActivate(request, userId, userDetails);

        // When & Then
        UnauthorizedException exception = assertThrows(UnauthorizedException.class, () -> {
            favouriteController.save(favouriteDto, request, userDetails);
        });
        
        assertEquals("You can access to resources of your own", exception.getMessage());
        verify(authUtil, times(1)).canActivate(request, userId, userDetails);
        verify(favouriteClientService, times(0)).save(any(FavouriteDto.class));
    }

    @Test
    @DisplayName("Should delete favourite successfully when user is authorized")
    void deleteById_ShouldReturnTrue_WhenUserIsAuthorizedAndDeletionSuccessful() {
        // Given
        String userId = "1";
        String productId = "1";
        ResponseEntity<Boolean> serviceResponse = new ResponseEntity<>(true, HttpStatus.OK);
        
        doNothing().when(authUtil).canActivate(request, userId, userDetails);
        when(favouriteClientService.deleteById(userId, productId)).thenReturn(serviceResponse);

        // When
        ResponseEntity<Boolean> result = favouriteController.deleteById(userId, productId, request, userDetails);

        // Then
        assertNotNull(result);
        assertEquals(HttpStatus.OK, result.getStatusCode());
        assertEquals(true, result.getBody());
        verify(authUtil, times(1)).canActivate(request, userId, userDetails);
        verify(favouriteClientService, times(1)).deleteById(userId, productId);
    }

    @Test
    @DisplayName("Should throw UnauthorizedException when user is not authorized to delete favourite")
    void deleteById_ShouldThrowUnauthorizedException_WhenUserIsNotAuthorized() {
        // Given
        String userId = "1";
        String productId = "1";
        
        doThrow(new UnauthorizedException("You can access to resources of your own"))
            .when(authUtil).canActivate(request, userId, userDetails);

        // When & Then
        UnauthorizedException exception = assertThrows(UnauthorizedException.class, () -> {
            favouriteController.deleteById(userId, productId, request, userDetails);
        });
        
        assertEquals("You can access to resources of your own", exception.getMessage());
        verify(authUtil, times(1)).canActivate(request, userId, userDetails);
        verify(favouriteClientService, times(0)).deleteById(anyString(), anyString());
    }

    @Test
    @DisplayName("Should handle null response body from service")
    void findAll_ShouldHandleNullResponseBody_WhenServiceReturnsNullBody() {
        // Given
        ResponseEntity<FavouriteFavouriteServiceCollectionDtoResponse> serviceResponse = 
            new ResponseEntity<>(null, HttpStatus.OK);
        when(favouriteClientService.findAll()).thenReturn(serviceResponse);

        // When
        ResponseEntity<FavouriteFavouriteServiceCollectionDtoResponse> result = favouriteController.findAll();

        // Then
        assertNotNull(result);
        assertEquals(HttpStatus.OK, result.getStatusCode());
        assertEquals(null, result.getBody());
        verify(favouriteClientService, times(1)).findAll();
    }

    @Test
    @DisplayName("Should handle empty favourite collection")
    void findAll_ShouldHandleEmptyCollection_WhenNoFavouritesExist() {
        // Given
        Collection<FavouriteDto> emptyFavourites = Arrays.asList();
        FavouriteFavouriteServiceCollectionDtoResponse emptyResponse = FavouriteFavouriteServiceCollectionDtoResponse.builder()
                .collection(emptyFavourites)
                .build();
        ResponseEntity<FavouriteFavouriteServiceCollectionDtoResponse> serviceResponse = 
            new ResponseEntity<>(emptyResponse, HttpStatus.OK);
        when(favouriteClientService.findAll()).thenReturn(serviceResponse);

        // When
        ResponseEntity<FavouriteFavouriteServiceCollectionDtoResponse> result = favouriteController.findAll();

        // Then
        assertNotNull(result);
        assertEquals(HttpStatus.OK, result.getStatusCode());
        assertNotNull(result.getBody());
        assertEquals(0, result.getBody().getCollection().size());
        verify(favouriteClientService, times(1)).findAll();
    }

    @Test
    @DisplayName("Should handle favourite with null user ID")
    void save_ShouldThrowException_WhenUserIdIsNull() {
        // Given
        FavouriteDto favouriteWithNullUserId = FavouriteDto.builder()
                .userId(null)
                .productId(1)
                .likeDate(LocalDateTime.now())
                .build();

        // When & Then
        assertThrows(NullPointerException.class, () -> {
            favouriteController.save(favouriteWithNullUserId, request, userDetails);
        });

        verify(favouriteClientService, times(0)).save(any(FavouriteDto.class));
    }
}