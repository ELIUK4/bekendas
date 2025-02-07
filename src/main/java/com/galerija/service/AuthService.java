package com.galerija.service;

import com.galerija.entity.Role;
import com.galerija.entity.UserEntity;
import com.galerija.repository.RoleRepository;
import com.galerija.repository.UserRepository;
import com.galerija.security.jwt.JwtUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.Set;

@Service
public class AuthService {
    private static final Logger logger = LoggerFactory.getLogger(AuthService.class);

    @Autowired
    private AuthenticationManager authenticationManager;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private PasswordEncoder encoder;

    @Autowired
    private JwtUtils jwtUtils;

    public String authenticateUser(String username, String password) {
        try {
            logger.debug("Attempting to authenticate user: {}", username);
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(username, password));

            logger.debug("User authenticated successfully: {}", username);
            SecurityContextHolder.getContext().setAuthentication(authentication);
            
            logger.debug("Generating JWT token for user: {}", username);
            String token = jwtUtils.generateJwtToken(authentication);
            logger.debug("JWT token generated successfully for user: {}", username);
            
            return token;
        } catch (Exception e) {
            logger.error("Authentication failed for user {}: {}", username, e.getMessage(), e);
            throw new RuntimeException("Authentication failed: " + e.getMessage(), e);
        }
    }

    @Transactional
    public UserEntity registerUser(String username, String email, String password) {
        try {
            logger.debug("Attempting to register new user: {}", username);
            
            if (userRepository.existsByUsername(username)) {
                logger.error("Registration failed: Username {} is already taken", username);
                throw new RuntimeException("Error: Username is already taken!");
            }

            if (userRepository.existsByEmail(email)) {
                logger.error("Registration failed: Email {} is already in use", email);
                throw new RuntimeException("Error: Email is already in use!");
            }

            UserEntity user = new UserEntity();
            user.setUsername(username);
            user.setEmail(email);
            user.setPassword(encoder.encode(password));

            Role userRole = roleRepository.findByName(Role.ERole.ROLE_USER)
                    .orElseThrow(() -> {
                        logger.error("Error: User role not found in database");
                        return new RuntimeException("Error: Role is not found. Please check if database is initialized with roles.");
                    });
            
            Set<Role> roles = new HashSet<>();
            roles.add(userRole);
            user.setRoles(roles);

            logger.debug("Saving new user to database: {}", username);
            UserEntity savedUser = userRepository.save(user);
            logger.debug("User registered successfully: {}", username);
            
            return savedUser;
        } catch (Exception e) {
            logger.error("Registration failed for user {}: {}", username, e.getMessage(), e);
            throw e;
        }
    }

    @Transactional
    public void logout() {
        SecurityContextHolder.clearContext();
    }
}
