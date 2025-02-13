package com.galerija.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Entity
@Table(name = "images")
@Data
public class Image {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false, updatable = false)
    private Long id; // Pixabay image ID
    
    @Column(name = "page_url", columnDefinition = "TEXT")
    private String pageURL;
    
    @Column(nullable = false)
    private String type = "photo";
    
    @Column(columnDefinition = "TEXT")
    private String tags;
    
    @Column(name = "preview_url", columnDefinition = "TEXT")
    private String previewURL;
    
    @Column(name = "preview_width")
    private Integer previewWidth = 0;
    
    @Column(name = "preview_height")
    private Integer previewHeight = 0;
    
    @Column(name = "webformat_url", nullable = false, columnDefinition = "TEXT")
    private String webformatURL;
    
    @Column(name = "webformat_width")
    private Integer webformatWidth = 0;
    
    @Column(name = "webformat_height")
    private Integer webformatHeight = 0;
    
    @Column(name = "large_image_url", columnDefinition = "TEXT")
    private String largeImageURL;
    
    @Column(name = "fullhd_url", columnDefinition = "TEXT")
    private String fullHDURL;
    
    @Column(name = "image_url", columnDefinition = "TEXT")
    private String imageURL;
    
    @Column(name = "image_width")
    private Integer imageWidth = 0;
    
    @Column(name = "image_height")
    private Integer imageHeight = 0;
    
    @Column(name = "image_size")
    private Long imageSize = 0L;
    
    @Column
    private Integer views = 0;
    
    @Column
    private Integer downloads = 0;
    
    @Column
    private Integer likes = 0;
    
    @Column
    private Integer comments = 0;
    
    @Column(name = "user_id")
    private String userId = "0";
    
    @Column(name = "created_at")
    private LocalDateTime createdAt;
    
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
