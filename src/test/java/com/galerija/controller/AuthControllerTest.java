package com.galerija.controller;

import com.galerija.dto.JwtResponse;
import com.galerija.dto.LoginRequest;
import com.galerija.dto.MessageResponse;
import com.galerija.dto.SignupRequest;
import com.galerija.entity.UserEntity;
import com.galerija.security.services.UserDetailsImpl;
import com.galerija.service.AuthService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Collections;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class AuthControllerTest {

    @Mock
    private AuthService authService;

    @Mock
    private Authentication authentication;

    @Mock
    private SecurityContext securityContext;

    @InjectMocks
    private AuthController authController;

    private static final String USERNAME = "testUser";
    private static final String PASSWORD = "testPass";
    private static final String EMAIL = "test@example.com";
    private static final String JWT_TOKEN = "test.jwt.token";

    @BeforeEach
    void setUp() {
        SecurityContextHolder.setContext(securityContext);
    }

    @Test
    void authenticateUser_Success() {
        // Arrange
        LoginRequest loginRequest = new LoginRequest(USERNAME, PASSWORD);

        UserDetailsImpl userDetails = new UserDetailsImpl(
            1L, USERNAME, EMAIL, PASSWORD,
            Collections.singletonList(new SimpleGrantedAuthority("ROLE_USER"))
        );

        when(authService.authenticateUser(USERNAME, PASSWORD)).thenReturn(JWT_TOKEN);
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.getPrincipal()).thenReturn(userDetails);

        // Act
        ResponseEntity<?> response = authController.authenticateUser(loginRequest);

        // Assert
        assertTrue(response.getStatusCode().is2xxSuccessful());
        assertTrue(response.getBody() instanceof JwtResponse);
        JwtResponse jwtResponse = (JwtResponse) response.getBody();
        assertEquals(JWT_TOKEN, jwtResponse.getToken());
        assertEquals(USERNAME, jwtResponse.getUsername());
        assertEquals(EMAIL, jwtResponse.getEmail());
    }

    @Test
    void authenticateUser_Failure() {
        // Arrange
        LoginRequest loginRequest = new LoginRequest(USERNAME, PASSWORD);

        when(authService.authenticateUser(USERNAME, PASSWORD))
            .thenThrow(new RuntimeException("Authentication failed"));

        // Act
        ResponseEntity<?> response = authController.authenticateUser(loginRequest);

        // Assert
        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
        assertTrue(response.getBody() instanceof MessageResponse);
        MessageResponse messageResponse = (MessageResponse) response.getBody();
        assertTrue(messageResponse.getMessage().contains("Authentication failed"));
    }

    @Test
    void registerUser_Success() {
        // Arrange
        SignupRequest signupRequest = new SignupRequest();
        signupRequest.setUsername(USERNAME);
        signupRequest.setEmail(EMAIL);
        signupRequest.setPassword(PASSWORD);

        UserEntity registeredUser = new UserEntity();
        registeredUser.setUsername(USERNAME);
        registeredUser.setEmail(EMAIL);

        when(authService.authenticateUser(USERNAME, PASSWORD)).thenReturn(JWT_TOKEN);
        when(authService.registerUser(
            eq(USERNAME),
            eq(EMAIL),
            eq(PASSWORD)
        )).thenReturn(registeredUser);

        // Act
        ResponseEntity<?> response = authController.registerUser(signupRequest);

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertTrue(response.getBody() instanceof JwtResponse);
        JwtResponse jwtResponse = (JwtResponse) response.getBody();
        assertEquals(JWT_TOKEN, jwtResponse.getToken());
        assertEquals(USERNAME, jwtResponse.getUsername());
        assertEquals(EMAIL, jwtResponse.getEmail());
    }

    @Test
    void registerUser_Failure() {
        // Arrange
        SignupRequest signupRequest = new SignupRequest();
        signupRequest.setUsername(USERNAME);
        signupRequest.setEmail(EMAIL);
        signupRequest.setPassword(PASSWORD);

        when(authService.registerUser(
            eq(USERNAME),
            eq(EMAIL),
            eq(PASSWORD)
        ))
            .thenThrow(new RuntimeException("Error: Username is already taken!"));

        // Act
        ResponseEntity<?> response = authController.registerUser(signupRequest);

        // Assert
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertTrue(response.getBody() instanceof MessageResponse);
        MessageResponse messageResponse = (MessageResponse) response.getBody();
        assertEquals("Error: Username is already taken!", messageResponse.getMessage());
    }
}
