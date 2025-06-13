package com.selimhorri.app.integration.businness.user.product;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.util.Collections;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.web.servlet.MockMvc;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.selimhorri.app.business.product.controller.ProductController;
import com.selimhorri.app.business.product.model.CategoryDto;
import com.selimhorri.app.business.product.model.ProductDto;
import com.selimhorri.app.business.product.model.response.ProductProductServiceCollectionDtoResponse;
import com.selimhorri.app.business.product.service.ProductClientService;

@WebMvcTest(ProductController.class)
public class ProductControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ProductClientService productClientService;

    @Autowired
    private ObjectMapper objectMapper;

    private ProductDto testProduct;
    private ProductProductServiceCollectionDtoResponse collectionResponse;

    @BeforeEach
    void setUp() {
        // Setup test data
        testProduct = createTestProduct();
        collectionResponse = new ProductProductServiceCollectionDtoResponse();
        collectionResponse.setCollection(Collections.singletonList(testProduct));
    }

    private ProductDto createTestProduct() {
        CategoryDto category = new CategoryDto();
        category.setCategoryId(1);
        category.setCategoryTitle("Electronics");
        category.setImageUrl("http://example.com/electronics.jpg");

        ProductDto product = new ProductDto();
        product.setProductId(1);
        product.setProductTitle("Smartphone");
        product.setImageUrl("http://example.com/smartphone.jpg");
        product.setSku("SMARTPHONE-001");
        product.setPriceUnit(599.99);
        product.setQuantity(100);
        product.setCategoryDto(category);

        return product;
    }

    @Test
    void testFindAll_Success() throws Exception {
        // Given
        when(productClientService.findAll()).thenReturn(ResponseEntity.ok(collectionResponse));

        // When & Then
        mockMvc.perform(get("/api/products")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.collection").isArray())
                .andExpect(jsonPath("$.collection[0].productId").value(1))
                .andExpect(jsonPath("$.collection[0].productTitle").value("Smartphone"))
                .andExpect(jsonPath("$.collection[0].categoryDto.categoryTitle").value("Electronics"));
    }

    @Test
    void testFindById_Success() throws Exception {
        // Given
        String productId = "1";
        when(productClientService.findById(productId)).thenReturn(ResponseEntity.ok(testProduct));

        // When & Then
        mockMvc.perform(get("/api/products/{productId}", productId)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.productId").value(1))
                .andExpect(jsonPath("$.sku").value("SMARTPHONE-001"))
                .andExpect(jsonPath("$.priceUnit").value(599.99));
    }

    @Test
    void testSave_Success() throws Exception {
        // Given
        ProductDto newProduct = new ProductDto();
        newProduct.setProductTitle("Laptop");
        newProduct.setSku("LAPTOP-001");
        newProduct.setPriceUnit(999.99);
        newProduct.setQuantity(50);

        CategoryDto category = new CategoryDto();
        category.setCategoryId(2);
        category.setCategoryTitle("Computers");
        newProduct.setCategoryDto(category);

        ProductDto savedProduct = new ProductDto();
        savedProduct.setProductId(2);
        savedProduct.setProductTitle("Laptop");
        savedProduct.setSku("LAPTOP-001");
        savedProduct.setPriceUnit(999.99);
        savedProduct.setQuantity(50);
        savedProduct.setCategoryDto(category);

        when(productClientService.save(any(ProductDto.class))).thenReturn(ResponseEntity.ok(savedProduct));

        // When & Then
        mockMvc.perform(post("/api/products")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(newProduct)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.productId").value(2))
                .andExpect(jsonPath("$.productTitle").value("Laptop"))
                .andExpect(jsonPath("$.categoryDto.categoryTitle").value("Computers"));
    }

    @Test
    void testUpdate_Success() throws Exception {
        // Given
        String productId = "1";
        ProductDto updatedProduct = new ProductDto();
        updatedProduct.setProductTitle("Smartphone Pro");
        updatedProduct.setPriceUnit(699.99);
        updatedProduct.setQuantity(75);

        CategoryDto category = new CategoryDto();
        category.setCategoryId(1);
        updatedProduct.setCategoryDto(category);

        ProductDto savedProduct = new ProductDto();
        savedProduct.setProductId(1);
        savedProduct.setProductTitle("Smartphone Pro");
        savedProduct.setSku("SMARTPHONE-001");
        savedProduct.setPriceUnit(699.99);
        savedProduct.setQuantity(75);
        savedProduct.setCategoryDto(category);

        when(productClientService.update(eq(productId), any(ProductDto.class))).thenReturn(ResponseEntity.ok(savedProduct));

        // When & Then
        mockMvc.perform(put("/api/products/{productId}", productId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updatedProduct)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.productId").value(1))
                .andExpect(jsonPath("$.productTitle").value("Smartphone Pro"))
                .andExpect(jsonPath("$.priceUnit").value(699.99));
    }

    @Test
    void testDeleteById_Success() throws Exception {
        // Given
        String productId = "1";
        when(productClientService.deleteById(productId)).thenReturn(ResponseEntity.ok(true));

        // When & Then
        mockMvc.perform(delete("/api/products/{productId}", productId)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").value(true));
    }

    @Test
    void testFindAll_Empty() throws Exception {
        // Given
        ProductProductServiceCollectionDtoResponse emptyResponse = new ProductProductServiceCollectionDtoResponse();
        emptyResponse.setCollection(Collections.emptyList());
        when(productClientService.findAll()).thenReturn(ResponseEntity.ok(emptyResponse));

        // When & Then
        mockMvc.perform(get("/api/products")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.collection").isEmpty());
    }

    @Test
    void testSave_WithMinimumFields_Success() throws Exception {
        // Given
        ProductDto newProduct = new ProductDto();
        newProduct.setProductTitle("Headphones");
        newProduct.setPriceUnit(99.99);

        ProductDto savedProduct = new ProductDto();
        savedProduct.setProductId(3);
        savedProduct.setProductTitle("Headphones");
        savedProduct.setPriceUnit(99.99);

        when(productClientService.save(any(ProductDto.class))).thenReturn(ResponseEntity.ok(savedProduct));

        // When & Then
        mockMvc.perform(post("/api/products")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(newProduct)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.productId").value(3))
                .andExpect(jsonPath("$.productTitle").value("Headphones"));
    }
}