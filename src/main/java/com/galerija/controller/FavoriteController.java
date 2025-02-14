package com.galerija.controller;

import com.galerija.entity.Favorite;
import com.galerija.entity.Image;
import com.galerija.service.FavoriteService;
import com.galerija.service.ImageService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@CrossOrigin(origins = "http://localhost:3000", allowCredentials = "true", maxAge = 3600)
@RestController
@RequestMapping("/api/favorites")
public class FavoriteController {
    private static final Logger logger = LoggerFactory.getLogger(FavoriteController.class);
    
    @Autowired
    private FavoriteService favoriteService;

    @Autowired
    private ImageService imageService;

    @PostMapping("/{imageId}")
    @PreAuthorize("isAuthenticated()")
    @Transactional
    public ResponseEntity<?> addToFavorites(@PathVariable Long imageId, @RequestBody(required = false) Image imageData) {
        try {
            logger.debug("Adding image {} to favorites with data: {}", imageId, imageData);
            
            // Save image if provided
            if (imageData != null) {
                imageData.setId(imageId);
                imageService.saveImage(imageData);
            }
            
            // Add to favorites
            Favorite favorite = favoriteService.addToFavorites(imageId);
            return ResponseEntity.ok(favorite);
        } catch (Exception e) {
            logger.error("Error adding image to favorites: {}", e.getMessage(), e);
            Map<String, Object> response = new HashMap<>();
            response.put("error", e.getMessage());
            response.put("status", 400);
            return ResponseEntity.badRequest().body(response);
        }
    }

    @PostMapping("/batch")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> addBatchToFavorites(@RequestBody List<Long> imageIds) {
        try {
            if (imageIds == null || imageIds.isEmpty()) {
                Map<String, Object> response = new HashMap<>();
                response.put("error", "Image IDs list cannot be empty");
                response.put("status", 400);
                return ResponseEntity.badRequest().body(response);
            }
            List<Favorite> favorites = favoriteService.addBatchToFavorites(imageIds);
            if (favorites.isEmpty()) {
                Map<String, Object> response = new HashMap<>();
                response.put("error", "No images were added to favorites");
                response.put("status", 400);
                return ResponseEntity.badRequest().body(response);
            }
            return ResponseEntity.ok(favorites);
        } catch (Exception e) {
            logger.error("Error adding images to favorites: {}", e.getMessage(), e);
            Map<String, Object> response = new HashMap<>();
            response.put("error", "Error adding images to favorites: " + e.getMessage());
            response.put("status", 500);
            return ResponseEntity.internalServerError().body(response);
        }
    }

    @DeleteMapping("/{imageId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> removeFromFavorites(@PathVariable Long imageId) {
        try {
            logger.debug("Removing image {} from favorites", imageId);
            favoriteService.removeFromFavorites(imageId);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            logger.error("Error removing image from favorites: {}", e.getMessage(), e);
            Map<String, Object> response = new HashMap<>();
            response.put("error", "Error removing image from favorites: " + e.getMessage());
            response.put("status", 500);
            return ResponseEntity.internalServerError().body(response);
        }
    }

    @GetMapping("/{imageId}/check")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> checkFavorite(@PathVariable Long imageId) {
        try {
            logger.debug("Checking if image {} is in favorites", imageId);
            boolean isFavorite = favoriteService.isImageFavorite(imageId);
            return ResponseEntity.ok(isFavorite);
        } catch (Exception e) {
            logger.error("Error checking favorite status: {}", e.getMessage(), e);
            Map<String, Object> response = new HashMap<>();
            response.put("error", "Error checking favorite status: " + e.getMessage());
            response.put("status", 500);
            return ResponseEntity.internalServerError().body(response);
        }
    }

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> getUserFavorites(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        try {
            logger.debug("Getting user favorites with page: {} and size: {}", page, size);
            Page<Favorite> favorites = favoriteService.getUserFavorites(page, size);
            return ResponseEntity.ok(favorites);
        } catch (Exception e) {
            logger.error("Error getting user favorites: {}", e.getMessage(), e);
            Map<String, Object> response = new HashMap<>();
            response.put("error", "Error getting user favorites: " + e.getMessage());
            response.put("status", 500);
            return ResponseEntity.internalServerError().body(response);
        }
    }
}
