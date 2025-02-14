package com.galerija.service;

import com.galerija.entity.Image;
import com.galerija.entity.ImageDto;
import com.galerija.entity.UserEntity;
import com.galerija.exception.ResourceNotFoundException;
import com.galerija.repository.ImageRepository;
import com.galerija.repository.UserRepository;
import com.galerija.security.SecurityUtils;

import jakarta.annotation.PostConstruct;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class ImageService {

    private static final Logger logger = LoggerFactory.getLogger(ImageService.class);

    private final ImageRepository imageRepository;
    private final UserRepository userRepository;
    private final SecurityUtils securityUtils;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    private final String apiUrl;
    private final String apiKey;

    @Value("${app.upload.dir:${user.home}/uploads}")
    private String uploadDir;

    @Autowired
    public ImageService(
            ImageRepository imageRepository,
            UserRepository userRepository,
            SecurityUtils securityUtils,
            RestTemplate restTemplate,
            ObjectMapper objectMapper,
            @Value("${pixabay.api.url}") String apiUrl,
            @Value("${pixabay.api.key}") String apiKey) {
        this.imageRepository = imageRepository;
        this.userRepository = userRepository;
        this.securityUtils = securityUtils;
        this.restTemplate = restTemplate;
        this.objectMapper = objectMapper;
        this.apiUrl = apiUrl;
        this.apiKey = apiKey;
    }

    @PostConstruct
    public void init() {
        try {
            Files.createDirectories(Paths.get(uploadDir));
        } catch (IOException e) {
            throw new RuntimeException("Could not create upload directory!", e);
        }
    }

    @Transactional(readOnly = true)
    public Image getImageById(Long id) {
        return imageRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Image not found"));
    }

    @Transactional
    public Image saveImage(Image image) {
        return imageRepository.save(image);
    }

    @Transactional
    public Image saveExternalImage(Image image) {
        Optional<Image> existingImage = imageRepository.findByWebformatURL(image.getWebformatURL());
        return existingImage.orElseGet(() -> imageRepository.save(image));
    }

    @Transactional
    public Image likeImage(Long id) {
        Image image = getImageById(id);
        image.setLikes(image.getLikes() + 1);
        return imageRepository.save(image);
    }

    @Transactional
    public Image saveUploadedImage(MultipartFile file) {
        if (file.isEmpty()) {
            throw new IllegalArgumentException("Failed to store empty file");
        }

        try {
            String fileName = UUID.randomUUID().toString() + "_" + file.getOriginalFilename();
            Path targetLocation = Paths.get(uploadDir).resolve(fileName);
            Files.copy(file.getInputStream(), targetLocation, StandardCopyOption.REPLACE_EXISTING);

            Image image = new Image();
            image.setFileName(fileName);
            image.setOriginalFileName(file.getOriginalFilename());
            image.setType("photo");
            image.setWebformatURL("http://localhost:8080/api/images/uploads/" + fileName);
            image.setLargeImageURL("http://localhost:8080/api/images/uploads/" + fileName);
            image.setPreviewURL("http://localhost:8080/api/images/uploads/" + fileName);
            image.setUploadDate(LocalDateTime.now());
            
            UserEntity currentUser = securityUtils.getCurrentUser();
            if (currentUser != null) {
                image.setUserId(currentUser.getId());
            }

            return imageRepository.save(image);
        } catch (IOException e) {
            throw new RuntimeException("Failed to store file", e);
        }
    }

    @Transactional
    public void deleteImage(Long id) {
        Image image = getImageById(id);
        UserEntity currentUser = securityUtils.getCurrentUser();
        
        if (currentUser != null && !image.getUserId().equals(currentUser.getId())) {
            throw new ResourceNotFoundException("You don't have permission to delete this image");
        }

        try {
            Path filePath = Paths.get(uploadDir).resolve(image.getFileName());
            Files.deleteIfExists(filePath);
            imageRepository.delete(image);
        } catch (IOException e) {
            throw new RuntimeException("Failed to delete image file", e);
        }
    }

    @Transactional(readOnly = true)
    public int getTotalSearchResults(String query, String imageType, String orientation, String category) {
        try {
            UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(apiUrl)
                    .queryParam("key", apiKey)
                    .queryParam("q", query)
                    .queryParam("image_type", imageType)
                    .queryParam("orientation", orientation);
            
            if (category != null && !category.equals("all")) {
                builder.queryParam("category", category);
            }
            
            String url = builder.build().toUriString();
            logger.info("Calling Pixabay API with URL: {}", url.replace(apiKey, "API_KEY"));
            
            ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);
            if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
                logger.error("Pixabay API returned error status: {}", response.getStatusCode());
                return 0;
            }
            
            JsonNode root = objectMapper.readTree(response.getBody());
            if (root.has("error")) {
                String error = root.get("error").asText();
                logger.error("Pixabay API returned error: {}", error);
                return 0;
            }
            
            if (!root.has("totalHits")) {
                logger.error("Invalid response format from Pixabay API");
                return 0;
            }
            
            return root.get("totalHits").asInt();
        } catch (Exception e) {
            logger.error("Error getting total search results: {}", e.getMessage(), e);
            return 0;
        }
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
            
            ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);
            if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
                logger.error("Pixabay API returned error status: {}", response.getStatusCode());
                return images;
            }
            
            JsonNode root = objectMapper.readTree(response.getBody());
            if (root.has("error")) {
                String error = root.get("error").asText();
                logger.error("Pixabay API returned error: {}", error);
                return images;
            }
            
            JsonNode hits = root.get("hits");
            if (hits == null || !hits.isArray()) {
                logger.error("Invalid response format from Pixabay API");
                return images;
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
            logger.error("Error searching Pixabay images: {}", e.getMessage(), e);
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

    @Transactional(readOnly = true)
    public List<Image> getUserImages() {
        UserEntity currentUser = securityUtils.getCurrentUser();
        if (currentUser == null) {
            throw new ResourceNotFoundException("User not found");
        }
        return imageRepository.findByUserId(currentUser.getId());
    }
}
