package com.galerija.entity;

import java.time.LocalDateTime;

public class ImageDto {
    private Long id;
    private String fileName;
    private String originalFileName;
    private String type;
    private String webformatURL;
    private String largeImageURL;
    private Long userId;
    private LocalDateTime uploadDate;

    // Getters
    public Long getId() { return id; }
    public String getFileName() { return fileName; }
    public String getOriginalFileName() { return originalFileName; }
    public String getType() { return type; }
    public String getWebformatURL() { return webformatURL; }
    public String getLargeImageURL() { return largeImageURL; }
    public Long getUserId() { return userId; }
    public LocalDateTime getUploadDate() { return uploadDate; }

    // Setters
    public void setId(Long id) { this.id = id; }
    public void setFileName(String fileName) { this.fileName = fileName; }
    public void setOriginalFileName(String originalFileName) { this.originalFileName = originalFileName; }
    public void setType(String type) { this.type = type; }
    public void setWebformatURL(String webformatURL) { this.webformatURL = webformatURL; }
    public void setLargeImageURL(String largeImageURL) { this.largeImageURL = largeImageURL; }
    public void setUserId(Long userId) { this.userId = userId; }
    public void setUploadDate(LocalDateTime uploadDate) { this.uploadDate = uploadDate; }
}
