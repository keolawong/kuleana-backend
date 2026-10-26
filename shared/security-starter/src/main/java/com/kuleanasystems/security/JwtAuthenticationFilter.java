package com.kuleanasystems.security;

import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(JwtAuthenticationFilter.class);

    @Autowired
    private JwtTokenProvider tokenProvider;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        try {
            String jwt = getJwtFromRequest(request);

            if (StringUtils.hasText(jwt) && tokenProvider.validateJwtToken(jwt)) {
                Claims claims = tokenProvider.getClaimsFromJwtToken(jwt);
                String username = claims.getSubject();

                if (StringUtils.hasText(username)) {
                    String roles = claims.get("roles", String.class);
                    List<SimpleGrantedAuthority> authorities = parseAuthorities(roles);

                    UUID userId = null;
                    String userIdStr = claims.get("userId", String.class);
                    if (StringUtils.hasText(userIdStr)) {
                        try {
                            userId = UUID.fromString(userIdStr);
                        } catch (IllegalArgumentException ignored) {
                        }
                    }
                    String email = claims.get("email", String.class);

                    UserPrincipal principal = UserPrincipal.builder()
                            .id(userId)
                            .username(username)
                            .email(email)
                            .authorities(authorities)
                            .build();

                    UsernamePasswordAuthenticationToken authentication =
                            new UsernamePasswordAuthenticationToken(principal, null, authorities);
                    authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

                    SecurityContextHolder.getContext().setAuthentication(authentication);
                }
            }
        } catch (Exception ex) {
            log.error("Could not set user authentication in security context", ex);
        }

        filterChain.doFilter(request, response);
    }

    private List<SimpleGrantedAuthority> parseAuthorities(String roles) {
        if (!StringUtils.hasText(roles)) {
            return Collections.emptyList();
        }
        return Arrays.stream(roles.split(","))
                .filter(StringUtils::hasText)
                .flatMap(role -> {
                    String clean = role.trim();
                    String bare = clean.startsWith("ROLE_") ? clean.substring(5) : clean;
                    return Stream.of(
                            new SimpleGrantedAuthority("ROLE_" + bare),
                            new SimpleGrantedAuthority("ROLE_" + bare.toUpperCase()),
                            new SimpleGrantedAuthority("ROLE_" + bare.toLowerCase()),
                            new SimpleGrantedAuthority(bare),
                            new SimpleGrantedAuthority(bare.toUpperCase()),
                            new SimpleGrantedAuthority(bare.toLowerCase())
                    );
                })
                .distinct()
                .collect(Collectors.toList());
    }

    private String getJwtFromRequest(HttpServletRequest request) {
        // 1. Fallback for EventSource / SSE requests
        String queryToken = request.getParameter("token");
        if (StringUtils.hasText(queryToken)) {
            return queryToken;
        }

        // 2. Cookie fallback
        if (request.getCookies() != null) {
            for (Cookie cookie : request.getCookies()) {
                if ("jwt".equals(cookie.getName())) {
                    return cookie.getValue();
                }
            }
        }

        // 3. Authorization Bearer header
        String bearerToken = request.getHeader("Authorization");
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }

        return null;
    }
}
