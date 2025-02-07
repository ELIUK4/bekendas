package com.galerija.service;

import com.galerija.entity.Role;
import com.galerija.entity.UserEntity;
import com.galerija.repository.RoleRepository;
import com.galerija.repository.UserRepository;
import com.galerija.security.jwt.JwtUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private UserRepository userRepository;

    @Mock
    private RoleRepository roleRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtUtils jwtUtils;

    @InjectMocks
    private AuthService authService;

    @Mock
    private Authentication authentication;

    private static final String USERNAME = "testUser";
    private static final String PASSWORD = "testPass";
    private static final String EMAIL = "test@example.com";
    private static final String JWT_TOKEN = "test.jwt.token";

    private UserEntity testUser;
    private Role userRole;

    @BeforeEach
    void setUp() {
        testUser = new UserEntity();
        testUser.setId(1L);
        testUser.setUsername(USERNAME);
        testUser.setEmail(EMAIL);
        testUser.setPassword(PASSWORD);

        userRole = new Role();
        userRole.setId(1L);
        userRole.setName(Role.ERole.ROLE_USER);

        // Reset mocks before each test
        reset(userRepository, roleRepository, passwordEncoder, authenticationManager, jwtUtils);
    }

    @Test
    void authenticateUser_Success() {
        // Arrange
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(authentication);
        when(jwtUtils.generateJwtToken(authentication)).thenReturn(JWT_TOKEN);

        // Act
        String token = authService.authenticateUser(USERNAME, PASSWORD);

        // Assert
        assertNotNull(token);
        assertEquals(JWT_TOKEN, token);
        verify(authenticationManager).authenticate(any(UsernamePasswordAuthenticationToken.class));
        verify(jwtUtils).generateJwtToken(authentication);
    }

    @Test
    void registerUser_Success() {
        // Arrange
        when(userRepository.existsByUsername(USERNAME)).thenReturn(false);
        when(userRepository.existsByEmail(EMAIL)).thenReturn(false);
        when(passwordEncoder.encode(PASSWORD)).thenReturn("encodedPassword");
        when(roleRepository.findByName(Role.ERole.ROLE_USER)).thenReturn(java.util.Optional.of(userRole));
        when(userRepository.save(any(UserEntity.class))).thenReturn(testUser);

        // Act
        UserEntity result = authService.registerUser(USERNAME, EMAIL, PASSWORD);

        // Assert
        assertNotNull(result);
        assertEquals(USERNAME, result.getUsername());
        verify(userRepository).save(any(UserEntity.class));
    }

    @Test
    void registerUser_UsernameExists() {
        // Arrange
        when(userRepository.existsByUsername(USERNAME)).thenReturn(true);

        // Act & Assert
        Exception exception = assertThrows(RuntimeException.class, () -> 
            authService.registerUser(USERNAME, EMAIL, PASSWORD)
        );
        assertTrue(exception.getMessage().contains("Username is already taken"));
        verify(userRepository, never()).save(any(UserEntity.class));
    }

    @Test
    void registerUser_EmailExists() {
        // Arrange
        when(userRepository.existsByUsername(USERNAME)).thenReturn(false);
        when(userRepository.existsByEmail(EMAIL)).thenReturn(true);

        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class, () -> 
            authService.registerUser(USERNAME, EMAIL, PASSWORD)
        );
        assertEquals("Error: Email is already in use!", exception.getMessage());
        verify(userRepository, never()).save(any(UserEntity.class));
    }
}
