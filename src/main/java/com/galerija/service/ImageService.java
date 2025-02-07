package com.galerija.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.galerija.entity.Image;
import com.galerija.repository.ImageRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.List;

@Service
public class ImageService {
    @Autowired
    private ImageRepository imageRepository;

    @Autowired
    private ObjectMapper objectMapper;

    @Value("${pixabay.api.key}")
    private String apiKey;

    @Value("${pixabay.api.url}")
    private String apiUrl;

    @Autowired
    private RestTemplate restTemplate;

    @Transactional(readOnly = true)
    public Page<Image> searchImages(String query, Pageable pageable) {
        return imageRepository.searchImages(query, pageable);
    }

    @Transactional(readOnly = true)
    public Page<Image> getUserFavorites(Long userId, Pageable pageable) {
        return imageRepository.findUserFavorites(userId, pageable);
    }

    @Transactional(readOnly = true)
    public Image getImageById(Long id) {
        return imageRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Image not found with id: " + id));
    }

    @Transactional
    public Image saveImage(Image image) {
        return imageRepository.save(image);
    }

    @Transactional
    public List<Image> searchPixabayImages(String query, String imageType, 
            String orientation, String category, Integer perPage, Integer page) {
        String url = String.format("%s?key=%s&q=%s&image_type=%s&orientation=%s&category=%s&per_page=%d&page=%d",
                apiUrl, apiKey, query, imageType, orientation, category, perPage, page);
        
        ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);
        List<Image> images = new ArrayList<>();
        
        if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
            try {
                JsonNode root = objectMapper.readTree(response.getBody());
                JsonNode hits = root.get("hits");
                
                if (hits != null && hits.isArray()) {
                    for (JsonNode hit : hits) {
                        Image image = new Image();
                        image.setId(hit.get("id").asLong());
                        image.setPageURL(hit.get("pageURL").asText());
                        image.setType(hit.get("type").asText());
                        image.setTags(hit.get("tags").asText());
                        image.setPreviewURL(hit.get("previewURL").asText());
                        image.setPreviewWidth(hit.get("previewWidth").asInt());
                        image.setPreviewHeight(hit.get("previewHeight").asInt());
                        image.setWebformatURL(hit.get("webformatURL").asText());
                        image.setWebformatWidth(hit.get("webformatWidth").asInt());
                        image.setWebformatHeight(hit.get("webformatHeight").asInt());
                        image.setLargeImageURL(hit.get("largeImageURL").asText());
                        image.setImageURL(hit.get("imageURL") != null ? hit.get("imageURL").asText() : null);
                        image.setImageWidth(hit.get("imageWidth").asInt());
                        image.setImageHeight(hit.get("imageHeight").asInt());
                        image.setImageSize(hit.get("imageSize").asLong());
                        image.setViews(hit.get("views").asInt());
                        image.setDownloads(hit.get("downloads").asInt());
                        image.setLikes(hit.get("likes").asInt());
                        image.setComments(hit.get("comments").asInt());
                        image.setUserId(hit.get("user_id").asLong());
                        
                        // Save or update the image
                        image = imageRepository.save(image);
                        images.add(image);
                    }
                }
            } catch (Exception e) {
                throw new RuntimeException("Failed to process Pixabay response: " + e.getMessage());
            }
        }
        
        return images;
    }

    @Transactional(readOnly = true)
    public List<Image> getImagesByIds(List<Long> ids) {
        return imageRepository.findByIdIn(ids);
    }
}
