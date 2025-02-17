package com.galerija.service;

import com.galerija.entity.Image;
import com.galerija.entity.ImageDto;
import com.galerija.entity.ImagePrivacy;
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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;
import org.springframework.web.multipart.MultipartFile;

import java.awt.image.BufferedImage;
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
import java.util.stream.Collectors;

import javax.imageio.ImageIO;

import java.util.Date;

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
            Path uploadPath = Paths.get(uploadDir);
            if (!Files.exists(uploadPath)) {
                Files.createDirectories(uploadPath);
                logger.info("Created upload directory: {}", uploadPath);
            }
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
    public Image saveExternalImage(Image externalImage) {
        logger.info("Saving external image: {}", externalImage.getWebformatURL());
        
        if (externalImage == null) {
            logger.error("External image cannot be null");
            throw new IllegalArgumentException("External image cannot be null");
        }

        UserEntity currentUser = securityUtils.getCurrentUserEntity();
        if (currentUser == null) {
            logger.error("User not found when saving external image");
            throw new ResourceNotFoundException("User not found");
        }
        logger.info("User found: {}", currentUser.getUsername());

        // Check if image already exists by webformatURL
        Optional<Image> existingImage = imageRepository.findByWebformatURL(externalImage.getWebformatURL());
        if (existingImage.isPresent()) {
            logger.info("Image already exists with webformatURL: {}", externalImage.getWebformatURL());
            Image image = existingImage.get();
            // Update views
            image.setViews(image.getViews() + 1);
            return imageRepository.save(image);
        }

        // Set required fields
        externalImage.setUser(currentUser);
        externalImage.setPrivacy(ImagePrivacy.PRIVATE);
        externalImage.setType("photo");
        externalImage.setUploadDate(LocalDateTime.now());
        externalImage.setViews(1);
        externalImage.setDownloads(0);
        externalImage.setLikes(0);
        externalImage.setComments(0);
        
        // Generate a filename if not present
        if (externalImage.getFileName() == null) {
            String fileName = UUID.randomUUID().toString() + ".jpg";
            externalImage.setFileName(fileName);
            externalImage.setOriginalFileName(fileName);
        }
        
        // Ensure all URLs are set
        if (externalImage.getPreviewURL() == null) {
            externalImage.setPreviewURL(externalImage.getWebformatURL());
        }
        if (externalImage.getLargeImageURL() == null) {
            externalImage.setLargeImageURL(externalImage.getWebformatURL());
        }
        if (externalImage.getImageURL() == null) {
            externalImage.setImageURL(externalImage.getWebformatURL());
        }
        
        // Save image
        try {
            Image savedImage = imageRepository.save(externalImage);
            logger.info("Saved external image with ID: {}", savedImage.getId());
            return savedImage;
        } catch (Exception e) {
            logger.error("Failed to save external image: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to save external image: " + e.getMessage(), e);
        }
    }

    @Transactional
    public Image likeImage(Long id) {
        Image image = getImageById(id);
        image.setLikes(image.getLikes() + 1);
        return imageRepository.save(image);
    }

    @Transactional
    public Image saveUploadedImage(MultipartFile file) {
        try {
            logger.info("Starting to save uploaded image: {}", file.getOriginalFilename());
            
            UserEntity currentUser = securityUtils.getCurrentUserEntity();
            if (currentUser == null) {
                logger.error("No authenticated user found");
                throw new ResourceNotFoundException("User not found");
            }
            logger.info("User found: {}", currentUser.getUsername());

            if (file == null || file.isEmpty()) {
                logger.error("No file selected or empty file");
                throw new IllegalArgumentException("Nepasirinktas failas");
            }

            String contentType = file.getContentType();
            if (contentType == null || !contentType.startsWith("image/")) {
                logger.error("Invalid file type: {}", contentType);
                throw new IllegalArgumentException("Netinkamas failo tipas: " + contentType);
            }

            // Generate unique filename
            String originalFilename = file.getOriginalFilename();
            String fileName = UUID.randomUUID().toString() + "_" + 
                (originalFilename != null ? originalFilename.replaceAll("[^a-zA-Z0-9.-]", "_") : "image.jpg");
            
            Path uploadPath = Paths.get(uploadDir);
            if (!Files.exists(uploadPath)) {
                logger.info("Creating upload directory: {}", uploadPath);
                Files.createDirectories(uploadPath);
            }

            Path filePath = uploadPath.resolve(fileName);
            logger.info("Saving file to: {}", filePath);
            
            // Check if file already exists
            if (Files.exists(filePath)) {
                logger.warn("File already exists, generating new name");
                fileName = UUID.randomUUID().toString() + "_" + fileName;
                filePath = uploadPath.resolve(fileName);
            }
            
            Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);
            logger.info("File saved successfully to disk");

            Image image = new Image();
            image.setFileName(fileName);
            image.setOriginalFileName(originalFilename);
            image.setType("photo");
            image.setUser(currentUser);
            image.setPrivacy(ImagePrivacy.PRIVATE);
            
            // Set URLs
            String baseUrl = "/api/uploads/";
            image.setWebformatURL(baseUrl + fileName);
            image.setLargeImageURL(baseUrl + fileName);
            image.setPreviewURL(baseUrl + fileName);
            image.setImageURL(baseUrl + fileName);
            
            // Set metadata
            image.setUploadDate(LocalDateTime.now());
            image.setViews(0);
            image.setDownloads(0);
            image.setLikes(0);
            image.setComments(0);
            
            // Set image dimensions if possible
            try {
                BufferedImage bufferedImage = ImageIO.read(file.getInputStream());
                if (bufferedImage != null) {
                    int width = bufferedImage.getWidth();
                    int height = bufferedImage.getHeight();
                    image.setImageWidth(width);
                    image.setImageHeight(height);
                    image.setWebformatWidth(width);
                    image.setWebformatHeight(height);
                    image.setPreviewWidth(width);
                    image.setPreviewHeight(height);
                }
            } catch (Exception e) {
                logger.warn("Could not read image dimensions: {}", e.getMessage());
            }

            // Set file size
            image.setImageSize(file.getSize());

            Image savedImage = imageRepository.save(image);
            logger.info("Successfully saved image to database with ID: {}", savedImage.getId());
            
            return savedImage;
        } catch (IOException e) {
            logger.error("Failed to save file: {}", e.getMessage(), e);
            throw new RuntimeException("Nepavyko išsaugoti failo: " + e.getMessage(), e);
        } catch (Exception e) {
            logger.error("Unexpected error while saving image: {}", e.getMessage(), e);
            throw new RuntimeException("Netikėta klaida išsaugant nuotrauką: " + e.getMessage(), e);
        }
    }

    @Transactional
    public void deleteImage(Long id) {
        logger.info("Attempting to delete image with ID: {}", id);
        
        Image image = getImageById(id);
        if (image == null) {
            logger.error("Image not found with ID: {}", id);
            throw new ResourceNotFoundException("Image not found");
        }

        UserEntity currentUser = securityUtils.getCurrentUserEntity();
        if (currentUser == null) {
            logger.error("No authenticated user found");
            throw new ResourceNotFoundException("User not found");
        }
        
        if (!image.getUser().getId().equals(currentUser.getId())) {
            logger.error("User {} does not have permission to delete image {}", currentUser.getId(), id);
            throw new ResourceNotFoundException("You don't have permission to delete this image");
        }

        try {
            // First delete from database
            imageRepository.delete(image);
            logger.info("Deleted image {} from database", id);

            // Then try to delete file
            if (image.getFileName() != null) {
                Path filePath = Paths.get(uploadDir).resolve(image.getFileName());
                if (Files.exists(filePath)) {
                    Files.delete(filePath);
                    logger.info("Deleted image file: {}", filePath);
                } else {
                    logger.warn("Image file not found: {}", filePath);
                }
            }
        } catch (IOException e) {
            logger.error("Failed to delete image file: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to delete image file: " + e.getMessage(), e);
        } catch (Exception e) {
            logger.error("Unexpected error while deleting image: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to delete image: " + e.getMessage(), e);
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
            UserEntity user = userRepository.findById(userIdNode.asLong()).orElse(null);
            image.setUser(user);
        } else {
            image.setUser(null);
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
    public ImageDto toDto(Image image) {
        if (image == null) return null;
        
        ImageDto dto = new ImageDto();
        dto.setId(image.getId());
        dto.setFileName(image.getFileName());
        dto.setOriginalFileName(image.getOriginalFileName());
        dto.setType(image.getType());
        dto.setWebformatURL(image.getWebformatURL());
        dto.setLargeImageURL(image.getLargeImageURL());
        dto.setPreviewURL(image.getPreviewURL());
        dto.setUploadDate(image.getUploadDate());
        dto.setPrivacy(image.getPrivacy());
        dto.setLikes(image.getLikes());
        dto.setDownloads(image.getDownloads());
        dto.setViews(image.getViews());
        dto.setUserId(image.getUser() != null ? image.getUser().getId() : null);
        dto.setTags(image.getTags());
        return dto;
    }

    @Transactional(readOnly = true)
    public List<ImageDto> getUserImages() {
        logger.info("Getting images for current user");
        UserEntity currentUser = securityUtils.getCurrentUserEntity();
        
        if (currentUser == null) {
            logger.error("No authenticated user found when getting user images");
            throw new ResourceNotFoundException("User not found");
        }
        
        logger.info("Found user: {}", currentUser.getUsername());
        List<Image> images = imageRepository.findByUser(currentUser);
        logger.info("Found {} images for user", images.size());
        
        List<ImageDto> imageDtos = images.stream()
            .map(this::toDto)
            .collect(Collectors.toList());
            
        logger.info("Converted {} images to DTOs", imageDtos.size());
        return imageDtos;
    }

    @Transactional(readOnly = true)
    public Page<ImageDto> getPublicImages(PageRequest pageRequest) {
        Page<Image> imagePage = imageRepository.findByPrivacy(ImagePrivacy.PUBLIC, pageRequest);
        return imagePage.map(this::toDto);
    }

    @Transactional
    public Image updateImagePrivacy(Long id, ImagePrivacy privacy) {
        Image image = imageRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Image not found"));

        UserEntity currentUser = securityUtils.getCurrentUserEntity();
        if (currentUser == null || !image.getUser().getId().equals(currentUser.getId())) {
            throw new RuntimeException("You don't have permission to update this image");
        }

        image.setPrivacy(privacy);
        return imageRepository.save(image);
    }
}
