package com.galerija.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.galerija.entity.Image;
import com.galerija.entity.UserEntity;
import com.galerija.repository.ImageRepository;
import com.galerija.exception.ResourceNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.galerija.security.SecurityUtils;

@Service
public class ImageService {
    @Autowired
    private ImageRepository imageRepository;

    @Autowired
    private RestTemplate restTemplate;

    @Autowired
    private SecurityUtils securityUtils;

    @Value("${pixabay.api.key}")
    private String apiKey;

    @Value("${pixabay.api.url}")
    private String apiUrl;

    private static final Logger logger = LoggerFactory.getLogger(ImageService.class);

    @Transactional(readOnly = true)
    public Page<Image> searchImages(String query, Pageable pageable) {
        return imageRepository.searchImages(query, pageable);
    }

    @Transactional(readOnly = true)
    public Page<Image> getUserFavorites(Long userId, Pageable pageable) {
        if (userId == null) {
            UserEntity currentUser = securityUtils.getCurrentUser();
            if (currentUser == null) {
                throw new ResourceNotFoundException("User not found or not authenticated");
            }
            userId = currentUser.getId();
        }
        return imageRepository.findUserFavorites(userId, pageable);
    }

    @Transactional(readOnly = true)
    public Image getImageById(Long id) {
        return imageRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Image not found with id: " + id));
    }

    @Transactional
    public Image saveImage(Image image) {
        return imageRepository.save(image);
    }

    @Transactional
    public List<Image> searchPixabayImages(String query, String imageType, 
            String orientation, String category, Integer perPage, Integer page) {
        List<Image> images = new ArrayList<>();
        try {
            UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(apiUrl)
                    .queryParam("key", apiKey)
                    .queryParam("q", query)
                    .queryParam("image_type", imageType)
                    .queryParam("orientation", orientation)
                    .queryParam("per_page", perPage)
                    .queryParam("page", page);
            
            if (category != null && !category.equals("all")) {
                builder.queryParam("category", category);
            }
            
            String url = builder.build().toUriString();
            logger.info("Calling Pixabay API with URL: {}", url.replace(apiKey, "API_KEY"));
            
            try {
                ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);
                logger.info("Pixabay API Response Status: {}", response.getStatusCode());
                logger.info("Pixabay API Response Headers: {}", response.getHeaders());
                
                if (!response.getStatusCode().is2xxSuccessful()) {
                    logger.error("Pixabay API returned error status: {}", response.getStatusCode());
                    throw new ResourceNotFoundException("Failed to get images from Pixabay: " + response.getStatusCode());
                }
                
                if (response.getBody() == null) {
                    logger.error("Pixabay API returned empty response");
                    throw new ResourceNotFoundException("Empty response from Pixabay API");
                }
                
                logger.info("Pixabay API Response Body: {}", response.getBody());
                
                ObjectMapper mapper = new ObjectMapper();
                JsonNode root = mapper.readTree(response.getBody());
                
                if (root.has("error")) {
                    String error = root.get("error").asText();
                    logger.error("Pixabay API returned error: {}", error);
                    throw new ResourceNotFoundException("Pixabay API error: " + error);
                }
                
                JsonNode hits = root.get("hits");
                if (hits == null || !hits.isArray()) {
                    logger.error("Invalid response format from Pixabay API");
                    throw new ResourceNotFoundException("Invalid response format from Pixabay API");
                }
                
                for (JsonNode hit : hits) {
                    try {
                        Image image = new Image();
                        image.setId(hit.get("id").asLong());
                        image.setPageURL(getTextSafely(hit, "pageURL"));
                        image.setType(getTextSafely(hit, "type"));
                        image.setTags(getTextSafely(hit, "tags"));
                        image.setPreviewURL(getTextSafely(hit, "previewURL"));
                        image.setPreviewWidth(getIntSafely(hit, "previewWidth"));
                        image.setPreviewHeight(getIntSafely(hit, "previewHeight"));
                        image.setWebformatURL(getTextSafely(hit, "webformatURL"));
                        image.setWebformatWidth(getIntSafely(hit, "webformatWidth"));
                        image.setWebformatHeight(getIntSafely(hit, "webformatHeight"));
                        image.setLargeImageURL(getTextSafely(hit, "largeImageURL"));
                        image.setImageWidth(getIntSafely(hit, "imageWidth"));
                        image.setImageHeight(getIntSafely(hit, "imageHeight"));
                        image.setImageSize(getLongSafely(hit, "imageSize"));
                        image.setViews(getIntSafely(hit, "views"));
                        image.setDownloads(getIntSafely(hit, "downloads"));
                        image.setLikes(getIntSafely(hit, "likes"));
                        image.setComments(getIntSafely(hit, "comments"));
                        image.setUserId(getTextSafely(hit, "user_id"));
                        
                        images.add(image);
                        
                    } catch (Exception e) {
                        logger.error("Error processing image from Pixabay: {}", e.getMessage(), e);
                    }
                }
            } catch (Exception e) {
                logger.error("Error making request to Pixabay API: {}", e.getMessage(), e);
                throw new ResourceNotFoundException("Failed to make request to Pixabay API: " + e.getMessage(), e);
            }
        } catch (Exception e) {
            logger.error("Error searching Pixabay images: {}", e.getMessage(), e);
            throw new ResourceNotFoundException("Failed to search images: " + e.getMessage(), e);
        }
        
        return images;
    }
    
    private String getTextSafely(JsonNode node, String fieldName) {
        JsonNode field = node.get(fieldName);
        return field != null && !field.isNull() ? field.asText() : null;
    }
    
    private Integer getIntSafely(JsonNode node, String fieldName) {
        JsonNode field = node.get(fieldName);
        return field != null && !field.isNull() ? field.asInt() : 0;
    }
    
    private Long getLongSafely(JsonNode node, String fieldName) {
        JsonNode field = node.get(fieldName);
        return field != null && !field.isNull() ? field.asLong() : 0L;
    }

    @Transactional(readOnly = true)
    public int getTotalSearchResults(String query, String imageType, String orientation, String category) {
        UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(apiUrl)
                .queryParam("key", apiKey)
                .queryParam("q", query)
                .queryParam("image_type", imageType)
                .queryParam("orientation", orientation)
                .queryParam("per_page", 1)  // We only need the total count, so minimize data transfer
                .queryParam("page", 1);
        
        if (category != null) {
            builder.queryParam("category", category);
        }
        
        String url = builder.build().toUriString();
        ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);
        
        if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
            try {
                JsonNode root = new ObjectMapper().readTree(response.getBody());
                JsonNode totalHits = root.get("totalHits");
                if (totalHits != null) {
                    return totalHits.asInt();
                }
            } catch (Exception e) {
                logger.error("Failed to get total search results: {}", e.getMessage(), e);
            }
        }
        
        return 0;  // Return 0 if no results or error
    }

    @Transactional(readOnly = true)
    public List<Image> getImagesByIds(List<Long> ids) {
        return imageRepository.findByIdIn(ids);
    }

  

    @Transactional(readOnly = true)
    public Optional<Image> findByWebformatURL(String webformatURL) {
        return imageRepository.findByWebformatURL(webformatURL);
    }

    @Transactional
    public Image saveExternalImage(String webformatURL, String tags, String userId) {
        logger.info("Trying to save external image with URL: {}", webformatURL);
        // Check if image already exists
        Optional<Image> existingImage = findByWebformatURL(webformatURL);
        if (existingImage.isPresent()) {
            logger.debug("Image already exists with URL: {}", webformatURL);
            return existingImage.get();
        }

        // Create new image
        Image image = new Image();
        image.setWebformatURL(webformatURL);
        image.setPreviewURL(webformatURL); // Use webformatURL as previewURL
        image.setTags(tags);
        image.setUserId(userId);
        image.setPageURL(webformatURL); // Use webformatURL as pageURL if not provided
        image.setType("photo"); // Default type
        image.setLikes(0);
        image.setViews(0);
        image.setDownloads(0);
        image.setComments(0);

        logger.info("Saving new external image to database");
        return imageRepository.save(image);
    }
    
    @Transactional
    public Image likeImage(Long id) {
        Image image = imageRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Image not found with id: " + id));
        image.setLikes(image.getLikes() + 1);
        return imageRepository.save(image);
    }
}
