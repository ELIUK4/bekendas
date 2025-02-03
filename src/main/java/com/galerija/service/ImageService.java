package com.galerija.service;

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

import java.util.List;

@Service
public class ImageService {
    @Autowired
    private ImageRepository imageRepository;

    @Value("${pixabay.api.key}")
    private String apiKey;

    @Value("${pixabay.api.url}")
    private String apiUrl;

    private final RestTemplate restTemplate = new RestTemplate();

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

    public ResponseEntity<String> searchPixabayImages(String query, String imageType, 
            String orientation, String category, Integer perPage, Integer page) {
        String url = String.format("%s?key=%s&q=%s&image_type=%s&orientation=%s&category=%s&per_page=%d&page=%d",
                apiUrl, apiKey, query, imageType, orientation, category, perPage, page);
        
        return restTemplate.getForEntity(url, String.class);
    }

    @Transactional(readOnly = true)
    public List<Image> getImagesByIds(List<Long> ids) {
        return imageRepository.findByIdIn(ids);
    }
}
