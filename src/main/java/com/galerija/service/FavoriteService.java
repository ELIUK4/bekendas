package com.galerija.service;

import com.galerija.entity.Favorite;
import com.galerija.entity.Image;
import com.galerija.entity.UserEntity;
import com.galerija.repository.FavoriteRepository;
import com.galerija.repository.ImageRepository;
import com.galerija.security.SecurityUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Optional;

@Service
public class FavoriteService {
    private static final Logger logger = LoggerFactory.getLogger(FavoriteService.class);

    @Autowired
    private FavoriteRepository favoriteRepository;

    @Autowired
    private ImageRepository imageRepository;

    @Autowired
    private SecurityUtils securityUtils;

    @Transactional
    public Favorite addToFavorites(Long imageId) {
        logger.debug("Adding image {} to favorites", imageId);
        
        UserEntity currentUser = securityUtils.getCurrentUser();
        logger.debug("Current user: {}", currentUser.getUsername());
        
        Image image = imageRepository.findById(imageId)
                .orElseThrow(() -> new RuntimeException("Image not found with id: " + imageId));

        Optional<Favorite> existingFavorite = favoriteRepository.findByUserAndImage(currentUser, image);
        if (existingFavorite.isPresent()) {
            logger.debug("Image {} is already in favorites for user {}", imageId, currentUser.getUsername());
            throw new RuntimeException("Image is already in favorites");
        }

        Favorite favorite = new Favorite();
        favorite.setUser(currentUser);
        favorite.setImage(image);
        
        logger.debug("Saving favorite to database");
        return favoriteRepository.save(favorite);
    }

    @Transactional
    public List<Favorite> addBatchToFavorites(List<Long> imageIds) {
        logger.debug("Adding batch of {} images to favorites", imageIds.size());
        
        UserEntity currentUser = securityUtils.getCurrentUser();
        List<Image> images = imageRepository.findAllById(imageIds);
        
        if (images.size() != imageIds.size()) {
            logger.warn("Some images were not found. Found {}/{}", images.size(), imageIds.size());
            throw new RuntimeException("Some images were not found");
        }

        return images.stream()
                .map(image -> {
                    Optional<Favorite> existingFavorite = favoriteRepository.findByUserAndImage(currentUser, image);
                    if (existingFavorite.isPresent()) {
                        logger.debug("Image {} is already in favorites", image.getId());
                        return existingFavorite.get();
                    }
                    
                    logger.debug("Adding image {} to favorites", image.getId());
                    Favorite favorite = new Favorite();
                    favorite.setUser(currentUser);
                    favorite.setImage(image);
                    return favoriteRepository.save(favorite);
                })
                .toList();
    }

    @Transactional
    public void removeFromFavorites(Long imageId) {
        logger.debug("Removing image {} from favorites", imageId);
        
        UserEntity currentUser = securityUtils.getCurrentUser();
        Image image = imageRepository.findById(imageId)
                .orElseThrow(() -> new RuntimeException("Image not found with id: " + imageId));

        Optional<Favorite> favorite = favoriteRepository.findByUserAndImage(currentUser, image);
        if (favorite.isEmpty()) {
            logger.warn("Image {} is not in favorites for user {}", imageId, currentUser.getUsername());
            throw new RuntimeException("Image is not in favorites");
        }

        logger.debug("Deleting favorite from database");
        favoriteRepository.delete(favorite.get());
    }

    @Transactional(readOnly = true)
    public boolean isImageFavorite(Long imageId) {
        logger.debug("Checking if image {} is favorite", imageId);
        
        UserEntity currentUser = securityUtils.getCurrentUser();
        Image image = imageRepository.findById(imageId)
                .orElseThrow(() -> new RuntimeException("Image not found with id: " + imageId));

        return favoriteRepository.findByUserAndImage(currentUser, image).isPresent();
    }
}
