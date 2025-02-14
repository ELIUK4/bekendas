package com.galerija.service;

import com.galerija.entity.Image;
import com.galerija.entity.UserEntity;
import com.galerija.exception.ResourceNotFoundException;
import com.galerija.repository.ImageRepository;
import com.galerija.security.SecurityUtils;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.ResponseEntity;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;

class ImageServiceTest {

    @Mock
    private ImageRepository imageRepository;

    @Mock
    private RestTemplate restTemplate;

    @Mock
    private SecurityUtils securityUtils;

    @Mock
    private ObjectMapper objectMapper;

    @InjectMocks
    private ImageService imageService;

    private Image testImage;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);

        testImage = new Image();
        testImage.setId(1L);
        testImage.setWebformatURL("https://test.com/image.jpg");
        testImage.setTags("test, nature");
        testImage.setUserId(1L);
        testImage.setType("photo");
        testImage.setPreviewURL("https://test.com/preview.jpg");
        testImage.setLargeImageURL("https://test.com/large.jpg");

        ReflectionTestUtils.setField(imageService, "apiUrl", "https://pixabay.com/api/");
        ReflectionTestUtils.setField(imageService, "apiKey", "test-api-key");
        ReflectionTestUtils.setField(imageService, "uploadDir", System.getProperty("java.io.tmpdir"));
    }

    @Test
    void getImageById_Success() {
        when(imageRepository.findById(1L)).thenReturn(Optional.of(testImage));

        Image result = imageService.getImageById(1L);

        assertNotNull(result);
        assertEquals(1L, result.getId());
        assertEquals("https://test.com/image.jpg", result.getWebformatURL());
        verify(imageRepository).findById(1L);
    }

    @Test
    void getImageById_NotFound() {
        when(imageRepository.findById(1L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> imageService.getImageById(1L));
        verify(imageRepository).findById(1L);
    }

    @Test
    void saveExternalImage_Success() {
        Image newImage = new Image();
        newImage.setWebformatURL("http://example.com/image.jpg");
        newImage.setTags("test tags");
        
        when(imageRepository.findByWebformatURL(anyString())).thenReturn(Optional.empty());
        when(imageRepository.save(any(Image.class))).thenReturn(newImage);
        
        Image result = imageService.saveExternalImage(newImage);
        
        assertNotNull(result);
        assertEquals("http://example.com/image.jpg", result.getWebformatURL());
        assertEquals("test tags", result.getTags());
        verify(imageRepository).save(any(Image.class));
    }

    @Test
    void saveExternalImage_ExistingImage() {
        Image existingImage = new Image();
        existingImage.setWebformatURL("http://example.com/image.jpg");
        existingImage.setTags("existing tags");
        
        when(imageRepository.findByWebformatURL(anyString())).thenReturn(Optional.of(existingImage));
        
        Image result = imageService.saveExternalImage(existingImage);
        
        assertNotNull(result);
        assertEquals("http://example.com/image.jpg", result.getWebformatURL());
        assertEquals("existing tags", result.getTags());
        verify(imageRepository, never()).save(any(Image.class));
    }

    @Test
    void likeImage_Success() {
        when(imageRepository.findById(1L)).thenReturn(Optional.of(testImage));
        when(imageRepository.save(any(Image.class))).thenAnswer(i -> i.getArgument(0));

        Image result = imageService.likeImage(1L);

        assertNotNull(result);
        assertEquals(1, result.getLikes());
        verify(imageRepository).findById(1L);
        verify(imageRepository).save(testImage);
    }

    @Test
    void likeImage_NotFound() {
        when(imageRepository.findById(1L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> imageService.likeImage(1L));
        verify(imageRepository).findById(1L);
        verify(imageRepository, never()).save(any(Image.class));
    }

    @Test
    void getTotalSearchResults_Success() throws Exception {
        String jsonResponse = "{\"totalHits\": 100}";
        JsonNode rootNode = mock(JsonNode.class);
        JsonNode totalHitsNode = mock(JsonNode.class);
        
        when(totalHitsNode.asInt()).thenReturn(100);
        when(rootNode.has("totalHits")).thenReturn(true);
        when(rootNode.get("totalHits")).thenReturn(totalHitsNode);
        
        ResponseEntity<String> mockResponse = ResponseEntity.ok(jsonResponse);
        when(restTemplate.getForEntity(anyString(), eq(String.class)))
            .thenReturn(mockResponse);
        when(objectMapper.readTree(jsonResponse)).thenReturn(rootNode);

        int result = imageService.getTotalSearchResults("nature", "photo", "all", null);

        assertEquals(100, result);
        verify(restTemplate).getForEntity(anyString(), eq(String.class));
        verify(objectMapper).readTree(jsonResponse);
    }

    @Test
    void getTotalSearchResults_ApiError() throws Exception {
        when(restTemplate.getForEntity(anyString(), eq(String.class)))
            .thenThrow(new RuntimeException("API error"));

        int result = imageService.getTotalSearchResults("nature", "photo", "all", null);

        assertEquals(0, result);
        verify(restTemplate).getForEntity(anyString(), eq(String.class));
        verify(objectMapper, never()).readTree(anyString());
    }

    @Test
    void getTotalSearchResults_InvalidResponse() throws Exception {
        String jsonResponse = "{}";
        JsonNode rootNode = mock(JsonNode.class);
        
        when(rootNode.has("totalHits")).thenReturn(false);
        when(restTemplate.getForEntity(anyString(), eq(String.class)))
            .thenReturn(ResponseEntity.ok(jsonResponse));
        when(objectMapper.readTree(jsonResponse)).thenReturn(rootNode);

        int result = imageService.getTotalSearchResults("nature", "photo", "all", null);

        assertEquals(0, result);
        verify(restTemplate).getForEntity(anyString(), eq(String.class));
        verify(objectMapper).readTree(jsonResponse);
    }

    @Test
    void saveUploadedImage_Success() throws IOException {
        MultipartFile file = mock(MultipartFile.class);
        when(file.isEmpty()).thenReturn(false);
        when(file.getOriginalFilename()).thenReturn("test.jpg");
        when(file.getInputStream()).thenReturn(new ByteArrayInputStream("test".getBytes()));
        
        UserEntity user = new UserEntity();
        user.setId(1L);
        when(securityUtils.getCurrentUser()).thenReturn(user);
        
        when(imageRepository.save(any(Image.class))).thenAnswer(i -> {
            Image savedImage = i.getArgument(0);
            savedImage.setId(1L);
            return savedImage;
        });

        Image result = imageService.saveUploadedImage(file);

        assertNotNull(result);
        assertEquals("test.jpg", result.getOriginalFileName());
        assertEquals("photo", result.getType());
        assertEquals(1L, result.getUserId());
        assertTrue(result.getFileName().endsWith("_test.jpg"));
        verify(imageRepository).save(any(Image.class));
    }

    @Test
    void saveUploadedImage_EmptyFile() {
        MultipartFile file = mock(MultipartFile.class);
        when(file.isEmpty()).thenReturn(true);

        assertThrows(IllegalArgumentException.class, () -> imageService.saveUploadedImage(file));
        verify(imageRepository, never()).save(any(Image.class));
    }

    @Test
    void deleteImage_Success() throws IOException {
        Image image = new Image();
        image.setId(1L);
        image.setFileName("test.jpg");
        image.setUserId(1L);

        UserEntity user = new UserEntity();
        user.setId(1L);
        
        when(imageRepository.findById(1L)).thenReturn(Optional.of(image));
        when(securityUtils.getCurrentUser()).thenReturn(user);

        imageService.deleteImage(1L);

        verify(imageRepository).findById(1L);
        verify(imageRepository).delete(image);
    }

    @Test
    void deleteImage_NotFound() {
        when(imageRepository.findById(1L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> imageService.deleteImage(1L));
        verify(imageRepository).findById(1L);
        verify(imageRepository, never()).delete(any(Image.class));
    }

    @Test
    void deleteImage_UnauthorizedUser() {
        Image image = new Image();
        image.setId(1L);
        image.setFileName("test.jpg");
        image.setUserId(1L);

        UserEntity user = new UserEntity();
        user.setId(2L);
        
        when(imageRepository.findById(1L)).thenReturn(Optional.of(image));
        when(securityUtils.getCurrentUser()).thenReturn(user);

        assertThrows(ResourceNotFoundException.class, () -> imageService.deleteImage(1L));
        verify(imageRepository).findById(1L);
        verify(imageRepository, never()).delete(any(Image.class));
    }
}
