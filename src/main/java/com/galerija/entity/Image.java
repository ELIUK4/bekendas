package com.galerija.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Entity
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "images")
public class Image {
    @Id
    private Long id; // Pixabay image ID
    
    @Column(name = "page_url", nullable = false)
    private String pageURL;
    
    @Column(nullable = false)
    private String type;
    
    @Column(length = 500)
    private String tags;
    
    @Column(name = "preview_url", nullable = false)
    private String previewURL;
    
    @Column(name = "preview_width")
    private Integer previewWidth;
    
    @Column(name = "preview_height")
    private Integer previewHeight;
    
    @Column(name = "webformat_url", nullable = false)
    private String webformatURL;
    
    @Column(name = "webformat_width")
    private Integer webformatWidth;
    
    @Column(name = "webformat_height")
    private Integer webformatHeight;
    
    @Column(name = "large_image_url")
    private String largeImageURL;
    
    @Column(name = "fullhd_url")
    private String fullHDURL;
    
    @Column(name = "image_url")
    private String imageURL;
    
    @Column(name = "image_width")
    private Integer imageWidth;
    
    @Column(name = "image_height")
    private Integer imageHeight;
    
    @Column(name = "image_size")
    private Long imageSize;
    
    private Integer views;
    private Integer downloads;
    private Integer likes;
    private Integer comments;
    
    @Column(name = "user_id")
    private Long userId;
    
    @Column(name = "created_at")
    private java.time.LocalDateTime createdAt;
    
    @PrePersist
    protected void onCreate() {
        createdAt = java.time.LocalDateTime.now();
    }
}
