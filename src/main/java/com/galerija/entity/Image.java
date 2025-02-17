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
    @Column(name = "id", nullable = false)
    private Long id; // Auto-generated ID
    
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
    
    @Column(name = "webformat_url", nullable = false, columnDefinition = "LONGTEXT")
    private String webformatURL;
    
    @Column(name = "webformat_width")
    private Integer webformatWidth = 0;
    
    @Column(name = "webformat_height")
    private Integer webformatHeight = 0;
    
    @Column(name = "large_image_url", columnDefinition = "LONGTEXT")
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
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private UserEntity user;

    public UserEntity getUser() {
        return user;
    }

    public void setUser(UserEntity user) {
        this.user = user;
    }

    public Long getUserId() {
        return user != null ? user.getId() : null;
    }

    public void setUserId(Long userId) {
        // This method is kept for backwards compatibility
        // The user should be set using setUser(UserEntity) instead
    }
    
    @Column(name = "file_name")
    private String fileName;

    @Column(name = "original_file_name")
    private String originalFileName;

    @Column(name = "upload_date")
    private LocalDateTime uploadDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "privacy", nullable = false)
    private ImagePrivacy privacy = ImagePrivacy.PRIVATE;

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public String getOriginalFileName() {
        return originalFileName;
    }

    public void setOriginalFileName(String originalFileName) {
        this.originalFileName = originalFileName;
    }

    public LocalDateTime getUploadDate() {
        return uploadDate;
    }

    public void setUploadDate(LocalDateTime uploadDate) {
        this.uploadDate = uploadDate;
    }
    
    public ImagePrivacy getPrivacy() {
        return privacy;
    }

    public void setPrivacy(ImagePrivacy privacy) {
        this.privacy = privacy;
    }
    
    @Column(name = "created_at")
    private LocalDateTime createdAt;
    
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
