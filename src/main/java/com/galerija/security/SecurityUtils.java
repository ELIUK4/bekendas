package com.galerija.security;

import com.galerija.entity.UserEntity;
import com.galerija.repository.UserRepository;
import com.galerija.security.services.UserDetailsImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

@Component
public class SecurityUtils {
    private static final Logger logger = LoggerFactory.getLogger(SecurityUtils.class);
    
    @Autowired
    private UserRepository userRepository;

    public UserEntity getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        logger.debug("Current authentication: {}", authentication);
        
        if (authentication == null) {
            logger.warn("No authentication found");
            return null;
        }

        if (!authentication.isAuthenticated()) {
            logger.warn("User is not authenticated");
            return null;
        }

        Object principal = authentication.getPrincipal();
        logger.debug("Principal type: {}", principal.getClass().getName());
        
        if (principal instanceof UserDetailsImpl) {
            Long userId = ((UserDetailsImpl) principal).getId();
            logger.debug("Found user ID from UserDetailsImpl: {}", userId);
            
            return userRepository.findById(userId)
                    .orElseGet(() -> {
                        logger.warn("No user found for ID: {}", userId);
                        return null;
                    });
        } else if (principal instanceof UserDetails) {
            String username = ((UserDetails) principal).getUsername();
            logger.debug("Found username from UserDetails: {}", username);
            
            return userRepository.findByUsername(username)
                    .orElseGet(() -> {
                        logger.warn("No user found for username: {}", username);
                        return null;
                    });
        }
        
        logger.warn("Unsupported principal type: {}", principal.getClass().getName());
        return null;
    }

    public Long getCurrentUserId() {
        UserEntity currentUser = getCurrentUser();
        if (currentUser == null) {
            logger.warn("No current user found");
            return null;
        }
        logger.debug("Current user ID: {}", currentUser.getId());
        return currentUser.getId();
    }

    public UserEntity getCurrentUserEntity() {
        UserEntity currentUser = getCurrentUser();
        if (currentUser == null) {
            logger.warn("No current user found");
            throw new com.galerija.exception.ResourceNotFoundException("Vartotojas nerastas arba neprisijungęs");
        }
        return currentUser;
    }
}
