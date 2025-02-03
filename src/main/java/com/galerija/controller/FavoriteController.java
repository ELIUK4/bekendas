package com.galerija.controller;

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

@CrossOrigin(origins = "*", maxAge = 3600)
@RestController
@RequestMapping("/api/favorites")
@PreAuthorize("hasRole('USER')")
public class FavoriteController {
    @Autowired
    private FavoriteService favoriteService;

    @Autowired
    private ImageService imageService;

    @PostMapping("/{imageId}")
    public ResponseEntity<Favorite> addToFavorites(@PathVariable Long imageId) {
        Favorite favorite = favoriteService.addToFavorites(imageId);
        return ResponseEntity.ok(favorite);
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
