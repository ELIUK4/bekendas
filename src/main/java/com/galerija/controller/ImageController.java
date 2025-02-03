package com.galerija.controller;

import com.galerija.entity.Image;
import com.galerija.service.ImageService;
import com.galerija.service.SearchHistoryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@CrossOrigin(origins = "*", maxAge = 3600)
@RestController
@RequestMapping("/api/images")
@PreAuthorize("hasRole('USER')")
public class ImageController {
    @Autowired
    private ImageService imageService;

    @Autowired
    private SearchHistoryService searchHistoryService;

    @GetMapping("/search")
    public ResponseEntity<?> searchImages(
            @RequestParam String query,
            @RequestParam(required = false, defaultValue = "photo") String imageType,
            @RequestParam(required = false, defaultValue = "all") String orientation,
            @RequestParam(required = false) String category,
            @RequestParam(required = false, defaultValue = "20") Integer perPage,
            @RequestParam(required = false, defaultValue = "1") Integer page) {
        
        ResponseEntity<String> response = imageService.searchPixabayImages(
                query, imageType, orientation, category, perPage, page);
        
        // Save search history
        searchHistoryService.saveSearch(query, 
                String.format("type=%s,orientation=%s,category=%s", imageType, orientation, category),
                perPage);
        
        return response;
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
