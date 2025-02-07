package com.galerija.controller;

import com.galerija.dto.FavoriteRequest;
import com.galerija.entity.Favorite;
import com.galerija.entity.Image;
import com.galerija.entity.UserEntity;
import com.galerija.service.FavoriteService;
import com.galerija.service.ImageService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;
import org.springframework.security.test.context.support.WithMockUser;

import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class FavoriteControllerTest {

    @Mock
    private FavoriteService favoriteService;

    @Mock
    private ImageService imageService;

    @InjectMocks
    private FavoriteController favoriteController;

    private static final Long IMAGE_ID = 1L;
    private Favorite testFavorite;
    private Image testImage;
    private UserEntity testUser;
    private FavoriteRequest testRequest;

    @BeforeEach
    void setUp() {
        testUser = new UserEntity();
        testUser.setId(1L);
        testUser.setUsername("testUser");

        testImage = new Image();
        testImage.setId(IMAGE_ID);
        testImage.setTags("Test Image");

        testFavorite = new Favorite();
        testFavorite.setId(1L);
        testFavorite.setUser(testUser);
        testFavorite.setImage(testImage);

        testRequest = new FavoriteRequest();
        testRequest.setUrl("http://example.com/image.jpg");
        testRequest.setTags("test, image");
        testRequest.setUser("testUser");
    }

    @Test
    @WithMockUser(roles = "USER")
    void addToFavorites_Success() {
        // Arrange
        when(favoriteService.addToFavorites(IMAGE_ID)).thenReturn(testFavorite);

        // Act
        ResponseEntity<Favorite> response = favoriteController.addToFavorites(IMAGE_ID, testRequest);

        // Assert
        assertTrue(response.getStatusCode().is2xxSuccessful());
        assertNotNull(response.getBody());
        assertEquals(testFavorite.getId(), response.getBody().getId());
        assertEquals(testImage.getId(), response.getBody().getImage().getId());
        verify(favoriteService).addToFavorites(IMAGE_ID);
    }

    @Test
    @WithMockUser(roles = "USER")
    void addToFavorites_AlreadyExists() {
        // Arrange
        when(favoriteService.addToFavorites(IMAGE_ID))
                .thenThrow(new RuntimeException("Image is already in favorites"));

        // Act & Assert
        Exception exception = assertThrows(RuntimeException.class, () ->
            favoriteController.addToFavorites(IMAGE_ID, testRequest)
        );
        assertTrue(exception.getMessage().contains("Image is already in favorites"));
    }

    @Test
    @WithMockUser(roles = "USER")
    void removeFromFavorites_Success() {
        // Arrange
        doNothing().when(favoriteService).removeFromFavorites(IMAGE_ID);

        // Act
        ResponseEntity<?> response = favoriteController.removeFromFavorites(IMAGE_ID);

        // Assert
        assertTrue(response.getStatusCode().is2xxSuccessful());
        verify(favoriteService).removeFromFavorites(IMAGE_ID);
    }

    @Test
    @WithMockUser(roles = "USER")
    void checkFavorite_True() {
        // Arrange
        when(favoriteService.isImageFavorite(IMAGE_ID)).thenReturn(true);

        // Act
        ResponseEntity<Boolean> response = favoriteController.checkFavorite(IMAGE_ID);

        // Assert
        assertTrue(response.getStatusCode().is2xxSuccessful());
        assertNotNull(response.getBody());
        assertTrue(response.getBody());
        verify(favoriteService).isImageFavorite(IMAGE_ID);
    }

    @Test
    @WithMockUser(roles = "USER")
    void checkFavorite_False() {
        // Arrange
        when(favoriteService.isImageFavorite(IMAGE_ID)).thenReturn(false);

        // Act
        ResponseEntity<Boolean> response = favoriteController.checkFavorite(IMAGE_ID);

        // Assert
        assertTrue(response.getStatusCode().is2xxSuccessful());
        assertNotNull(response.getBody());
        assertFalse(response.getBody());
        verify(favoriteService).isImageFavorite(IMAGE_ID);
    }
}
