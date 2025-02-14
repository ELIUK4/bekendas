package com.galerija.service;

import com.galerija.entity.Image;
import com.galerija.repository.ImageRepository;
import com.galerija.exception.ResourceNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestTemplate;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class ImageServiceTest {

    @Mock
    private ImageRepository imageRepository;

    @Mock
    private RestTemplate restTemplate;

    @InjectMocks
    private ImageService imageService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(imageService, "apiUrl", "https://pixabay.com/api/");
        ReflectionTestUtils.setField(imageService, "apiKey", "test-api-key");
    }

    private Image createTestImage() {
        Image image = new Image();
        image.setId(1L);
        image.setWebformatURL("https://test.com/image.jpg");
        image.setTags("test, nature");
        image.setUserId(1L);
        image.setLikes(0);
        image.setViews(0);
        image.setDownloads(0);
        image.setComments(0);
        image.setType("photo");
        image.setPreviewURL("https://test.com/preview.jpg");
        image.setLargeImageURL("https://test.com/large.jpg");
        return image;
    }

    @Test
    void getImageById_Success() {
        // Arrange
        Image mockImage = createTestImage();
        when(imageRepository.findById(1L)).thenReturn(Optional.of(mockImage));

        // Act
        Image result = imageService.getImageById(1L);

        // Assert
        assertNotNull(result);
        assertEquals(1L, result.getId());
        assertEquals("https://test.com/image.jpg", result.getWebformatURL());
        verify(imageRepository).findById(1L);
    }

    @Test
    void getImageById_NotFound() {
        // Arrange
        when(imageRepository.findById(1L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(ResourceNotFoundException.class, () -> imageService.getImageById(1L));
        verify(imageRepository).findById(1L);
    }

    @Test
    void whenSaveExternalImage_thenImageIsSaved() {
        // Given
        Image externalImage = new Image();
        externalImage.setWebformatURL("http://example.com/image.jpg");
        externalImage.setTags("test tags");
        externalImage.setUserId(1L);
        
        when(imageRepository.findByWebformatURL(anyString())).thenReturn(Optional.empty());
        when(imageRepository.save(any(Image.class))).thenReturn(externalImage);
        
        // When
        Image savedImage = imageService.saveExternalImage(externalImage);
        
        // Then
        assertNotNull(savedImage);
        assertEquals("http://example.com/image.jpg", savedImage.getWebformatURL());
        assertEquals("test tags", savedImage.getTags());
        assertEquals(1L, savedImage.getUserId());
        verify(imageRepository).save(any(Image.class));
    }

    @Test
    void whenSaveExternalImageWithExistingUrl_thenReturnExistingImage() {
        // Given
        Image existingImage = new Image();
        existingImage.setWebformatURL("http://example.com/image.jpg");
        existingImage.setTags("existing tags");
        existingImage.setUserId(1L);
        
        when(imageRepository.findByWebformatURL(anyString())).thenReturn(Optional.of(existingImage));
        
        // When
        Image result = imageService.saveExternalImage(existingImage);
        
        // Then
        assertNotNull(result);
        assertEquals("http://example.com/image.jpg", result.getWebformatURL());
        assertEquals("existing tags", result.getTags());
        assertEquals(1L, result.getUserId());
        verify(imageRepository, never()).save(any(Image.class));
    }

    @Test
    void likeImage_Success() {
        // Arrange
        Image mockImage = createTestImage();
        when(imageRepository.findById(1L)).thenReturn(Optional.of(mockImage));
        when(imageRepository.save(any(Image.class))).thenAnswer(i -> i.getArgument(0));

        // Act
        Image result = imageService.likeImage(1L);

        // Assert
        assertNotNull(result);
        assertEquals(1, result.getLikes());
        verify(imageRepository).findById(1L);
        verify(imageRepository).save(mockImage);
    }

    @Test
    void likeImage_NotFound() {
        // Arrange
        when(imageRepository.findById(1L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(ResourceNotFoundException.class, () -> imageService.likeImage(1L));
        verify(imageRepository).findById(1L);
        verify(imageRepository, never()).save(any(Image.class));
    }

    @Test
    void getTotalSearchResults_GrazinaTeisingaRezultatuSkaiciu() throws Exception {
        // Paruošiame duomenis
        String query = "gamta";
        String imageType = "photo";
        String orientation = "all";
        String category = null;
        
        String jsonResponse = "{\"totalHits\": 100, \"hits\": []}";
        when(restTemplate.getForEntity(anyString(), eq(String.class)))
            .thenReturn(ResponseEntity.ok(jsonResponse));

        // Vykdome metodą
        int totalResults = imageService.getTotalSearchResults(query, imageType, orientation, category);

        // Tikriname rezultatus
        assertEquals(100, totalResults);
        verify(restTemplate).getForEntity(anyString(), eq(String.class));
    }

    @Test
    void getTotalSearchResults_GrazinaNuliKaiKlaida() throws Exception {
        // Paruošiame duomenis
        String query = "gamta";
        String imageType = "photo";
        String orientation = "all";
        String category = null;
        
        when(restTemplate.getForEntity(anyString(), eq(String.class)))
            .thenReturn(ResponseEntity.badRequest().body(null));

        // Vykdome metodą
        int totalResults = imageService.getTotalSearchResults(query, imageType, orientation, category);

        // Tikriname rezultatus
        assertEquals(0, totalResults);
        verify(restTemplate).getForEntity(anyString(), eq(String.class));
    }

    @Test
    void getTotalSearchResults_GrazinaNuliKaiNeteisingasAtsakymas() throws Exception {
        // Paruošiame duomenis
        String query = "gamta";
        String imageType = "photo";
        String orientation = "all";
        String category = null;
        
        when(restTemplate.getForEntity(anyString(), eq(String.class)))
            .thenReturn(ResponseEntity.ok("{\"error\": \"Invalid API key\"}"));

        // Vykdome metodą
        int totalResults = imageService.getTotalSearchResults(query, imageType, orientation, category);

        // Tikriname rezultatus
        assertEquals(0, totalResults);
        verify(restTemplate).getForEntity(anyString(), eq(String.class));
    }
}
