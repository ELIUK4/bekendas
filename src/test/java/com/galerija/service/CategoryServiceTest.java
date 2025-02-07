package com.galerija.service;

import com.galerija.entity.Category;
import com.galerija.repository.CategoryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class CategoryServiceTest {

    @Mock
    private CategoryRepository categoryRepository;

    @InjectMocks
    private CategoryService categoryService;

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
        when(categoryRepository.findAll()).thenReturn(categories);

        // Act
        List<Category> result = categoryService.getAllCategories();

        // Assert
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(CATEGORY_NAME, result.get(0).getName());
    }

    @Test
    void getCategoryByName_Success() {
        // Arrange
        when(categoryRepository.findByName(CATEGORY_NAME))
                .thenReturn(Optional.of(testCategory));

        // Act
        Category result = categoryService.getCategoryByName(CATEGORY_NAME);

        // Assert
        assertNotNull(result);
        assertEquals(CATEGORY_NAME, result.getName());
    }

    @Test
    void getCategoryByName_NotFound() {
        // Arrange
        when(categoryRepository.findByName(CATEGORY_NAME))
                .thenReturn(Optional.empty());

        // Act & Assert
        Exception exception = assertThrows(RuntimeException.class, () ->
            categoryService.getCategoryByName(CATEGORY_NAME)
        );
        assertTrue(exception.getMessage().contains("Category not found"));
    }

    @Test
    void initializeDefaultCategories_Success() {
        // Arrange
        when(categoryRepository.existsByName(any())).thenReturn(false);
        when(categoryRepository.save(any(Category.class))).thenReturn(new Category());

        // Act
        categoryService.initializeDefaultCategories();

        // Assert
        verify(categoryRepository, times(Category.PIXABAY_CATEGORIES.length))
                .save(any(Category.class));
    }

    @Test
    void initializeDefaultCategories_AlreadyExists() {
        // Arrange
        when(categoryRepository.existsByName(any())).thenReturn(true);

        // Act
        categoryService.initializeDefaultCategories();

        // Assert
        verify(categoryRepository, never()).save(any(Category.class));
    }

    @Test
    void createCategory_Success() {
        // Arrange
        when(categoryRepository.existsByName(CATEGORY_NAME)).thenReturn(false);
        when(categoryRepository.save(any(Category.class))).thenReturn(testCategory);

        // Act
        Category result = categoryService.createCategory(CATEGORY_NAME, CATEGORY_DESCRIPTION);

        // Assert
        assertNotNull(result);
        assertEquals(CATEGORY_NAME, result.getName());
        assertEquals(CATEGORY_DESCRIPTION, result.getDescription());
    }

    @Test
    void createCategory_AlreadyExists() {
        // Arrange
        when(categoryRepository.existsByName(CATEGORY_NAME)).thenReturn(true);

        // Act & Assert
        Exception exception = assertThrows(RuntimeException.class, () ->
            categoryService.createCategory(CATEGORY_NAME, CATEGORY_DESCRIPTION)
        );
        assertTrue(exception.getMessage().contains("Category already exists"));
        verify(categoryRepository, never()).save(any(Category.class));
    }
}
