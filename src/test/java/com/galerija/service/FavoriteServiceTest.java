package com.galerija.service;

import com.galerija.config.SecurityTestConfig;
import com.galerija.entity.Favorite;
import com.galerija.entity.Image;
import com.galerija.entity.UserEntity;
import com.galerija.exception.ResourceNotFoundException;
import com.galerija.repository.FavoriteRepository;
import com.galerija.repository.ImageRepository;
import com.galerija.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;

@ExtendWith({MockitoExtension.class, SpringExtension.class})
@ContextConfiguration(classes = SecurityTestConfig.class)
class FavoriteServiceTest {

    private static final Long IMAGE_ID = 1L;
    private static final Long USER_ID = 1L;
    private static final String USERNAME = "testUser";
    private static final String ERROR_MESSAGE = "Image already in favorites";
    
    @Mock
    private FavoriteRepository favoriteRepository;

    @Mock
    private ImageRepository imageRepository;

    @Mock
    private UserRepository userRepository;

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
        testUser.setUsername(USERNAME);

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

        // Mock userRepository
        when(userRepository.findByUsername(USERNAME)).thenReturn(Optional.of(testUser));
    }

    @Test
    @WithMockUser(username = USERNAME)
    void addToFavorites_Success() {
        // Arrange
        when(imageRepository.findById(IMAGE_ID)).thenReturn(Optional.of(testImage));
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
    @WithMockUser(username = USERNAME)
    void addToFavorites_AlreadyExists() {
        // Arrange
        when(imageRepository.findById(IMAGE_ID)).thenReturn(Optional.of(testImage));
        when(favoriteRepository.findByUserAndImage(testUser, testImage)).thenReturn(Optional.of(testFavorite));

        // Act & Assert
        ResourceNotFoundException exception = assertThrows(
            ResourceNotFoundException.class,
            () -> favoriteService.addToFavorites(IMAGE_ID),
            "Should throw ResourceNotFoundException when favorite already exists"
        );
        
        assertEquals(ERROR_MESSAGE, exception.getMessage(), "Error message should match");
        verify(favoriteRepository, never()).save(any(Favorite.class));
    }

    @Test
    @WithMockUser(username = USERNAME)
    void removeFromFavorites_Success() {
        // Arrange
        when(imageRepository.findById(IMAGE_ID)).thenReturn(Optional.of(testImage));
        when(favoriteRepository.findByUserAndImage(testUser, testImage)).thenReturn(Optional.of(testFavorite));

        // Act
        favoriteService.removeFromFavorites(IMAGE_ID);

        // Assert
        verify(favoriteRepository).delete(testFavorite);
    }

    @Test
    @WithMockUser(username = USERNAME)
    void removeFromFavorites_NotFound() {
        // Arrange
        String errorMessage = "Image not found with id: " + IMAGE_ID;
        when(imageRepository.findById(IMAGE_ID)).thenReturn(Optional.empty());

        // Act & Assert
        ResourceNotFoundException exception = assertThrows(
            ResourceNotFoundException.class,
            () -> favoriteService.removeFromFavorites(IMAGE_ID),
            "Should throw ResourceNotFoundException when image not found"
        );
        
        assertEquals(errorMessage, exception.getMessage(), "Error message should match");
        verify(favoriteRepository, never()).delete(any());
    }

    @Test
    @WithMockUser(username = USERNAME)
    void removeFromFavorites_NotInFavorites() {
        // Arrange
        when(imageRepository.findById(IMAGE_ID)).thenReturn(Optional.of(testImage));
        when(favoriteRepository.findByUserAndImage(testUser, testImage)).thenReturn(Optional.empty());

        // Act & Assert
        ResourceNotFoundException exception = assertThrows(
            ResourceNotFoundException.class,
            () -> favoriteService.removeFromFavorites(IMAGE_ID),
            "Should throw ResourceNotFoundException when image is not in favorites"
        );
        
        assertEquals("Image not in favorites", exception.getMessage(), "Error message should match");
        verify(favoriteRepository, never()).delete(any());
    }

    @Test
    @WithMockUser(username = USERNAME)
    void isImageFavorite_True() {
        // Arrange
        when(imageRepository.findById(IMAGE_ID)).thenReturn(Optional.of(testImage));
        when(favoriteRepository.findByUserAndImage(testUser, testImage)).thenReturn(Optional.of(testFavorite));

        // Act
        boolean result = favoriteService.isImageFavorite(IMAGE_ID);

        // Assert
        assertTrue(result, "Image should be marked as favorite");
    }

    @Test
    @WithMockUser(username = USERNAME)
    void isImageFavorite_False() {
        // Arrange
        when(imageRepository.findById(IMAGE_ID)).thenReturn(Optional.of(testImage));
        when(favoriteRepository.findByUserAndImage(testUser, testImage)).thenReturn(Optional.empty());

        // Act
        boolean result = favoriteService.isImageFavorite(IMAGE_ID);

        // Assert
        assertFalse(result, "Image should not be marked as favorite");
    }

    @Test
    @WithMockUser(username = USERNAME)
    void isImageFavorite_ImageNotFound() {
        // Arrange
        when(imageRepository.findById(IMAGE_ID)).thenReturn(Optional.empty());

        // Act & Assert
        ResourceNotFoundException exception = assertThrows(
            ResourceNotFoundException.class,
            () -> favoriteService.isImageFavorite(IMAGE_ID),
            "Should throw ResourceNotFoundException when image not found"
        );
        
        assertEquals("Image not found with id: " + IMAGE_ID, exception.getMessage(), "Error message should match");
    }
}
