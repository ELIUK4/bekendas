package com.galerija.service;

import com.galerija.entity.Favorite;
import com.galerija.entity.Image;
import com.galerija.entity.UserEntity;
import com.galerija.repository.FavoriteRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class FavoriteServiceTest {

    private static final Long IMAGE_ID = 1L;
    private static final Long USER_ID = 1L;
    private static final String ERROR_MESSAGE = "Image already in favorites";
    
    @Mock
    private FavoriteRepository favoriteRepository;

    @Mock
    private UserService userService;

    @Mock
    private ImageService imageService;

    @InjectMocks
    private FavoriteService favoriteService;
    
    private Favorite testFavorite;
    private Image testImage;
    private UserEntity testUser;

    @BeforeEach
    void setUp() {
        // Initialize test user
        testUser = new UserEntity();
        testUser.setId(USER_ID);
        testUser.setUsername("testUser");

        // Initialize test image
        testImage = new Image();
        testImage.setId(IMAGE_ID);
        testImage.setTags("Test Image");
        testImage.setPageURL("http://example.com");
        testImage.setType("photo");
        testImage.setPreviewURL("http://example.com/preview");
        testImage.setWebformatURL("http://example.com/webformat");

        // Initialize test favorite
        testFavorite = new Favorite();
        testFavorite.setId(1L);
        testFavorite.setUser(testUser);
        testFavorite.setImage(testImage);
    }

    @Test
    void addToFavorites_Success() {
        // Arrange
        when(userService.getCurrentUser()).thenReturn(testUser);
        when(imageService.getImageById(IMAGE_ID)).thenReturn(testImage);
        when(favoriteRepository.findByUserAndImage(testUser, testImage)).thenReturn(Optional.empty());
        when(favoriteRepository.save(any(Favorite.class))).thenReturn(testFavorite);

        // Act
        Favorite result = favoriteService.addToFavorites(IMAGE_ID);

        // Assert
        assertNotNull(result, "Result should not be null");
        assertEquals(testFavorite.getId(), result.getId(), "Favorite ID should match");
        assertEquals(testUser.getId(), result.getUser().getId(), "User ID should match");
        assertEquals(testImage.getId(), result.getImage().getId(), "Image ID should match");
        verify(favoriteRepository).save(any(Favorite.class));
    }

    @Test
    void addToFavorites_AlreadyExists() {
        // Arrange
        when(userService.getCurrentUser()).thenReturn(testUser);
        when(imageService.getImageById(IMAGE_ID)).thenReturn(testImage);
        when(favoriteRepository.findByUserAndImage(testUser, testImage)).thenReturn(Optional.of(testFavorite));

        // Act & Assert
        RuntimeException exception = assertThrows(
            RuntimeException.class,
            () -> favoriteService.addToFavorites(IMAGE_ID),
            "Should throw RuntimeException when favorite already exists"
        );
        
        assertEquals(ERROR_MESSAGE, exception.getMessage(), "Error message should match");
        verify(favoriteRepository, never()).save(any(Favorite.class));
    }

    @Test
    void removeFromFavorites_Success() {
        // Arrange
        when(userService.getCurrentUser()).thenReturn(testUser);
        when(imageService.getImageById(IMAGE_ID)).thenReturn(testImage);

        // Act
        favoriteService.removeFromFavorites(IMAGE_ID);

        // Assert
        verify(favoriteRepository).deleteByUserAndImage(testUser, testImage);
    }

    @Test
    void removeFromFavorites_NotFound() {
        // Arrange
        String errorMessage = "Favorite not found";
        when(userService.getCurrentUser()).thenReturn(testUser);
        when(imageService.getImageById(IMAGE_ID)).thenReturn(testImage);
        doThrow(new RuntimeException(errorMessage))
            .when(favoriteRepository).deleteByUserAndImage(testUser, testImage);

        // Act & Assert
        RuntimeException exception = assertThrows(
            RuntimeException.class,
            () -> favoriteService.removeFromFavorites(IMAGE_ID),
            "Should throw RuntimeException when favorite not found"
        );
        
        assertEquals(errorMessage, exception.getMessage(), "Error message should match");
    }

    @Test
    void isImageFavorite_True() {
        // Arrange
        when(userService.getCurrentUser()).thenReturn(testUser);
        when(imageService.getImageById(IMAGE_ID)).thenReturn(testImage);
        when(favoriteRepository.findByUserAndImage(testUser, testImage)).thenReturn(Optional.of(testFavorite));

        // Act
        boolean result = favoriteService.isImageFavorite(IMAGE_ID);

        // Assert
        assertTrue(result, "Image should be marked as favorite");
    }

    @Test
    void isImageFavorite_False() {
        // Arrange
        when(userService.getCurrentUser()).thenReturn(testUser);
        when(imageService.getImageById(IMAGE_ID)).thenReturn(testImage);
        when(favoriteRepository.findByUserAndImage(testUser, testImage)).thenReturn(Optional.empty());

        // Act
        boolean result = favoriteService.isImageFavorite(IMAGE_ID);

        // Assert
        assertFalse(result, "Image should not be marked as favorite");
    }
}
