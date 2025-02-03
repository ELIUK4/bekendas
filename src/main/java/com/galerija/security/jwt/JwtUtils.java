package com.galerija.security.jwt;

import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.UnsupportedJwtException;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import com.galerija.security.services.UserDetailsImpl;

import java.security.Key;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.stereotype.Component;

import java.util.Date;
import javax.crypto.SecretKey;

@Component
public class JwtUtils {
  private static final Logger logger = LoggerFactory.getLogger(JwtUtils.class);
  private static final String TOKEN_TYPE = "Bearer ";
  private static final String INVALID_JWT_SIGNATURE = "Invalid JWT signature";
  private static final String TOKEN_VALIDATION_FAILED = "Token validation failed";

  @Value("${galerija.app.jwtSecret}")
  private String jwtSecret;

  @Value("${galerija.app.jwtExpirationMs}")
  private int jwtExpirationMs;

  private final UserDetailsService userDetailsService;

  public JwtUtils(UserDetailsService userDetailsService) {
    this.userDetailsService = userDetailsService;
  }

  public void setSecretKey(String key) {
    jwtSecret = key;
  }

  public void setExpirationMs(int ms) {
    jwtExpirationMs = ms;
  }

  public String generateJwtToken(Authentication authentication) {
    if (authentication == null || authentication.getPrincipal() == null) {
      throw new IllegalArgumentException("Authentication cannot be null");
    }

    UserDetailsImpl userPrincipal = (UserDetailsImpl) authentication.getPrincipal();
    
    return Jwts.builder()
        .claim("sub", userPrincipal.getUsername())
        .claim("roles", userPrincipal.getAuthorities().toString())
        .setIssuedAt(new Date())
        .setExpiration(new Date((new Date()).getTime() + jwtExpirationMs))
        .signWith(key())
        .compact();
  }

  public Key toKey(String secret) {
    return Keys.hmacShaKeyFor(Decoders.BASE64.decode(secret));
  }

  private Key key() {
    if (jwtSecret == null || jwtSecret.trim().isEmpty()) {
      throw new IllegalStateException("JWT secret cannot be null or empty");
    }
    return toKey(jwtSecret);
  }

  public String getUserNameFromJwtToken(String token) {
    if (token == null || token.trim().isEmpty()) {
      throw new IllegalArgumentException("Token cannot be null or empty");
    }
    
    return Jwts.parserBuilder()
        .setSigningKey(key())
        .build()
        .parseClaimsJws(token)
        .getBody()
        .getSubject();
  }

  public boolean validateJwtToken(String authToken) {
    if (authToken == null || authToken.trim().isEmpty()) {
      logger.error("Token is null or empty");
      return false;
    }

    try {
      Jwts.parserBuilder()
          .setSigningKey(key())
          .build()
          .parseClaimsJws(authToken);
      return true;
    } catch (MalformedJwtException e) {
      logger.error("Invalid JWT token: {}", e.getMessage());
    } catch (ExpiredJwtException e) {
      logger.error("JWT token is expired: {}", e.getMessage());
    } catch (UnsupportedJwtException e) {
      logger.error("JWT token is unsupported: {}", e.getMessage());
    } catch (IllegalArgumentException e) {
      logger.error("JWT claims string is empty: {}", e.getMessage());
    } catch (Exception e) {
      logger.error(TOKEN_VALIDATION_FAILED, e);
    }
    return false;
  }

  public Authentication getAuthentication(String token) {
    if (token == null || token.trim().isEmpty()) {
      throw new IllegalArgumentException("Token cannot be null or empty");
    }

    String username = getUserNameFromJwtToken(token);
    UserDetails userDetails = userDetailsService.loadUserByUsername(username);
    
    if (userDetails == null) {
      throw new IllegalStateException("User details not found for token");
    }

    logger.debug("Authentication successful for user: {}", username);
    return new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
  }
}
