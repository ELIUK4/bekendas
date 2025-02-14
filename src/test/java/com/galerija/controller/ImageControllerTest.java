package com.galerija.controller;

import com.galerija.entity.Image;
import com.galerija.service.ImageService;
import com.galerija.service.SearchHistoryService;
import com.galerija.exception.ResourceNotFoundException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import java.util.Collections;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.hamcrest.Matchers.containsString;

class ImageControllerTest {

    private MockMvc mockMvc;
    private ObjectMapper objectMapper = new ObjectMapper();

    @Mock
    private ImageService imageService;

    @Mock
    private SearchHistoryService searchHistoryService;

    @InjectMocks
    private ImageController imageController;

    private Image createTestImage(Long id, String webformatURL, String tags, Long userId, int likes) {
        Image image = new Image();
        image.setId(id);
        image.setWebformatURL(webformatURL);
        image.setTags(tags);
        image.setUserId(userId);
        image.setLikes(likes);
        image.setViews(0);
        image.setDownloads(0);
        image.setComments(0);
        image.setType("photo");
        image.setPreviewURL(webformatURL);
        image.setLargeImageURL(webformatURL);
        return image;
    }

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        mockMvc = MockMvcBuilders
            .standaloneSetup(imageController)
            .setControllerAdvice(new ResponseEntityExceptionHandler() {})
            .build();
    }

    @Test
    void testSearchImages() throws Exception {
        String query = "gamta";
        Image testImage = createTestImage(1L, "url", "tags", 1L, 0);
        List<Image> images = Collections.singletonList(testImage);
        
        when(imageService.searchPixabayImages(anyString(), anyString(), anyString(), any(), anyInt(), anyInt()))
            .thenReturn(images);

        mockMvc.perform(get("/api/images/search")
                .param("query", query)
                .param("imageType", "photo")
                .param("orientation", "all")
                .param("perPage", "10")
                .param("page", "1")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data[0].id").value(1))
                .andExpect(jsonPath("$.total").value(1));

        verify(searchHistoryService).saveSearch(
            eq(query),
            eq("type=photo, orientation=all"),
            eq(10)
        );
    }

    @Test
    void testGetImageById_Success() throws Exception {
        Image image = createTestImage(1L, "url", "tags", 1L, 10);
        when(imageService.getImageById(1L)).thenReturn(image);

        mockMvc.perform(get("/api/images/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.webformatURL").value("url"));
    }

    @Test
    void testGetImageById_NotFound() throws Exception {
        when(imageService.getImageById(1L)).thenThrow(new ResourceNotFoundException("Image not found"));

        mockMvc.perform(get("/api/images/1"))
                .andExpect(status().isNotFound());
    }

    @Test
    void testSaveImage() throws Exception {
        Image image = createTestImage(1L, "url", "tags", 1L, 0);
        when(imageService.saveImage(any(Image.class))).thenReturn(image);

        mockMvc.perform(post("/api/images")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"id\":1,\"webformatURL\":\"url\",\"tags\":\"tags\",\"userId\":1,\"likes\":0}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.webformatURL").value("url"));
    }

    @Test
    void testSearchImages_KaiNeraCategorijos() throws Exception {
        String query = "gamta";
        Image testImage = createTestImage(1L, "url", "tags", 1L, 0);
        List<Image> images = Collections.singletonList(testImage);
        
        when(imageService.searchPixabayImages(anyString(), anyString(), anyString(), isNull(), anyInt(), anyInt()))
            .thenReturn(images);

        mockMvc.perform(get("/api/images/search")
                .param("query", query)
                .param("imageType", "photo")
                .param("orientation", "all")
                .param("perPage", "10")
                .param("page", "1")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data[0].id").value(1))
                .andExpect(jsonPath("$.total").value(1));

        verify(searchHistoryService).saveSearch(
            eq(query), 
            eq("type=photo, orientation=all"), 
            eq(10)
        );
    }

    @Test
    void testSearchImages_KaiYraKlaida() throws Exception {
        when(imageService.searchPixabayImages(anyString(), anyString(), anyString(), any(), anyInt(), anyInt()))
            .thenThrow(new ResourceNotFoundException("API klaida"));

        mockMvc.perform(get("/api/images/search")
                .param("query", "gamta")
                .param("imageType", "photo")
                .param("orientation", "all")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isInternalServerError());

        verify(searchHistoryService, never()).saveSearch(any(), any(), any());
    }

    @Test
    void testSearchImages_SuKategorija() throws Exception {
        String query = "gamta";
        String category = "animals";
        Image testImage = createTestImage(1L, "url", "tags", 1L, 0);
        List<Image> images = Collections.singletonList(testImage);
        
        when(imageService.searchPixabayImages(anyString(), anyString(), anyString(), eq(category), anyInt(), anyInt()))
            .thenReturn(images);

        mockMvc.perform(get("/api/images/search")
                .param("query", query)
                .param("imageType", "photo")
                .param("orientation", "all")
                .param("category", category)
                .param("perPage", "10")
                .param("page", "1")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data[0].id").value(1))
                .andExpect(jsonPath("$.total").value(1));

        verify(searchHistoryService).saveSearch(
            eq(query), 
            eq("type=photo, orientation=all, category=animals"), 
            eq(10)
        );
    }

    @Test
    void testLikeImage_Success() throws Exception {
        Image image = createTestImage(1L, "url", "tags", 1L, 1);
        when(imageService.likeImage(1L)).thenReturn(image);

        mockMvc.perform(post("/api/images/1/like"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.likes").value(1));
    }

    @Test
    void testLikeImage_NotFound() throws Exception {
        when(imageService.likeImage(1L)).thenThrow(new ResourceNotFoundException("Image not found"));

        mockMvc.perform(post("/api/images/1/like"))
                .andExpect(status().isNotFound());
    }

    @Test
    void testSaveExternalImage_Success() throws Exception {
        // Given
        Image image = new Image();
        image.setWebformatURL("http://example.com/image.jpg");
        image.setTags("test tags");
        image.setUserId(1L);

        Image savedImage = new Image();
        savedImage.setId(1L);
        savedImage.setWebformatURL("http://example.com/image.jpg");
        savedImage.setTags("test tags");
        savedImage.setUserId(1L);

        when(imageService.saveExternalImage(any(Image.class))).thenReturn(savedImage);

        // When & Then
        mockMvc.perform(post("/api/images/external")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(image)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.webformatURL").value("http://example.com/image.jpg"))
                .andExpect(jsonPath("$.tags").value("test tags"))
                .andExpect(jsonPath("$.userId").value(1));

        verify(imageService).saveExternalImage(any(Image.class));
    }

    @Test
    void testSaveExternalImage_Error() throws Exception {
        // Given
        Image image = new Image();
        image.setWebformatURL("http://example.com/image.jpg");
        image.setTags("test tags");
        image.setUserId(1L);

        when(imageService.saveExternalImage(any(Image.class)))
                .thenThrow(new RuntimeException("Failed to save image"));

        // When & Then
        mockMvc.perform(post("/api/images/external")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(image)))
                .andExpect(status().isBadRequest())
                .andExpect(content().string(containsString("Failed to save image")));

        verify(imageService).saveExternalImage(any(Image.class));
    }
}
