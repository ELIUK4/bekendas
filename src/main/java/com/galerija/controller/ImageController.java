package com.galerija.controller;

import com.galerija.entity.Image;
import com.galerija.service.ImageService;
import com.galerija.service.SearchHistoryService;
import com.galerija.exception.ResourceNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/images")
public class ImageController {

    @Autowired
    private ImageService imageService;

    @Autowired
    private SearchHistoryService searchHistoryService;

    private static final Logger logger = LoggerFactory.getLogger(ImageController.class);

    @GetMapping("/search")
    public ResponseEntity<Map<String, Object>> searchImages(
            @RequestParam(required = false, defaultValue = "camera") String query,
            @RequestParam(required = false, defaultValue = "photo") String imageType,
            @RequestParam(required = false, defaultValue = "all") String orientation,
            @RequestParam(required = false) String category,
            @RequestParam(required = false, defaultValue = "20") Integer perPage,
            @RequestParam(required = false, defaultValue = "1") Integer page) {
        
        logger.info("Received search request - query: {}, type: {}, orientation: {}, category: {}, perPage: {}, page: {}", 
                   query, imageType, orientation, category, perPage, page);
        
        Map<String, Object> response = new HashMap<>();
        
        try {
            // Validate parameters
            if (query == null || query.trim().isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of(
                    "error", "Search query cannot be empty",
                    "data", Collections.emptyList()
                ));
            }
            
            if (perPage != null && (perPage < 1 || perPage > 200)) {
                return ResponseEntity.badRequest().body(Map.of(
                    "error", "perPage must be between 1 and 200",
                    "data", Collections.emptyList()
                ));
            }
            
            if (page != null && page < 1) {
                return ResponseEntity.badRequest().body(Map.of(
                    "error", "page must be greater than 0",
                    "data", Collections.emptyList()
                ));
            }
            
            // Get results from Pixabay API
            List<Image> images = imageService.searchPixabayImages(
                    query, imageType, orientation, category, perPage, page);
            
            if (images.isEmpty()) {
                logger.info("No images found for query: {}", query);
                response.put("data", Collections.emptyList());
                response.put("total", 0);
                return ResponseEntity.ok(response);
            }
            
            response.put("data", images);
            response.put("total", images.size());
            
            // Try to save search history asynchronously
            try {
                StringBuilder filters = new StringBuilder()
                        .append("type=").append(imageType)
                        .append(", orientation=").append(orientation);
                if (category != null) {
                    filters.append(", category=").append(category);
                }
                searchHistoryService.saveSearch(query, filters.toString(), perPage);
            } catch (Exception e) {
                logger.error("Failed to save search history: {}", e.getMessage(), e);
            }
            
            logger.info("Successfully retrieved {} images for query: {}", images.size(), query);
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("Error searching images: {}", e.getMessage(), e);
            response.put("error", "Failed to search images: " + e.getMessage());
            response.put("data", Collections.emptyList());
            response.put("total", 0);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getImage(@PathVariable Long id) {
        try {
            Image image = imageService.getImageById(id);
            return ResponseEntity.ok(image);
        } catch (ResourceNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Image not found");
        }
    }

    @PostMapping
    public ResponseEntity<Image> saveImage(@RequestBody Image image) {
        logger.info("Received request to save image: {}", image);
        Image savedImage = imageService.saveExternalImage(image);
        return ResponseEntity.status(HttpStatus.CREATED).body(savedImage);
    }

    @PostMapping("/{id}/like")
    public ResponseEntity<?> likeImage(@PathVariable Long id) {
        try {
            Image likedImage = imageService.likeImage(id);
            return ResponseEntity.ok(likedImage);
        } catch (ResourceNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Image not found");
        }
    }

    @PostMapping("/external")
    public ResponseEntity<?> saveExternalImage(@RequestBody Image externalImage) {
        try {
            logger.debug("Received external image: {}", externalImage);
            Image savedImage = imageService.saveExternalImage(externalImage);
            return ResponseEntity.status(HttpStatus.CREATED).body(savedImage);
        } catch (Exception e) {
            logger.error("Failed to save external image: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Failed to save external image: " + e.getMessage());
        }
    }

    @GetMapping("/user")
    public ResponseEntity<?> getUserImages() {
        try {
            List<Image> userImages = imageService.getUserImages();
            return ResponseEntity.ok(userImages);
        } catch (Exception e) {
            logger.error("Failed to get user images: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Failed to get user images: " + e.getMessage());
        }
    }
}
