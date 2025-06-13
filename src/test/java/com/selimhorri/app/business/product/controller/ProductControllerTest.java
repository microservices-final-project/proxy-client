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

import com.selimhorri.app.business.product.model.ProductDto;
import com.selimhorri.app.business.product.model.response.ProductProductServiceCollectionDtoResponse;
import com.selimhorri.app.business.product.service.ProductClientService;

@ExtendWith(MockitoExtension.class)
class ProductControllerUnitTest {

    @Mock
    private ProductClientService productClientService;

    @InjectMocks
    private ProductController productController;

    private ProductDto productDto;
    private ProductProductServiceCollectionDtoResponse collectionResponse;

    @BeforeEach
    void setUp() {
        productDto = new ProductDto();
        productDto.setProductId(1);
        productDto.setProductTitle("Test Product");
        productDto.setImageUrl("http://test.com/image.jpg");
        productDto.setSku("TEST123");
        productDto.setPriceUnit(99.99);
        productDto.setQuantity(10);

        collectionResponse = new ProductProductServiceCollectionDtoResponse();
        collectionResponse.setCollection(Collections.singletonList(productDto));
    }

    @Test
    void findAll_ShouldReturnAllProducts() {
        when(productClientService.findAll())
            .thenReturn(ResponseEntity.ok(collectionResponse));

        ResponseEntity<ProductProductServiceCollectionDtoResponse> response = 
            productController.findAll();

        assertNotNull(response);
        assertEquals(1, response.getBody().getCollection().size());
    }

    @Test
    void findById_ShouldReturnProduct() {
        when(productClientService.findById("1"))
            .thenReturn(ResponseEntity.ok(productDto));

        ResponseEntity<ProductDto> response = productController.findById("1");

        assertNotNull(response);
        assertEquals(1, response.getBody().getProductId());
        assertEquals("Test Product", response.getBody().getProductTitle());
    }

    @Test
    void save_ShouldCreateNewProduct() {
        when(productClientService.save(any(ProductDto.class)))
            .thenReturn(ResponseEntity.ok(productDto));

        ResponseEntity<ProductDto> response = productController.save(productDto);

        assertNotNull(response);
        assertEquals(1, response.getBody().getProductId());
    }

    @Test
    void updateWithId_ShouldUpdateProduct() {
        when(productClientService.update(eq("1"), any(ProductDto.class)))
            .thenReturn(ResponseEntity.ok(productDto));

        ResponseEntity<ProductDto> response = 
            productController.update("1", productDto);

        assertNotNull(response);
        assertEquals(1, response.getBody().getProductId());
    }

    @Test
    void deleteById_ShouldReturnTrue() {
        when(productClientService.deleteById("1"))
            .thenReturn(ResponseEntity.ok(true));

        ResponseEntity<Boolean> response = productController.deleteById("1");

        assertNotNull(response);
        assertTrue(response.getBody());
    }

    @Test
    void findAll_WhenNoProducts_ShouldReturnEmptyList() {
        ProductProductServiceCollectionDtoResponse emptyResponse = 
            new ProductProductServiceCollectionDtoResponse();
        emptyResponse.setCollection(Collections.emptyList());

        when(productClientService.findAll())
            .thenReturn(ResponseEntity.ok(emptyResponse));

        ResponseEntity<ProductProductServiceCollectionDtoResponse> response = 
            productController.findAll();

        assertNotNull(response);
        assertTrue(response.getBody().getCollection().isEmpty());
    }

    @Test
    void findById_WhenNotFound_ShouldReturnEmptyBody() {
        when(productClientService.findById("999"))
            .thenReturn(ResponseEntity.ok().build());

        ResponseEntity<ProductDto> response = productController.findById("999");

        assertNotNull(response);
        assertNull(response.getBody());
    }
}