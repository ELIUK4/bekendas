package com.galerija.service;

import com.galerija.entity.Favorite;
import com.galerija.entity.Image;
import com.galerija.entity.UserEntity;
import com.galerija.repository.FavoriteRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class FavoriteService {
    @Autowired
    private FavoriteRepository favoriteRepository;

    @Autowired
    private UserService userService;

    @Autowired
    private ImageService imageService;

    @Transactional
    public Favorite addToFavorites(Long imageId) {
        UserEntity currentUser = userService.getCurrentUser();
        Image image = imageService.getImageById(imageId);

        if (favoriteRepository.existsByUserAndImage(currentUser, image)) {
            throw new RuntimeException("Image already in favorites");
        }

        Favorite favorite = new Favorite();
        favorite.setUser(currentUser);
        favorite.setImage(image);

        return favoriteRepository.save(favorite);
    }

    @Transactional
    public void removeFromFavorites(Long imageId) {
        UserEntity currentUser = userService.getCurrentUser();
        Image image = imageService.getImageById(imageId);

        favoriteRepository.deleteByUserAndImage(currentUser, image);
    }

    @Transactional(readOnly = true)
    public boolean isImageFavorite(Long imageId) {
        UserEntity currentUser = userService.getCurrentUser();
        Image image = imageService.getImageById(imageId);

        return favoriteRepository.existsByUserAndImage(currentUser, image);
    }
}
