package com.galerija.controller;

import com.galerija.entity.Category;
import com.galerija.service.CategoryService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@CrossOrigin(origins = "http://localhost:3000", allowCredentials = "true", maxAge = 3600)
@RestController
@RequestMapping("/api/categories")
public class CategoryController {
    private static final Logger logger = LoggerFactory.getLogger(CategoryController.class);
    
    @Autowired
    private CategoryService categoryService;

    @GetMapping
    @PreAuthorize("permitAll()")
    public ResponseEntity<?> getAllCategories() {
        try {
            List<String> categories = List.of(
                "backgrounds", "fashion", "nature", "science", "education", 
                "feelings", "health", "people", "religion", "places", 
                "animals", "industry", "computer", "food", "sports", 
                "transportation", "travel", "buildings", "business", "music"
            );
            Map<String, Object> response = new HashMap<>();
            response.put("data", categories);
            response.put("total", categories.size());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Error getting categories: {}", e.getMessage(), e);
            Map<String, Object> response = new HashMap<>();
            response.put("error", "Error getting categories: " + e.getMessage());
            response.put("data", Collections.emptyList());
            response.put("status", 500);
            return ResponseEntity.internalServerError().body(response);
        }
    }

    @GetMapping("/{name}")
    @PreAuthorize("permitAll()")
    public ResponseEntity<?> getCategoryByName(@PathVariable String name) {
        try {
            Category category = categoryService.getCategoryByName(name);
            if (category == null) {
                Map<String, Object> response = new HashMap<>();
                response.put("error", "Category not found: " + name);
                response.put("status", 404);
                return ResponseEntity.status(404).body(response);
            }
            return ResponseEntity.ok(category);
        } catch (Exception e) {
            logger.error("Error getting category by name: {}", e.getMessage(), e);
            Map<String, Object> response = new HashMap<>();
            response.put("error", "Error getting category: " + e.getMessage());
            response.put("status", 500);
            return ResponseEntity.internalServerError().body(response);
        }
    }

    @PostMapping("/initialize")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> initializeCategories() {
        try {
            categoryService.initializeDefaultCategories();
            return ResponseEntity.ok("Categories initialized successfully");
        } catch (Exception e) {
            logger.error("Error initializing categories: {}", e.getMessage(), e);
            Map<String, Object> response = new HashMap<>();
            response.put("error", "Error initializing categories: " + e.getMessage());
            response.put("status", 500);
            return ResponseEntity.internalServerError().body(response);
        }
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> createCategory(
            @RequestParam String name,
            @RequestParam(required = false) String description) {
        try {
            Category category = categoryService.createCategory(name, description);
            return ResponseEntity.ok(category);
        } catch (Exception e) {
            logger.error("Error creating category: {}", e.getMessage(), e);
            Map<String, Object> response = new HashMap<>();
            response.put("error", "Error creating category: " + e.getMessage());
            response.put("status", 500);
            return ResponseEntity.internalServerError().body(response);
        }
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> deleteCategory(@PathVariable Long id) {
        try {
            categoryService.deleteCategory(id);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            logger.error("Error deleting category: {}", e.getMessage(), e);
            Map<String, Object> response = new HashMap<>();
            response.put("error", "Error deleting category: " + e.getMessage());
            response.put("status", 500);
            return ResponseEntity.internalServerError().body(response);
        }
    }
}
