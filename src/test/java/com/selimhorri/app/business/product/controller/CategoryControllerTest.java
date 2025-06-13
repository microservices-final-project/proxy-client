package com.selimhorri.app.business.product.controller;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.Collections;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;

import com.selimhorri.app.business.product.model.CategoryDto;
import com.selimhorri.app.business.product.model.response.CategoryProductServiceCollectionDtoResponse;
import com.selimhorri.app.business.product.service.CategoryClientService;

@ExtendWith(MockitoExtension.class)
class CategoryControllerUnitTest {

    @Mock
    private CategoryClientService categoryClientService;

    @InjectMocks
    private CategoryController categoryController;

    private CategoryDto categoryDto;
    private CategoryProductServiceCollectionDtoResponse collectionResponse;

    @BeforeEach
    void setUp() {
        categoryDto = new CategoryDto();
        categoryDto.setCategoryId(1);
        categoryDto.setCategoryTitle("Electronics");
        categoryDto.setImageUrl("http://example.com/electronics.jpg");

        collectionResponse = new CategoryProductServiceCollectionDtoResponse();
        collectionResponse.setCollection(Collections.singletonList(categoryDto));
    }

    @Test
    void findAll_ShouldReturnAllCategories() {
        // Configurar mock
        when(categoryClientService.findAll())
            .thenReturn(ResponseEntity.ok(collectionResponse));

        // Ejecutar método
        ResponseEntity<CategoryProductServiceCollectionDtoResponse> response = 
            categoryController.findAll();

        // Verificar resultados
        assertNotNull(response);
        assertEquals(1, response.getBody().getCollection().size());

    }

    @Test
    void findById_ShouldReturnCategory() {
        when(categoryClientService.findById("1"))
            .thenReturn(ResponseEntity.ok(categoryDto));

        ResponseEntity<CategoryDto> response = categoryController.findById("1");

        assertNotNull(response);
        assertEquals(1, response.getBody().getCategoryId());
        assertEquals("Electronics", response.getBody().getCategoryTitle());
    }

    @Test
    void save_ShouldCreateNewCategory() {
        when(categoryClientService.save(any(CategoryDto.class)))
            .thenReturn(ResponseEntity.ok(categoryDto));

        ResponseEntity<CategoryDto> response = categoryController.save(categoryDto);

        assertNotNull(response);
        assertEquals(1, response.getBody().getCategoryId());
        verify(categoryClientService, times(1)).save(any(CategoryDto.class));
    }



    @Test
    void updateWithId_ShouldUpdateSpecificCategory() {
        when(categoryClientService.update(eq("1"), any(CategoryDto.class)))
            .thenReturn(ResponseEntity.ok(categoryDto));

        ResponseEntity<CategoryDto> response = 
            categoryController.update("1", categoryDto);

        assertNotNull(response);
        assertEquals(1, response.getBody().getCategoryId());
        verify(categoryClientService, times(1)).update(eq("1"), any(CategoryDto.class));
    }

    @Test
    void deleteById_ShouldReturnTrueWhenDeleted() {
        when(categoryClientService.deleteById("1"))
            .thenReturn(ResponseEntity.ok(true));

        ResponseEntity<Boolean> response = categoryController.deleteById("1");

        assertNotNull(response);
        assertTrue(response.getBody());
        verify(categoryClientService, times(1)).deleteById("1");
    }

    @Test
    void findAll_WhenNoCategories_ShouldReturnEmptyList() {
        CategoryProductServiceCollectionDtoResponse emptyResponse = 
            new CategoryProductServiceCollectionDtoResponse();
        emptyResponse.setCollection(Collections.emptyList());

        when(categoryClientService.findAll())
            .thenReturn(ResponseEntity.ok(emptyResponse));

        ResponseEntity<CategoryProductServiceCollectionDtoResponse> response = 
            categoryController.findAll();

        assertNotNull(response);
        assertTrue(response.getBody().getCollection().isEmpty());
    }

    @Test
    void findById_WhenNotFound_ShouldReturnEmptyBody() {
        when(categoryClientService.findById("999"))
            .thenReturn(ResponseEntity.ok().build());

        ResponseEntity<CategoryDto> response = categoryController.findById("999");

        assertNotNull(response);
        assertNull(response.getBody());
    }
}