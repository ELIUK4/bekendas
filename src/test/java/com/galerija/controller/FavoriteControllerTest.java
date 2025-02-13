package com.galerija.controller;

import com.galerija.entity.Favorite;
import com.galerija.entity.Image;
import com.galerija.entity.UserEntity;
import com.galerija.service.FavoriteService;
import com.galerija.service.ImageService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.Arrays;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class FavoriteControllerTest {

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    @Mock
    private FavoriteService favoriteService;

    @Mock
    private ImageService imageService;

    @InjectMocks
    private FavoriteController favoriteController;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        objectMapper = new ObjectMapper();
        mockMvc = MockMvcBuilders
            .standaloneSetup(favoriteController)
            .build();
    }

    private Favorite createTestFavorite(Long id, Long userId, Long imageId) {
        UserEntity user = new UserEntity();
        user.setId(userId);
        user.setUsername("testUser");

        Image image = new Image();
        image.setId(imageId);
        image.setWebformatURL("test-url");
        image.setTags("test-tags");

        Favorite favorite = new Favorite();
        favorite.setId(id);
        favorite.setUser(user);
        favorite.setImage(image);
        return favorite;
    }

    @Test
    void testAddToFavorites_Success() throws Exception {
        Long imageId = 1L;
        Favorite favorite = createTestFavorite(1L, 1L, imageId);
        when(favoriteService.addToFavorites(imageId)).thenReturn(favorite);

        mockMvc.perform(post("/api/favorites/{imageId}", imageId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.user.id").value(1))
                .andExpect(jsonPath("$.image.id").value(imageId));

        verify(favoriteService).addToFavorites(imageId);
    }

    @Test
    void testAddToFavorites_NotFound() throws Exception {
        Long imageId = 1L;
        when(favoriteService.addToFavorites(imageId))
                .thenThrow(new RuntimeException("Image not found"));

        mockMvc.perform(post("/api/favorites/{imageId}", imageId))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.error").value("Error adding image to favorites: Image not found"))
                .andExpect(jsonPath("$.status").value(500));

        verify(favoriteService).addToFavorites(imageId);
    }

    @Test
    void testAddBatchToFavorites_Success() throws Exception {
        List<Long> imageIds = Arrays.asList(1L, 2L);
        List<Favorite> favorites = Arrays.asList(
            createTestFavorite(1L, 1L, 1L),
            createTestFavorite(2L, 1L, 2L)
        );
        when(favoriteService.addBatchToFavorites(imageIds)).thenReturn(favorites);

        mockMvc.perform(post("/api/favorites/batch")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(imageIds)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[1].id").value(2));

        verify(favoriteService).addBatchToFavorites(imageIds);
    }

    @Test
    void testAddBatchToFavorites_EmptyList() throws Exception {
        mockMvc.perform(post("/api/favorites/batch")
                .contentType(MediaType.APPLICATION_JSON)
                .content("[]"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Image IDs list cannot be empty"))
                .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    void testRemoveFromFavorites_Success() throws Exception {
        Long imageId = 1L;
        doNothing().when(favoriteService).removeFromFavorites(imageId);

        mockMvc.perform(delete("/api/favorites/{imageId}", imageId))
                .andExpect(status().isOk());

        verify(favoriteService).removeFromFavorites(imageId);
    }

    @Test
    void testRemoveFromFavorites_NotFound() throws Exception {
        Long imageId = 1L;
        doThrow(new RuntimeException("Favorite not found"))
            .when(favoriteService).removeFromFavorites(imageId);

        mockMvc.perform(delete("/api/favorites/{imageId}", imageId))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.error").value("Error removing image from favorites: Favorite not found"))
                .andExpect(jsonPath("$.status").value(500));

        verify(favoriteService).removeFromFavorites(imageId);
    }
}
