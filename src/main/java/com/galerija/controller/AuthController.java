package com.galerija.controller;

import com.galerija.dto.JwtResponse;
import com.galerija.dto.LoginRequest;
import com.galerija.dto.SignupRequest;
import com.galerija.entity.UserEntity;
import com.galerija.security.services.UserDetailsImpl;
import com.galerija.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/auth")
public class AuthController {
    @Autowired
    private AuthService authService;

    @PostMapping("/signin")
    @PreAuthorize("permitAll()")
    public ResponseEntity<?> authenticateUser(@Valid @RequestBody LoginRequest loginRequest) {
        String jwt = authService.authenticateUser(loginRequest.getUsername(), loginRequest.getPassword());
        
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();
        
        List<String> roles = userDetails.getAuthorities().stream()
                .map(item -> item.getAuthority())
                .collect(Collectors.toList());

        return ResponseEntity.ok(new JwtResponse(
                jwt,
                userDetails.getId(),
                userDetails.getUsername(),
                userDetails.getEmail(),
                roles));
    }

    @PostMapping("/signup")
    @PreAuthorize("permitAll()")
    public ResponseEntity<?> registerUser(@Valid @RequestBody SignupRequest signUpRequest) {
        UserEntity user = authService.registerUser(
                signUpRequest.getUsername(),
                signUpRequest.getEmail(),
                signUpRequest.getPassword());

        // After successful registration, automatically authenticate the user
        String jwt = authService.authenticateUser(signUpRequest.getUsername(), signUpRequest.getPassword());
        
        return ResponseEntity.ok(new JwtResponse(
                jwt,
                user.getId(),
                user.getUsername(),
                user.getEmail(),
                user.getRoles().stream()
                    .map(role -> role.getName().name())
                    .collect(Collectors.toList())));
    }

    @PostMapping("/signout")
    public ResponseEntity<?> logoutUser() {
        authService.logout();
        return ResponseEntity.ok("User logged out successfully!");
    }
}
