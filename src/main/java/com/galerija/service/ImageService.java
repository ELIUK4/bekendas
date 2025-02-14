package com.galerija.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.galerija.entity.Image;
import com.galerija.entity.ImageDto;
import com.galerija.entity.UserEntity;
import com.galerija.repository.ImageRepository;
import com.galerija.repository.UserRepository;
import com.galerija.exception.ResourceNotFoundException;
import com.galerija.security.SecurityUtils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.util.StringUtils;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class ImageService {

    @Autowired
    private ImageRepository imageRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private SecurityUtils securityUtils;

    @Autowired
    private RestTemplate restTemplate;

    @Value("${pixabay.api.key}")
    private String apiKey;

    @Value("${pixabay.api.url}")
    private String apiUrl;

    @Value("${app.upload.dir:${user.home}/uploads}")
    private String uploadDir;

    private static final Logger logger = LoggerFactory.getLogger(ImageService.class);

    private final ObjectMapper objectMapper = new ObjectMapper();

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
                
                JsonNode root = objectMapper.readTree(response.getBody());
                
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
                        Image image = mapToImage(hit);
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
    
    private Image mapToImage(JsonNode hit) {
        Image image = new Image();
        image.setWebformatURL(hit.get("webformatURL").asText());
        image.setPageURL(hit.get("pageURL").asText());
        image.setType("photo");
        image.setTags(hit.get("tags").asText());
        image.setPreviewURL(hit.get("previewURL").asText());
        image.setLargeImageURL(hit.get("largeImageURL").asText());
        
        // Konvertuoti userId į Long
        JsonNode userIdNode = hit.get("user_id");
        if (userIdNode != null) {
            image.setUserId(userIdNode.asLong());
        } else {
            image.setUserId(0L);
        }

        if (hit.has("fullHDURL")) {
            image.setFullHDURL(hit.get("fullHDURL").asText());
        }
        if (hit.has("imageURL")) {
            image.setImageURL(hit.get("imageURL").asText());
        }
        
        image.setImageWidth(hit.get("imageWidth").asInt());
        image.setImageHeight(hit.get("imageHeight").asInt());
        image.setImageSize(hit.get("imageSize").asLong());
        image.setViews(hit.get("views").asInt());
        image.setDownloads(hit.get("downloads").asInt());
        image.setLikes(hit.get("likes").asInt());
        image.setWebformatWidth(hit.get("webformatWidth").asInt());
        image.setWebformatHeight(hit.get("webformatHeight").asInt());
        image.setPreviewWidth(hit.get("previewWidth").asInt());
        image.setPreviewHeight(hit.get("previewHeight").asInt());
        
        return image;
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
                JsonNode root = objectMapper.readTree(response.getBody());
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
    public Image saveExternalImage(Image externalImage) {
        logger.info("Saving external image: {}", externalImage);
        
        // Validate required fields
        if (externalImage.getWebformatURL() == null && externalImage.getLargeImageURL() != null) {
            externalImage.setWebformatURL(externalImage.getLargeImageURL());
        }
        
        if (externalImage.getWebformatURL() == null) {
            throw new IllegalArgumentException("webformatURL cannot be null");
        }
        
        // Check if image already exists
        Optional<Image> existingImage = imageRepository.findByWebformatURL(externalImage.getWebformatURL());
        if (existingImage.isPresent()) {
            logger.info("Image already exists with ID: {}", existingImage.get().getId());
            return existingImage.get();
        }

        // Set default values if not provided
        if (externalImage.getType() == null) {
            externalImage.setType("photo");
        }
        if (externalImage.getTags() == null) {
            externalImage.setTags("");
        }
        if (externalImage.getPreviewURL() == null) {
            externalImage.setPreviewURL(externalImage.getWebformatURL());
        }
        if (externalImage.getLargeImageURL() == null) {
            externalImage.setLargeImageURL(externalImage.getWebformatURL());
        }
        
        // Save image
        Image savedImage = imageRepository.save(externalImage);
        logger.info("Saved external image with ID: {}", savedImage.getId());
        return savedImage;
    }
    
    @Transactional
    public Image likeImage(Long id) {
        Image image = imageRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Image not found with id: " + id));
        image.setLikes(image.getLikes() + 1);
        return imageRepository.save(image);
    }

    @Transactional(readOnly = true)
    public List<Image> getUserImages() {
        UserEntity currentUser = securityUtils.getCurrentUser();
        if (currentUser == null) {
            throw new ResourceNotFoundException("User not found or not authenticated");
        }
        return imageRepository.findByUserId(currentUser.getId());
    }

    @Transactional
    public Image saveUploadedImage(MultipartFile file, String username) {
        logger.info("Starting to save uploaded image for user: {}", username);
        
        try {
            // Gauti vartotoją
            logger.debug("Looking up user: {}", username);
            UserEntity user = userRepository.findByUsername(username)
                .orElseThrow(() -> {
                    logger.error("User not found: {}", username);
                    return new ResourceNotFoundException("User not found: " + username);
                });
            logger.debug("Found user with ID: {}", user.getId());
            
            // Sukurti uploads direktoriją, jei jos nėra
            Path uploadPath = Paths.get(uploadDir);
            if (!Files.exists(uploadPath)) {
                Files.createDirectories(uploadPath);
            }
            
            // Sugeneruoti unikalų failo pavadinimą
            String originalFileName = StringUtils.cleanPath(file.getOriginalFilename());
            String fileExtension = originalFileName.substring(originalFileName.lastIndexOf("."));
            String fileName = UUID.randomUUID().toString() + fileExtension;
            
            // Išsaugoti failą
            Path targetLocation = uploadPath.resolve(fileName);
            Files.copy(file.getInputStream(), targetLocation, StandardCopyOption.REPLACE_EXISTING);
            
            // Sukurti ir išsaugoti Image įrašą
            Image image = new Image();
            image.setUserId(user.getId());
            image.setUploadDate(LocalDateTime.now());
            image.setType("photo");
            image.setFileName(fileName);
            image.setOriginalFileName(originalFileName);
            image.setWebformatURL("http://localhost:8080/api/images/uploads/" + fileName);
            image.setLargeImageURL("http://localhost:8080/api/images/uploads/" + fileName);
            
            Image savedImage = imageRepository.save(image);
            logger.info("Image record saved successfully with ID: {}", savedImage.getId());
            
            return savedImage;
        } catch (Exception e) {
            logger.error("Unexpected error while saving image: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to process image upload: " + e.getMessage(), e);
        }
    }

    @Transactional(readOnly = true)
    public ImageDto toDto(Image image) {
        ImageDto dto = new ImageDto();
        dto.setId(image.getId());
        dto.setFileName(image.getFileName());
        dto.setOriginalFileName(image.getOriginalFileName());
        dto.setType(image.getType());
        dto.setWebformatURL(image.getWebformatURL());
        dto.setLargeImageURL(image.getLargeImageURL());
        dto.setUserId(image.getUserId());
        dto.setUploadDate(image.getUploadDate());
        return dto;
    }
}
