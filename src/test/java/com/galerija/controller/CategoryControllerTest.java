package com.galerija.controller;

import com.galerija.entity.Category;
import com.galerija.service.CategoryService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.Collections;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class CategoryControllerTest {
    private static final String CATEGORY_NAME = "test-category";
    private static final String CATEGORY_DESCRIPTION = "Test Category Description";

    private MockMvc mockMvc;

    @Mock
    private CategoryService categoryService;

    @InjectMocks
    private CategoryController categoryController;

    private Category testCategory;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        mockMvc = MockMvcBuilders
            .standaloneSetup(categoryController)
            .build();
            
        testCategory = new Category();
        testCategory.setId(1L);
        testCategory.setName(CATEGORY_NAME);
        testCategory.setDescription(CATEGORY_DESCRIPTION);
    }

    @Test
    void getAllCategories_Success() throws Exception {
        when(categoryService.getAllCategories()).thenReturn(Collections.singletonList(testCategory));

        mockMvc.perform(get("/api/categories"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].id").value(1))
                .andExpect(jsonPath("$.data[0].name").value(CATEGORY_NAME))
                .andExpect(jsonPath("$.total").value(1));

        verify(categoryService).getAllCategories();
    }

    @Test
    void getCategoryByName_Success() throws Exception {
        when(categoryService.getCategoryByName(CATEGORY_NAME)).thenReturn(testCategory);

        mockMvc.perform(get("/api/categories/{name}", CATEGORY_NAME))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.name").value(CATEGORY_NAME));

        verify(categoryService).getCategoryByName(CATEGORY_NAME);
    }

    @Test
    void getCategoryByName_NotFound() throws Exception {
        when(categoryService.getCategoryByName(CATEGORY_NAME))
                .thenThrow(new RuntimeException("Category not found: " + CATEGORY_NAME));

        mockMvc.perform(get("/api/categories/{name}", CATEGORY_NAME))
                .andExpect(status().is5xxServerError())
                .andExpect(jsonPath("$.error").value("Error getting category: Category not found: " + CATEGORY_NAME))
                .andExpect(jsonPath("$.status").value(500));

        verify(categoryService).getCategoryByName(CATEGORY_NAME);
    }

    @Test
    void createCategory_Success() throws Exception {
        when(categoryService.createCategory(CATEGORY_NAME, CATEGORY_DESCRIPTION)).thenReturn(testCategory);

        mockMvc.perform(post("/api/categories")
                .param("name", CATEGORY_NAME)
                .param("description", CATEGORY_DESCRIPTION))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.name").value(CATEGORY_NAME));

        verify(categoryService).createCategory(CATEGORY_NAME, CATEGORY_DESCRIPTION);
    }

    @Test
    void createCategory_Error() throws Exception {
        when(categoryService.createCategory(CATEGORY_NAME, CATEGORY_DESCRIPTION))
                .thenThrow(new RuntimeException("Category already exists"));

        mockMvc.perform(post("/api/categories")
                .param("name", CATEGORY_NAME)
                .param("description", CATEGORY_DESCRIPTION))
                .andExpect(status().is5xxServerError())
                .andExpect(jsonPath("$.error").value("Error creating category: Category already exists"))
                .andExpect(jsonPath("$.status").value(500));

        verify(categoryService).createCategory(CATEGORY_NAME, CATEGORY_DESCRIPTION);
    }

    @Test
    void deleteCategory_Success() throws Exception {
        mockMvc.perform(delete("/api/categories/{id}", 1L))
                .andExpect(status().isOk());

        verify(categoryService).deleteCategory(1L);
    }

    @Test
    void deleteCategory_Error() throws Exception {
        doThrow(new RuntimeException("Category not found"))
                .when(categoryService).deleteCategory(1L);

        mockMvc.perform(delete("/api/categories/{id}", 1L))
                .andExpect(status().is5xxServerError())
                .andExpect(jsonPath("$.error").value("Error deleting category: Category not found"))
                .andExpect(jsonPath("$.status").value(500));

        verify(categoryService).deleteCategory(1L);
    }
}
