package com.galerija.controller;

import com.galerija.dto.FavoriteRequest;
import com.galerija.entity.Favorite;
import com.galerija.entity.Image;
import com.galerija.service.FavoriteService;
import com.galerija.service.ImageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/favorites")
@PreAuthorize("hasRole('USER')")
public class FavoriteController {
    @Autowired
    private FavoriteService favoriteService;

    @Autowired
    private ImageService imageService;

    @PostMapping("/{imageId}")
    public ResponseEntity<Favorite> addToFavorites(
            @PathVariable Long imageId,
            @RequestBody(required = false) FavoriteRequest request) {
        Favorite favorite = favoriteService.addToFavorites(imageId);
        return ResponseEntity.ok(favorite);
    }

    @PostMapping("/batch")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<?> addBatchToFavorites(@RequestBody List<Long> imageIds) {
        try {
            if (imageIds == null || imageIds.isEmpty()) {
                return ResponseEntity.badRequest().body("Image IDs list cannot be empty");
            }
            List<Favorite> favorites = favoriteService.addBatchToFavorites(imageIds);
            if (favorites.isEmpty()) {
                return ResponseEntity.badRequest().body("No images were added to favorites");
            }
            return ResponseEntity.ok(favorites);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("Error adding images to favorites: " + e.getMessage());
        }
    }

    @DeleteMapping("/{imageId}")
    public ResponseEntity<?> removeFromFavorites(@PathVariable Long imageId) {
        favoriteService.removeFromFavorites(imageId);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/check/{imageId}")
    public ResponseEntity<Boolean> checkFavorite(@PathVariable Long imageId) {
        boolean isFavorite = favoriteService.isImageFavorite(imageId);
        return ResponseEntity.ok(isFavorite);
    }

    @GetMapping
    public Page<Image> getUserFavorites(Pageable pageable) {
        return imageService.getUserFavorites(null, pageable); // null будет заменен на текущего пользователя в сервисе
    }
}
