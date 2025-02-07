package com.galerija.controller;

import com.galerija.entity.Image;
import com.galerija.service.ImageService;
import com.galerija.service.SearchHistoryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/images")
@CrossOrigin(origins = "http://localhost:3000", allowedHeaders = "*", allowCredentials = "true", maxAge = 3600)
public class ImageController {
    @Autowired
    private ImageService imageService;

    @Autowired
    private SearchHistoryService searchHistoryService;

    @GetMapping("/search")
    public ResponseEntity<Map<String, Object>> searchImages(
            @RequestParam String query,
            @RequestParam(required = false, defaultValue = "photo") String imageType,
            @RequestParam(required = false, defaultValue = "all") String orientation,
            @RequestParam(required = false) String category,
            @RequestParam(required = false, defaultValue = "20") Integer perPage,
            @RequestParam(required = false, defaultValue = "1") Integer page) {
        
        List<Image> images = imageService.searchPixabayImages(
                query, imageType, orientation, category, perPage, page);
        
        // Save search history
        searchHistoryService.saveSearch(query, 
                String.format("type=%s,orientation=%s,category=%s", imageType, orientation, category),
                perPage);
        
        // Create response in Pixabay format
        Map<String, Object> response = new HashMap<>();
        response.put("total", images.size());
        response.put("totalHits", images.size());
        response.put("hits", images);
        
        return ResponseEntity.ok(response);
    }

    @GetMapping("/local")
    public Page<Image> searchLocalImages(
            @RequestParam String query,
            Pageable pageable) {
        return imageService.searchImages(query, pageable);
    }

    @GetMapping("/{id}")
    public ResponseEntity<Image> getImage(@PathVariable Long id) {
        Image image = imageService.getImageById(id);
        return ResponseEntity.ok(image);
    }

    @PostMapping
    public ResponseEntity<Image> saveImage(@RequestBody Image image) {
        Image savedImage = imageService.saveImage(image);
        return ResponseEntity.ok(savedImage);
    }
}
