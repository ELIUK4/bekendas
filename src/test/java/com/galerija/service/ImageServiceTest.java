package com.galerija.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.galerija.entity.Image;
import com.galerija.repository.ImageRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestTemplate;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
@SpringBootTest
@ActiveProfiles("test")
class ImageServiceTest {

    @Mock
    private ImageRepository imageRepository;

    @Mock
    private RestTemplate restTemplate;

    @Mock
    private ObjectMapper objectMapper;

    @InjectMocks
    private ImageService imageService;

    private static final Long IMAGE_ID = 1L;
    private static final Long USER_ID = 1L;
    private static final String QUERY = "test";
    private static final String API_KEY = "48247705-1f17db8e4da96243d471ac295";
    private static final String API_URL = "https://pixabay.com/api/";

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(imageService, "apiKey", API_KEY);
        ReflectionTestUtils.setField(imageService, "apiUrl", API_URL);
    }

    @Test
    void searchImages_Success() {
        // Arrange
        Pageable pageable = PageRequest.of(0, 10);
        Image image = new Image();
        image.setId(IMAGE_ID);
        Page<Image> expectedPage = new PageImpl<>(Arrays.asList(image));
        
        when(imageRepository.searchImages(eq(QUERY), eq(pageable)))
                .thenReturn(expectedPage);

        // Act
        Page<Image> result = imageService.searchImages(QUERY, pageable);

        // Assert
        assertNotNull(result);
        assertEquals(1, result.getContent().size());
        assertEquals(IMAGE_ID, result.getContent().get(0).getId());
    }

    @Test
    void getUserFavorites_Success() {
        // Arrange
        Pageable pageable = PageRequest.of(0, 10);
        Image image = new Image();
        image.setId(IMAGE_ID);
        Page<Image> expectedPage = new PageImpl<>(Arrays.asList(image));
        
        when(imageRepository.findUserFavorites(eq(USER_ID), eq(pageable)))
                .thenReturn(expectedPage);

        // Act
        Page<Image> result = imageService.getUserFavorites(USER_ID, pageable);

        // Assert
        assertNotNull(result);
        assertEquals(1, result.getContent().size());
        assertEquals(IMAGE_ID, result.getContent().get(0).getId());
    }

    @Test
    void getImageById_Success() {
        // Arrange
        Image image = new Image();
        image.setId(IMAGE_ID);
        
        when(imageRepository.findById(IMAGE_ID))
                .thenReturn(Optional.of(image));

        // Act
        Image result = imageService.getImageById(IMAGE_ID);

        // Assert
        assertNotNull(result);
        assertEquals(IMAGE_ID, result.getId());
    }

    @Test
    void getImageById_NotFound() {
        // Arrange
        when(imageRepository.findById(IMAGE_ID)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(RuntimeException.class, () -> {
            imageService.getImageById(IMAGE_ID);
        });
    }

    @Test
    void searchPixabayImages_Success() throws Exception {
        // Arrange
        String expectedUrl = String.format("%s?key=%s&q=%s&image_type=%s&orientation=%s&category=%s&per_page=%d&page=%d",
                API_URL, API_KEY, QUERY, "photo", "all", "nature", 20, 1);
        
        String mockJsonResponse = "{\"hits\":[{\"id\":123,\"pageURL\":\"test\",\"type\":\"photo\",\"tags\":\"test\",\"previewURL\":\"test\",\"previewWidth\":100,\"previewHeight\":100,\"webformatURL\":\"test\",\"webformatWidth\":800,\"webformatHeight\":600,\"largeImageURL\":\"test\",\"imageWidth\":1000,\"imageHeight\":800,\"imageSize\":12345,\"views\":100,\"downloads\":50,\"likes\":25,\"comments\":10,\"user_id\":456}]}";
        ResponseEntity<String> mockResponse = ResponseEntity.ok(mockJsonResponse);
        
        // Mock RestTemplate response
        when(restTemplate.getForEntity(eq(expectedUrl), eq(String.class)))
                .thenReturn(mockResponse);
        
        // Mock ObjectMapper
        JsonNode mockRoot = mock(JsonNode.class);
        JsonNode mockHits = mock(JsonNode.class);
        JsonNode mockHit = mock(JsonNode.class);
        
        when(objectMapper.readTree(mockJsonResponse)).thenReturn(mockRoot);
        when(mockRoot.get("hits")).thenReturn(mockHits);
        when(mockHits.isArray()).thenReturn(true);
        when(mockHits.iterator()).thenReturn(Arrays.asList(mockHit).iterator());
        
        // Mock hit node values
        when(mockHit.get(anyString())).thenAnswer(invocation -> {
            String field = invocation.getArgument(0);
            JsonNode mockValue = mock(JsonNode.class);
            switch (field) {
                case "id": when(mockValue.asLong()).thenReturn(123L); break;
                case "pageURL": when(mockValue.asText()).thenReturn("test"); break;
                case "type": when(mockValue.asText()).thenReturn("photo"); break;
                case "tags": when(mockValue.asText()).thenReturn("test"); break;
                case "previewURL": when(mockValue.asText()).thenReturn("test"); break;
                case "previewWidth": when(mockValue.asInt()).thenReturn(100); break;
                case "previewHeight": when(mockValue.asInt()).thenReturn(100); break;
                case "webformatURL": when(mockValue.asText()).thenReturn("test"); break;
                case "webformatWidth": when(mockValue.asInt()).thenReturn(800); break;
                case "webformatHeight": when(mockValue.asInt()).thenReturn(600); break;
                case "largeImageURL": when(mockValue.asText()).thenReturn("test"); break;
                case "imageURL": return null;
                case "imageWidth": when(mockValue.asInt()).thenReturn(1000); break;
                case "imageHeight": when(mockValue.asInt()).thenReturn(800); break;
                case "imageSize": when(mockValue.asLong()).thenReturn(12345L); break;
                case "views": when(mockValue.asInt()).thenReturn(100); break;
                case "downloads": when(mockValue.asInt()).thenReturn(50); break;
                case "likes": when(mockValue.asInt()).thenReturn(25); break;
                case "comments": when(mockValue.asInt()).thenReturn(10); break;
                case "user_id": when(mockValue.asLong()).thenReturn(456L); break;
            }
            return mockValue;
        });
        
        // Mock image repository
        Image savedImage = new Image();
        savedImage.setId(123L);
        when(imageRepository.save(any(Image.class))).thenReturn(savedImage);

        // Act
        List<Image> result = imageService.searchPixabayImages(
                QUERY, "photo", "all", "nature", 20, 1);

        // Assert
        assertNotNull(result);
        assertFalse(result.isEmpty());
        assertEquals(1, result.size());
        assertEquals(123L, result.get(0).getId());
        verify(restTemplate).getForEntity(eq(expectedUrl), eq(String.class));
        verify(imageRepository).save(any(Image.class));
    }
}
