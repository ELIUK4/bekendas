package com.galerija.controller;

import com.galerija.entity.Category;
import com.galerija.service.CategoryService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.test.context.support.WithMockUser;

import java.util.Arrays;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class CategoryControllerTest {

    @Mock
    private CategoryService categoryService;

    @InjectMocks
    private CategoryController categoryController;

    private Category testCategory;
    private static final String CATEGORY_NAME = "nature";
    private static final String CATEGORY_DESCRIPTION = "Nature category";

    @BeforeEach
    void setUp() {
        testCategory = new Category();
        testCategory.setName(CATEGORY_NAME);
        testCategory.setDescription(CATEGORY_DESCRIPTION);
    }

    @Test
    void getAllCategories_Success() {
        // Arrange
        List<Category> categories = Arrays.asList(testCategory);
        when(categoryService.getAllCategories()).thenReturn(categories);

        // Act
        List<Category> result = categoryController.getAllCategories();

        // Assert
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(CATEGORY_NAME, result.get(0).getName());
        verify(categoryService).getAllCategories();
    }

    @Test
    void getCategoryByName_Success() {
        // Arrange
        when(categoryService.getCategoryByName(CATEGORY_NAME)).thenReturn(testCategory);

        // Act
        ResponseEntity<Category> response = categoryController.getCategoryByName(CATEGORY_NAME);

        // Assert
        assertTrue(response.getStatusCode().is2xxSuccessful());
        assertNotNull(response.getBody());
        assertEquals(CATEGORY_NAME, response.getBody().getName());
    }

    @Test
    void getCategoryByName_NotFound() {
        // Arrange
        when(categoryService.getCategoryByName(CATEGORY_NAME))
                .thenThrow(new RuntimeException("Category not found"));

        // Act & Assert
        assertThrows(RuntimeException.class, () -> 
            categoryController.getCategoryByName(CATEGORY_NAME)
        );
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void initializeCategories_Success() {
        // Arrange
        doNothing().when(categoryService).initializeDefaultCategories();

        // Act
        ResponseEntity<?> response = categoryController.initializeCategories();

        // Assert
        assertTrue(response.getStatusCode().is2xxSuccessful());
        assertEquals("Categories initialized successfully", response.getBody());
        verify(categoryService).initializeDefaultCategories();
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void createCategory_Success() {
        // Arrange
        when(categoryService.createCategory(CATEGORY_NAME, CATEGORY_DESCRIPTION))
                .thenReturn(testCategory);

        // Act
        ResponseEntity<Category> response = categoryController.createCategory(CATEGORY_NAME, CATEGORY_DESCRIPTION);

        // Assert
        assertTrue(response.getStatusCode().is2xxSuccessful());
        assertNotNull(response.getBody());
        assertEquals(CATEGORY_NAME, response.getBody().getName());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void createCategory_AlreadyExists() {
        // Arrange
        String expectedErrorMessage = "Category already exists: " + CATEGORY_NAME;
        when(categoryService.createCategory(eq(CATEGORY_NAME), eq(CATEGORY_DESCRIPTION)))
                .thenThrow(new RuntimeException(expectedErrorMessage));

        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class, () -> 
            categoryController.createCategory(CATEGORY_NAME, CATEGORY_DESCRIPTION)
        );
        assertEquals(expectedErrorMessage, exception.getMessage());
        verify(categoryService).createCategory(CATEGORY_NAME, CATEGORY_DESCRIPTION);
    }
}
