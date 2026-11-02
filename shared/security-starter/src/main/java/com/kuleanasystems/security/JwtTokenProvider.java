package com.kuleanasystems.security;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Component;

import java.security.Key;
import java.util.Date;
import java.util.stream.Collectors;

@Component
public class JwtTokenProvider {

    @Value("${app.jwtSecret:your-256-bit-secret-your-256-bit-secret}")
    private String jwtSecret;

    @Value("${app.jwtExpirationMs:86400000}")
    private int jwtExpirationMs;

    private Key key;

    @PostConstruct
    public void init() {
        this.key = Keys.hmacShaKeyFor(jwtSecret.getBytes());
    }

    public String generateToken(Authentication authentication) {
        return generateToken(authentication, (String) null);
    }

    public String generateToken(Authentication authentication, String activeOrgId) {
        String username = authentication.getName();
        String roles = authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.joining(","));

        return generateToken(username, roles, activeOrgId);
    }

    public String generateToken(String username, String roles) {
        return generateToken(username, roles, (String) null);
    }

    public String generateToken(String username, String roles, String activeOrgId) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + jwtExpirationMs);

        JwtBuilder builder = Jwts.builder()
                .setSubject(username)
                .claim("roles", roles != null ? roles : "")
                .setIssuedAt(now)
                .setExpiration(expiryDate);

        if (activeOrgId != null && !activeOrgId.trim().isEmpty()) {
            builder.claim("activeOrgId", activeOrgId.trim());
            builder.claim("orgId", activeOrgId.trim());
        }

        return builder.signWith(key, SignatureAlgorithm.HS256).compact();
    }

    public String getUsernameFromJwtToken(String token) {
        return getClaimsFromJwtToken(token).getSubject();
    }

    public String getRolesFromJwtToken(String token) {
        return getClaimsFromJwtToken(token).get("roles", String.class);
    }

    public String getActiveOrgIdFromJwtToken(String token) {
        Claims claims = getClaimsFromJwtToken(token);
        String orgId = claims.get("activeOrgId", String.class);
        if (orgId == null) {
            orgId = claims.get("orgId", String.class);
        }
        return orgId;
    }

    public Claims getClaimsFromJwtToken(String token) {
        return Jwts.parserBuilder().setSigningKey(key).build()
                .parseClaimsJws(token).getBody();
    }

    public boolean validateJwtToken(String authToken) {
        try {
            Jwts.parserBuilder().setSigningKey(key).build().parseClaimsJws(authToken);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            // Ignore / invalid token
        }
        return false;
    }
}
