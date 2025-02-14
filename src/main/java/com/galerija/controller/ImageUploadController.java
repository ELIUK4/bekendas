package com.galerija.controller;

import com.galerija.entity.ImageDto;
import com.galerija.entity.Image;
import com.galerija.service.ImageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.net.MalformedURLException;
import java.nio.file.Path;
import java.nio.file.Paths;

@RestController
@RequestMapping("/api/images")
public class ImageUploadController {

    private final ImageService imageService;
    private final String uploadDir;

    @Autowired
    public ImageUploadController(
            ImageService imageService,
            @Value("${app.upload.dir:${user.home}/uploads}") String uploadDir
    ) {
        this.imageService = imageService;
        this.uploadDir = uploadDir;
    }

    @PostMapping("/upload")
    public ResponseEntity<ImageDto> uploadImage(
            @RequestParam("file") MultipartFile file,
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        Image savedImage = imageService.saveUploadedImage(file, userDetails.getUsername());
        return ResponseEntity.ok()
            .contentType(MediaType.APPLICATION_JSON)
            .body(imageService.toDto(savedImage));
    }

    @GetMapping("/uploads/{fileName:.+}")
    public ResponseEntity<Resource> serveFile(@PathVariable String fileName) {
        try {
            Path filePath = Paths.get(uploadDir).resolve(fileName).normalize();
            Resource resource = new UrlResource(filePath.toUri());
            
            if (resource.exists() && resource.isReadable()) {
                return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_TYPE, MediaType.IMAGE_JPEG_VALUE)
                    .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + resource.getFilename() + "\"")
                    .body(resource);
            } else {
                throw new RuntimeException("Could not read file: " + fileName);
            }
        } catch (MalformedURLException e) {
            throw new RuntimeException("Could not read file: " + fileName, e);
        }
    }
}
