package com.galerija.service;

import com.galerija.entity.Comment;
import com.galerija.entity.Image;
import com.galerija.entity.UserEntity;
import com.galerija.repository.CommentRepository;
import com.galerija.repository.ImageRepository;
import com.galerija.repository.UserRepository;
import com.galerija.exception.ResourceNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
public class CommentService {
    private static final Logger logger = LoggerFactory.getLogger(CommentService.class);

    @Autowired
    private CommentRepository commentRepository;

    @Autowired
    private ImageRepository imageRepository;

    @Autowired
    private UserRepository userRepository;

    private UserEntity getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new ResourceNotFoundException("No authenticated user found");
        }

        String username = authentication.getName();
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + username));
    }

    @Transactional
    public Comment addComment(Long imageId, String content) {
        logger.info("Adding comment to image {}: {}", imageId, content);
        
        UserEntity user = getCurrentUser();
        Image image = imageRepository.findById(imageId)
                .orElseThrow(() -> new ResourceNotFoundException("Image not found with id: " + imageId));

        Comment comment = new Comment();
        comment.setUser(user);
        comment.setImage(image);
        comment.setContent(content);

        return commentRepository.save(comment);
    }

    @Transactional(readOnly = true)
    public Page<Comment> getImageComments(Long imageId, Pageable pageable) {
        Image image = imageRepository.findById(imageId)
                .orElseThrow(() -> new ResourceNotFoundException("Image not found with id: " + imageId));
        return commentRepository.findByImage(image, pageable);
    }

    @Transactional(readOnly = true)
    public Page<Comment> getUserComments(Long userId, Pageable pageable) {
        UserEntity user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));
        return commentRepository.findByUser(user, pageable);
    }
}
