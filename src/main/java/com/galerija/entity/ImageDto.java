package com.galerija.entity;

import java.time.LocalDateTime;

public class ImageDto {
    private Long id;
    private String fileName;
    private String originalFileName;
    private String type;
    private String webformatURL;
    private String largeImageURL;
    private String previewURL;
    private Long userId;
    private LocalDateTime uploadDate;
    private ImagePrivacy privacy;
    private Integer likes;
    private Integer downloads;
    private Integer views;
    private String tags;

    // Getters
    public Long getId() { return id; }
    public String getFileName() { return fileName; }
    public String getOriginalFileName() { return originalFileName; }
    public String getType() { return type; }
    public String getWebformatURL() { return webformatURL; }
    public String getLargeImageURL() { return largeImageURL; }
    public String getPreviewURL() { return previewURL; }
    public Long getUserId() { return userId; }
    public LocalDateTime getUploadDate() { return uploadDate; }
    public ImagePrivacy getPrivacy() { return privacy; }
    public Integer getLikes() { return likes; }
    public Integer getDownloads() { return downloads; }
    public Integer getViews() { return views; }
    public String getTags() { return tags; }

    // Setters
    public void setId(Long id) { this.id = id; }
    public void setFileName(String fileName) { this.fileName = fileName; }
    public void setOriginalFileName(String originalFileName) { this.originalFileName = originalFileName; }
    public void setType(String type) { this.type = type; }
    public void setWebformatURL(String webformatURL) { this.webformatURL = webformatURL; }
    public void setLargeImageURL(String largeImageURL) { this.largeImageURL = largeImageURL; }
    public void setPreviewURL(String previewURL) { this.previewURL = previewURL; }
    public void setUserId(Long userId) { this.userId = userId; }
    public void setUploadDate(LocalDateTime uploadDate) { this.uploadDate = uploadDate; }
    public void setPrivacy(ImagePrivacy privacy) { this.privacy = privacy; }
    public void setLikes(Integer likes) { this.likes = likes; }
    public void setDownloads(Integer downloads) { this.downloads = downloads; }
    public void setViews(Integer views) { this.views = views; }
    public void setTags(String tags) { this.tags = tags; }
}
