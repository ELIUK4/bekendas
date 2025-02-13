package com.galerija.dto;

import lombok.Data;

@Data
public class ExternalImageDto {
    private Long id;
    private String pageURL;
    private String type;
    private String tags;
    private String previewURL;
    private Integer previewWidth;
    private Integer previewHeight;
    private String webformatURL;
    private Integer webformatWidth;
    private Integer webformatHeight;
    private String largeImageURL;
    private Integer imageWidth;
    private Integer imageHeight;
    private Long imageSize;
    private Integer views;
    private Integer downloads;
    private Integer likes;
    private Integer comments;
    private String userId;
}
