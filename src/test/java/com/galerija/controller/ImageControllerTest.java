package com.galerija.controller;

import com.galerija.entity.Image;
import com.galerija.service.ImageService;
import com.galerija.service.SearchHistoryService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.test.context.support.WithMockUser;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class ImageControllerTest {

    @Mock
    private ImageService imageService;

    @Mock
    private SearchHistoryService searchHistoryService;

    @InjectMocks
    private ImageController imageController;

    private static final String QUERY = "test";
    private static final String IMAGE_TYPE = "photo";
    private static final String ORIENTATION = "all";
    private static final String CATEGORY = "nature";
    private static final Integer PER_PAGE = 20;
    private static final Integer PAGE = 1;

    @BeforeEach
    void setUp() {
        // Common setup if needed
    }

    @Test
    @WithMockUser(roles = "USER")
    void searchImages_Success() {
        // Arrange
        Image mockImage = new Image();
        mockImage.setId(1L);
        List<Image> mockImages = Arrays.asList(mockImage);
        
        when(imageService.searchPixabayImages(
                eq(QUERY), eq(IMAGE_TYPE), eq(ORIENTATION), 
                eq(CATEGORY), eq(PER_PAGE), eq(PAGE)))
                .thenReturn(mockImages);

        // Act
        ResponseEntity<Map<String, Object>> response = imageController.searchImages(
                QUERY, IMAGE_TYPE, ORIENTATION, CATEGORY, PER_PAGE, PAGE);

        // Assert
        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        
        Map<String, Object> responseBody = response.getBody();
        assertEquals(1, responseBody.get("total"));
        assertEquals(1, responseBody.get("totalHits"));
        assertNotNull(responseBody.get("hits"));
        
        List<Image> hits = (List<Image>) responseBody.get("hits");
        assertEquals(1, hits.size());
        assertEquals(1L, hits.get(0).getId());
        
        verify(searchHistoryService).saveSearch(
                eq(QUERY), 
                eq(String.format("type=%s,orientation=%s,category=%s", 
                        IMAGE_TYPE, ORIENTATION, CATEGORY)),
                eq(PER_PAGE));
    }

    @Test
    @WithMockUser(roles = "USER")
    void searchImages_WithDefaultValues() {
        // Arrange
        Image mockImage = new Image();
        mockImage.setId(1L);
        List<Image> mockImages = Arrays.asList(mockImage);
        
        when(imageService.searchPixabayImages(
                eq(QUERY), eq(IMAGE_TYPE), eq(ORIENTATION), 
                eq(CATEGORY), eq(PER_PAGE), eq(PAGE)))
                .thenReturn(mockImages);

        // Act
        ResponseEntity<Map<String, Object>> response = imageController.searchImages(
                QUERY, IMAGE_TYPE, ORIENTATION, CATEGORY, PER_PAGE, PAGE);

        // Assert
        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        
        Map<String, Object> responseBody = response.getBody();
        assertEquals(1, responseBody.get("total"));
        assertEquals(1, responseBody.get("totalHits"));
        assertNotNull(responseBody.get("hits"));
        
        List<Image> hits = (List<Image>) responseBody.get("hits");
        assertEquals(1, hits.size());
        assertEquals(1L, hits.get(0).getId());
        
        verify(searchHistoryService).saveSearch(
                eq(QUERY), 
                eq(String.format("type=%s,orientation=%s,category=%s", 
                        IMAGE_TYPE, ORIENTATION, CATEGORY)),
                eq(PER_PAGE));
    }

    @Test
    @WithMockUser(roles = "USER")
    void searchImages_Error() {
        // Arrange
        when(imageService.searchPixabayImages(
                any(), any(), any(), any(), any(), any()))
                .thenThrow(new RuntimeException("API Error"));

        // Act & Assert
        assertThrows(RuntimeException.class, () -> 
            imageController.searchImages(QUERY, IMAGE_TYPE, ORIENTATION, CATEGORY, PER_PAGE, PAGE)
        );
    }
}
