package com.galerija.controller;

import com.galerija.entity.Comment;
import com.galerija.service.CommentService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@CrossOrigin(origins = "http://localhost:3000", allowCredentials = "true", maxAge = 3600)
@RestController
@RequestMapping("/api/images/{imageId}/comments")
public class CommentController {
    private static final Logger logger = LoggerFactory.getLogger(CommentController.class);

    @Autowired
    private CommentService commentService;

    @PostMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> addComment(@PathVariable Long imageId, @RequestBody Map<String, String> payload) {
        try {
            String content = payload.get("comment");
            if (content == null || content.trim().isEmpty()) {
                Map<String, Object> response = new HashMap<>();
                response.put("error", "Comment content cannot be empty");
                response.put("status", 400);
                return ResponseEntity.badRequest().body(response);
            }

            Comment comment = commentService.addComment(imageId, content.trim());
            return ResponseEntity.ok(comment);
        } catch (Exception e) {
            logger.error("Error adding comment: {}", e.getMessage(), e);
            Map<String, Object> response = new HashMap<>();
            response.put("error", "Error adding comment: " + e.getMessage());
            response.put("status", 500);
            return ResponseEntity.internalServerError().body(response);
        }
    }

    @GetMapping
    public ResponseEntity<?> getImageComments(
            @PathVariable Long imageId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        try {
            Page<Comment> comments = commentService.getImageComments(
                imageId,
                PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"))
            );
            return ResponseEntity.ok(comments);
        } catch (Exception e) {
            logger.error("Error getting comments: {}", e.getMessage(), e);
            Map<String, Object> response = new HashMap<>();
            response.put("error", "Error getting comments: " + e.getMessage());
            response.put("status", 500);
            return ResponseEntity.internalServerError().body(response);
        }
    }
}
