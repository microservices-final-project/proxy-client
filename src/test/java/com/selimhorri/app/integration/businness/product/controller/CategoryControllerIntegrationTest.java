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
import com.selimhorri.app.business.product.controller.CategoryController;
import com.selimhorri.app.business.product.model.CategoryDto;
import com.selimhorri.app.business.product.model.response.CategoryProductServiceCollectionDtoResponse;
import com.selimhorri.app.business.product.service.CategoryClientService;
import com.selimhorri.app.config.template.TemplateConfig;
import com.selimhorri.app.jwt.service.JwtService;
import com.selimhorri.app.jwt.util.JwtUtil;
import com.selimhorri.app.security.SecurityConfig;

@Tag("integration")
@WebMvcTest(CategoryController.class)
@Import({ TemplateConfig.class, SecurityConfig.class })
public class CategoryControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private UserDetailsService userDetailsService;

    @MockBean
    private CategoryClientService categoryClientService;

    @MockBean
    private PasswordEncoder passwordEncoder;

    @MockBean
    private JwtUtil jwtUtil;

    @MockBean
    private JwtService jwtService;

    @Autowired
    private ObjectMapper objectMapper;

    private CategoryDto testCategory;
    private CategoryProductServiceCollectionDtoResponse collectionResponse;

    @BeforeEach
    void setUp() {
        testCategory = new CategoryDto();
        testCategory.setCategoryId(1);
        testCategory.setCategoryTitle("Electronics");
        testCategory.setImageUrl("http://example.com/electronics.jpg");

        collectionResponse = new CategoryProductServiceCollectionDtoResponse();
        collectionResponse.setCollection(Collections.singletonList(testCategory));
    }

    @Test
    void testFindAll_Unauthenticated_Success() throws Exception {
        when(categoryClientService.findAll()).thenReturn(ResponseEntity.ok(collectionResponse));

        mockMvc.perform(get("/api/categories")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.collection[0].categoryId").value(1))
                .andExpect(jsonPath("$.collection[0].categoryTitle").value("Electronics"));
    }

    @Test
    void testFindById_Unauthenticated_Success() throws Exception {
        String categoryId = "1";
        when(categoryClientService.findById(categoryId)).thenReturn(ResponseEntity.ok(testCategory));

        mockMvc.perform(get("/api/categories/{categoryId}", categoryId)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.categoryId").value(1));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void testSave_AsAdmin_Success() throws Exception {
        CategoryDto newCategory = new CategoryDto();
        newCategory.setCategoryTitle("Furniture");

        CategoryDto savedCategory = new CategoryDto();
        savedCategory.setCategoryId(2);
        savedCategory.setCategoryTitle("Furniture");

        when(categoryClientService.save(any(CategoryDto.class))).thenReturn(ResponseEntity.ok(savedCategory));

        mockMvc.perform(post("/api/categories")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(newCategory)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.categoryId").value(2));
    }

    @Test
    @WithMockUser(roles = "USER")
    void testSave_AsUser_Forbidden() throws Exception {
        CategoryDto newCategory = new CategoryDto();
        newCategory.setCategoryTitle("Clothing");

        mockMvc.perform(post("/api/categories")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(newCategory)))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void testUpdate_AsAdmin_Success() throws Exception {
        String categoryId = "1";
        CategoryDto updatedCategory = new CategoryDto();
        updatedCategory.setCategoryTitle("Updated Electronics");

        CategoryDto savedCategory = new CategoryDto();
        savedCategory.setCategoryId(1);
        savedCategory.setCategoryTitle("Updated Electronics");

        when(categoryClientService.update(eq(categoryId), any(CategoryDto.class)))
                .thenReturn(ResponseEntity.ok(savedCategory));

        mockMvc.perform(put("/api/categories/{categoryId}", categoryId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updatedCategory)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.categoryTitle").value("Updated Electronics"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void testDeleteById_AsAdmin_Success() throws Exception {
        String categoryId = "1";
        when(categoryClientService.deleteById(categoryId)).thenReturn(ResponseEntity.ok(true));

        mockMvc.perform(delete("/api/categories/{categoryId}", categoryId)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").value(true));
    }
}