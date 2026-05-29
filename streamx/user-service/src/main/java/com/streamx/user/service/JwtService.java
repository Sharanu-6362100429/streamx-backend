package com.streamx.user.service;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.Map;

@Service
@Slf4j
public class  JwtService {

    @Value("${app.jwt.secret}")
    private String secret;

    @Value("${app.jwt.access-token-expiry:900000}")   // 15 minutes default
    private long accessTokenExpiry;

    @Value("${app.jwt.refresh-token-expiry:604800000}") // 7 days default
    private long refreshTokenExpiry;

    public String generateAccessToken(String userId, String email, String role) {
        return buildToken(userId, Map.of("email", email, "role", role), accessTokenExpiry);
    }

    public String generateRefreshToken(String userId) {
        return buildToken(userId, Map.of("type", "refresh"), refreshTokenExpiry);
    }

    private String buildToken(String subject, Map<String, Object> claims, long expiry) {
        SecretKey key = getSigningKey();
        return Jwts.builder()
                .subject(subject)
                .claims(claims)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + expiry))
                .signWith(key)
                .compact();
    }

    public Claims extractAllClaims(String token) {
        return Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public String extractUserId(String token) {
        return extractAllClaims(token).getSubject();
    }

    public boolean isTokenValid(String token) {
        try {
            Claims claims = extractAllClaims(token);
            return !claims.getExpiration().before(new Date());
        } catch (Exception e) {
            log.warn("Token validation failed: {}", e.getMessage());
            return false;
        }
    }

    public long getAccessTokenExpiry() {
        return accessTokenExpiry / 1000; // return in seconds
    }

    private SecretKey getSigningKey() {
        return Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }
}
