package com.kuleanasystems.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Component;

import java.security.Key;
import java.util.Date;
import java.util.UUID;
import java.util.stream.Collectors;

@Component
public class JwtTokenProvider {

    @Value("${security.jwt.secret:${app.jwtSecret:kuleana-systems-secret-key-that-is-at-least-256-bits-long-for-hmac-sha-security}}")
    private String jwtSecret;

    @Value("${security.jwt.expiration-ms:${app.jwtExpirationMs:86400000}}")
    private long jwtExpirationMs;

    private Key key;

    @PostConstruct
    public void init() {
        this.key = Keys.hmacShaKeyFor(jwtSecret.getBytes());
    }

    public String generateToken(Authentication authentication) {
        String username = authentication.getName();
        String roles = authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.joining(","));

        UUID userId = null;
        String email = null;
        if (authentication.getPrincipal() instanceof UserPrincipal principal) {
            userId = principal.getId();
            email = principal.getEmail();
        }

        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + jwtExpirationMs);

        var builder = Jwts.builder()
                .setSubject(username)
                .claim("roles", roles)
                .setIssuedAt(now)
                .setExpiration(expiryDate);

        if (userId != null) {
            builder.claim("userId", userId.toString());
        }
        if (email != null) {
            builder.claim("email", email);
        }

        return builder.signWith(key, SignatureAlgorithm.HS256).compact();
    }

    public String generateToken(String username, String roles, UUID userId, String email) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + jwtExpirationMs);

        var builder = Jwts.builder()
                .setSubject(username)
                .claim("roles", roles != null ? roles : "")
                .setIssuedAt(now)
                .setExpiration(expiryDate);

        if (userId != null) {
            builder.claim("userId", userId.toString());
        }
        if (email != null) {
            builder.claim("email", email);
        }

        return builder.signWith(key, SignatureAlgorithm.HS256).compact();
    }

    public Claims getClaimsFromJwtToken(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(key)
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    public boolean validateJwtToken(String authToken) {
        try {
            Jwts.parserBuilder().setSigningKey(key).build().parseClaimsJws(authToken);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }
}
