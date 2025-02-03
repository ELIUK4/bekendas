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
    
    @Column(nullable = false)
    private String pageURL;
    
    @Column(nullable = false)
    private String type;
    
    @Column(length = 500)
    private String tags;
    
    @Column(nullable = false)
    private String previewURL;
    private Integer previewWidth;
    private Integer previewHeight;
    
    @Column(nullable = false)
    private String webformatURL;
    private Integer webformatWidth;
    private Integer webformatHeight;
    
    private String largeImageURL;
    private String fullHDURL;
    private String imageURL;
    
    private Integer imageWidth;
    private Integer imageHeight;
    private Long imageSize;
    
    private Integer views;
    private Integer downloads;
    private Integer likes;
    private Integer comments;
    
    @Column(name = "pixabay_user_id")
    private Long userId;
    
    @Column(name = "pixabay_username")
    private String user;
    
    private String userImageURL;
    
    @Column(name = "saved_date")
    private java.time.LocalDateTime savedDate;
}
