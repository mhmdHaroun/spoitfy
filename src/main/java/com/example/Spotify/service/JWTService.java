package com.example.Spotify.service;

import org.springframework.security.core.userdetails.UserDetails;

import java.util.HashMap;

public interface JWTService {
    String extractUserName(String token);
    Long extractUserId(String token);
    String generateToken(UserDetails userDetails);
    boolean isTokenValid(String token, UserDetails userDetails);
    String generateRefreshToken(HashMap<String, Object> extraClaims, UserDetails userDetails) ;
    String generateRefreshToken(UserDetails userDetails);
}
