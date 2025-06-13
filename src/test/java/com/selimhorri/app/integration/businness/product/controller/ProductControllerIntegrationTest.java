package com.selimhorri.app.integration.businness.product.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.util.Collections;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.selimhorri.app.business.product.controller.ProductController;
import com.selimhorri.app.business.product.model.CategoryDto;
import com.selimhorri.app.business.product.model.ProductDto;
import com.selimhorri.app.business.product.model.response.ProductProductServiceCollectionDtoResponse;
import com.selimhorri.app.business.product.service.ProductClientService;
import com.selimhorri.app.config.template.TemplateConfig;
import com.selimhorri.app.jwt.service.JwtService;
import com.selimhorri.app.jwt.util.JwtUtil;
import com.selimhorri.app.security.SecurityConfig;

@Tag("integration")
@WebMvcTest(ProductController.class)
@Import({ TemplateConfig.class, SecurityConfig.class })
public class ProductControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ProductClientService productClientService;

    @MockBean
    private JwtUtil jwtUtil;

    @MockBean
    private PasswordEncoder passwordEncoder;

    @MockBean
    private JwtService jwtService;

    @MockBean
    private UserDetailsService userDetailsService;

    @Autowired
    private ObjectMapper objectMapper;

    private ProductDto testProduct;
    private ProductProductServiceCollectionDtoResponse collectionResponse;

    @BeforeEach
    void setUp() {
        CategoryDto category = new CategoryDto();
        category.setCategoryId(1);
        category.setCategoryTitle("Electronics");

        testProduct = new ProductDto();
        testProduct.setProductId(1);
        testProduct.setProductTitle("Smartphone");
        testProduct.setImageUrl("http://example.com/smartphone.jpg");
        testProduct.setSku("SMARTPHONE-001");
        testProduct.setPriceUnit(599.99);
        testProduct.setQuantity(100);
        testProduct.setCategoryDto(category);

        collectionResponse = new ProductProductServiceCollectionDtoResponse();
        collectionResponse.setCollection(Collections.singletonList(testProduct));
    }

    @Test
    void testFindAll_Unauthenticated_Success() throws Exception {
        when(productClientService.findAll()).thenReturn(ResponseEntity.ok(collectionResponse));

        mockMvc.perform(get("/api/products")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.collection[0].productId").value(1))
                .andExpect(jsonPath("$.collection[0].productTitle").value("Smartphone"));
    }

    @Test
    void testFindById_Unauthenticated_Success() throws Exception {
        String productId = "1";
        when(productClientService.findById(productId)).thenReturn(ResponseEntity.ok(testProduct));

        mockMvc.perform(get("/api/products/{productId}", productId)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.productId").value(1));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void testSave_AsAdmin_Success() throws Exception {
        ProductDto newProduct = new ProductDto();
        newProduct.setProductTitle("Laptop");
        newProduct.setSku("LAPTOP-001");
        newProduct.setPriceUnit(999.99);

        ProductDto savedProduct = new ProductDto();
        savedProduct.setProductId(2);
        savedProduct.setProductTitle("Laptop");
        savedProduct.setSku("LAPTOP-001");
        savedProduct.setPriceUnit(999.99);

        when(productClientService.save(any(ProductDto.class))).thenReturn(ResponseEntity.ok(savedProduct));

        mockMvc.perform(post("/api/products")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(newProduct)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.productId").value(2));
    }

    @Test
    @WithMockUser(roles = "USER")
    void testSave_AsUser_Forbidden() throws Exception {
        ProductDto newProduct = new ProductDto();
        newProduct.setProductTitle("Tablet");

        mockMvc.perform(post("/api/products")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(newProduct)))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void testUpdate_AsAdmin_Success() throws Exception {
        String productId = "1";
        ProductDto updatedProduct = new ProductDto();
        updatedProduct.setProductTitle("Smartphone Pro");
        updatedProduct.setPriceUnit(699.99);

        ProductDto savedProduct = new ProductDto();
        savedProduct.setProductId(1);
        savedProduct.setProductTitle("Smartphone Pro");
        savedProduct.setPriceUnit(699.99);

        when(productClientService.update(eq(productId), any(ProductDto.class)))
                .thenReturn(ResponseEntity.ok(savedProduct));

        mockMvc.perform(put("/api/products/{productId}", productId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updatedProduct)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.productTitle").value("Smartphone Pro"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void testDeleteById_AsAdmin_Success() throws Exception {
        String productId = "1";
        when(productClientService.deleteById(productId)).thenReturn(ResponseEntity.ok(true));

        mockMvc.perform(delete("/api/products/{productId}", productId)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").value(true));
    }
}