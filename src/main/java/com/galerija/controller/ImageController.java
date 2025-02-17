package com.galerija.controller;

import com.galerija.entity.Image;
import com.galerija.entity.ImageDto;
import com.galerija.entity.ImagePrivacy;
import com.galerija.service.ImageService;
import com.galerija.service.SearchHistoryService;
import com.galerija.exception.ResourceNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/images")
@CrossOrigin(origins = "http://localhost:3000", allowCredentials = "true", maxAge = 3600)
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
    @PreAuthorize("isAuthenticated()")
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
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> getUserImages() {
        try {
            List<ImageDto> userImages = imageService.getUserImages();
            return ResponseEntity.ok(userImages);
        } catch (ResourceNotFoundException e) {
            logger.error("Failed to get user images: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", "Vartotojas nerastas", "message", e.getMessage()));
        } catch (Exception e) {
            logger.error("Failed to get user images: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Nepavyko gauti vartotojo nuotraukų", "message", e.getMessage()));
        }
    }

    @GetMapping("/public")
    public ResponseEntity<Page<ImageDto>> getPublicImages(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir) {
        
        Sort.Direction direction = Sort.Direction.fromString(sortDir.toUpperCase());
        PageRequest pageRequest = PageRequest.of(page, size, Sort.by(direction, sortBy));
        
        Page<ImageDto> images = imageService.getPublicImages(pageRequest);
        return ResponseEntity.ok(images);
    }

    @PutMapping("/{id}/privacy")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> updateImagePrivacy(
            @PathVariable Long id,
            @RequestParam ImagePrivacy privacy) {
        try {
            Image updatedImage = imageService.updateImagePrivacy(id, privacy);
            return ResponseEntity.ok(imageService.toDto(updatedImage));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Failed to update image privacy: " + e.getMessage());
        }
    }

    @PostMapping("/upload")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> uploadImage(@RequestParam("file") MultipartFile file) {
        try {
            if (file == null || file.isEmpty()) {
                logger.error("Received empty file");
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "Nepasirinktas failas", "message", "File is empty"));
            }

            logger.debug("Received file upload request: name={}, size={}, contentType={}", 
                    file.getOriginalFilename(), file.getSize(), file.getContentType());

            if (!file.getContentType().startsWith("image/")) {
                logger.error("Invalid file type: {}", file.getContentType());
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "Netinkamas failo tipas", 
                                   "message", "Only image files are allowed"));
            }

            Image savedImage = imageService.saveUploadedImage(file);
            logger.info("Successfully uploaded image: {}", savedImage.getId());
            return ResponseEntity.status(HttpStatus.CREATED).body(savedImage);
        } catch (ResourceNotFoundException e) {
            logger.error("Failed to upload image - user not found: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", "Vartotojas nerastas", "message", e.getMessage()));
        } catch (IllegalArgumentException e) {
            logger.error("Failed to upload image - invalid argument: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "Neteisingi duomenys", "message", e.getMessage()));
        } catch (RuntimeException e) {
            logger.error("Failed to upload image - runtime error: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Nepavyko įkelti nuotraukos", 
                                "message", "Failed to store file: " + e.getMessage()));
        } catch (Exception e) {
            logger.error("Failed to upload image - unexpected error: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Netikėta klaida", "message", e.getMessage()));
        }
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> deleteImage(@PathVariable Long id) {
        try {
            logger.debug("Deleting image with id: {}", id);
            imageService.deleteImage(id);
            return ResponseEntity.ok().build();
        } catch (ResourceNotFoundException e) {
            logger.error("Image not found for deletion: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", "Nuotrauka nerasta", "message", e.getMessage()));
        } catch (Exception e) {
            logger.error("Failed to delete image: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Nepavyko ištrinti nuotraukos", "message", e.getMessage()));
        }
    }
}
