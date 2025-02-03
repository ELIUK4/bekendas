package com.galerija.service;

import com.galerija.entity.Category;
import com.galerija.repository.CategoryRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.List;

@Service
public class CategoryService {
    @Autowired
    private CategoryRepository categoryRepository;

    @Transactional(readOnly = true)
    public List<Category> getAllCategories() {
        return categoryRepository.findAll();
    }

    @Transactional(readOnly = true)
    public Category getCategoryByName(String name) {
        return categoryRepository.findByName(name)
                .orElseThrow(() -> new RuntimeException("Category not found: " + name));
    }

    @Transactional
    public void initializeDefaultCategories() {
        Arrays.stream(Category.PIXABAY_CATEGORIES).forEach(categoryName -> {
            if (!categoryRepository.existsByName(categoryName)) {
                Category category = new Category();
                category.setName(categoryName);
                category.setDescription("Pixabay " + categoryName + " category");
                categoryRepository.save(category);
            }
        });
    }

    @Transactional
    public Category createCategory(String name, String description) {
        if (categoryRepository.existsByName(name)) {
            throw new RuntimeException("Category already exists: " + name);
        }

        Category category = new Category();
        category.setName(name);
        category.setDescription(description);
        return categoryRepository.save(category);
    }

    @Transactional
    public void deleteCategory(Long id) {
        categoryRepository.deleteById(id);
    }
}
