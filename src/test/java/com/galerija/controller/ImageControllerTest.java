package com.galerija.controller;

import com.galerija.entity.Image;
import com.galerija.service.ImageService;
import com.galerija.service.SearchHistoryService;
import com.galerija.exception.ResourceNotFoundException;
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

class ImageControllerTest {

    private MockMvc mockMvc;

    @Mock
    private ImageService imageService;

    @Mock
    private SearchHistoryService searchHistoryService;

    @InjectMocks
    private ImageController imageController;

    private Image createTestImage(Long id, String webformatURL, String tags, String user, int likes) {
        Image image = new Image();
        image.setId(id);
        image.setWebformatURL(webformatURL);
        image.setTags(tags);
        image.setUserId(user);
        image.setLikes(likes);
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
        Image testImage = createTestImage(1L, "url", "tags", "user", 0);
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
        Image image = createTestImage(1L, "url", "tags", "user", 10);
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
        Image image = createTestImage(1L, "url", "tags", "user", 0);
        when(imageService.saveImage(any(Image.class))).thenReturn(image);

        mockMvc.perform(post("/api/images")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"id\":1,\"webformatURL\":\"url\",\"tags\":\"tags\",\"user\":\"user\",\"likes\":0}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.webformatURL").value("url"));
    }

    @Test
    void testSearchImages_KaiNeraCategorijos() throws Exception {
        String query = "gamta";
        Image testImage = createTestImage(1L, "url", "tags", "user", 0);
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
        Image testImage = createTestImage(1L, "url", "tags", "user", 0);
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
        Image image = createTestImage(1L, "url", "tags", "user", 1);
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
        Image image = createTestImage(1L, "webformatURL", "tags", "user", 0);
        when(imageService.saveExternalImage(anyString(), anyString(), anyString())).thenReturn(image);

        mockMvc.perform(post("/api/images/external")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"webformatURL\":\"webformatURL\",\"tags\":\"tags\",\"user\":\"user\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.webformatURL").value("webformatURL"));
    }
}
