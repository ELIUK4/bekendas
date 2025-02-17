package com.galerija.service;

import com.galerija.entity.Favorite;
import com.galerija.entity.Image;
import com.galerija.entity.UserEntity;
import com.galerija.exception.ResourceNotFoundException;
import com.galerija.repository.FavoriteRepository;
import com.galerija.repository.ImageRepository;
import com.galerija.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
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
    private UserRepository userRepository;

    private UserEntity getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            logger.error("No authenticated user found");
            throw new ResourceNotFoundException("No authenticated user found");
        }

        String username = authentication.getName();
        logger.debug("Getting user for username: {}", username);
        
        return userRepository.findByUsername(username)
                .orElseGet(() -> {
                    logger.debug("Creating new user: {}", username);
                    UserEntity user = new UserEntity();
                    user.setUsername(username);
                    return userRepository.save(user);
                });
    }

    @Transactional(readOnly = true)
    public Page<Favorite> getUserFavorites(int page, int size) {
        logger.debug("Getting favorites for page {} with size {}", page, size);
        UserEntity currentUser = getCurrentUser();
        logger.debug("Current user: {}", currentUser.getUsername());
        
        PageRequest pageRequest = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        return favoriteRepository.findByUserIdWithUser(currentUser.getId(), pageRequest);
    }

    @Transactional
    public Favorite addToFavorites(Long imageId) {
        logger.info("Starting to add image with ID {} to favorites", imageId);
        
        if (imageId == null) {
            logger.error("Image ID cannot be null");
            throw new ResourceNotFoundException("Image ID cannot be null");
        }
        
        UserEntity user = getCurrentUser();
        logger.debug("Current user retrieved: {}", user.getUsername());

        Image image = imageRepository.findById(imageId)
                .orElseThrow(() -> {
                    logger.error("Image not found with id: {}", imageId);
                    return new ResourceNotFoundException("Image not found with id: " + imageId);
                });
        logger.debug("Image found: {}", image.getWebformatURL());

        Optional<Favorite> existingFavorite = favoriteRepository.findByUserAndImage(user, image);
        if (existingFavorite.isPresent()) {
            logger.debug("Image already in favorites for user {}", user.getUsername());
            throw new ResourceNotFoundException("Image already in favorites");
        }

        Favorite favorite = new Favorite();
        favorite.setUser(user);
        favorite.setImage(image);
        logger.info("Saving new favorite for user {} and image {}", user.getUsername(), imageId);
        
        return favoriteRepository.save(favorite);
    }

    @Transactional
    public List<Favorite> addBatchToFavorites(List<Long> imageIds) {
        if (imageIds == null || imageIds.isEmpty()) {
            throw new ResourceNotFoundException("Image IDs list cannot be empty");
        }
        
        logger.debug("Adding images {} to favorites", imageIds);
        
        UserEntity currentUser = getCurrentUser();
        List<Favorite> favorites = new ArrayList<>();

        for (Long imageId : imageIds) {
            try {
                Favorite favorite = addToFavorites(imageId);
                favorites.add(favorite);
            } catch (Exception e) {
                logger.error("Error adding image {} to favorites: {}", imageId, e.getMessage());
                // Continue with the next image
            }
        }

        return favorites;
    }

    @Transactional
    public void removeFromFavorites(Long imageId) {
        logger.debug("Removing image {} from favorites", imageId);
        
        UserEntity currentUser = getCurrentUser();
        logger.debug("Current user: {}", currentUser.getUsername());
        
        Image image = imageRepository.findById(imageId)
                .orElseThrow(() -> {
                    logger.error("Image not found with id: {}", imageId);
                    return new ResourceNotFoundException("Image not found with id: " + imageId);
                });

        Optional<Favorite> favorite = favoriteRepository.findByUserAndImage(currentUser, image);
        if (favorite.isEmpty()) {
            logger.debug("Image {} is not in favorites for user {}", imageId, currentUser.getUsername());
            throw new ResourceNotFoundException("Image not in favorites");
        }

        logger.debug("Deleting favorite for image {} and user {}", imageId, currentUser.getUsername());
        favoriteRepository.delete(favorite.get());
    }

    @Transactional(readOnly = true)
    public boolean isImageFavorite(Long imageId) {
        logger.debug("Checking if image {} is in favorites", imageId);
        
        UserEntity currentUser = getCurrentUser();
        logger.debug("Current user: {}", currentUser.getUsername());
        
        Image image = imageRepository.findById(imageId)
                .orElseThrow(() -> {
                    logger.error("Image not found with id: {}", imageId);
                    return new ResourceNotFoundException("Image not found with id: " + imageId);
                });

        return favoriteRepository.findByUserAndImage(currentUser, image).isPresent();
    }
}
