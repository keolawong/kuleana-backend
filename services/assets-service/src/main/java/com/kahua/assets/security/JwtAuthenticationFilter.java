package com.kahua.assets.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
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
import java.util.List;
import java.util.stream.Collectors;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    @Autowired
    private JwtTokenProvider tokenProvider;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        try {
            String jwt = getJwtFromRequest(request);

            if (StringUtils.hasText(jwt) && tokenProvider.validateJwtToken(jwt)) {
                
                String username = tokenProvider.getUsernameFromJwtToken(jwt);
                String roles = tokenProvider.getClaimsFromJwtToken(jwt).get("roles", String.class);
                
                List<SimpleGrantedAuthority> authorities = Arrays.stream(roles.split(","))
                        .filter(StringUtils::hasText)
                        .flatMap(role -> {
                            String clean = role.trim();
                            String bare = clean.startsWith("ROLE_") ? clean.substring(5) : clean;
                            return java.util.stream.Stream.of(
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

                UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                        username, null, authorities);
                authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

                SecurityContextHolder.getContext().setAuthentication(authentication);
            }
        } catch (Exception ex) {
            logger.error("Could not set user authentication in security context", ex);
        }

        filterChain.doFilter(request, response);
    }

    private String getJwtFromRequest(HttpServletRequest request) {
        String queryToken = request.getParameter("token");
        if (org.springframework.util.StringUtils.hasText(queryToken)) {
            return queryToken;
        }
        
        if (request.getCookies() != null) {
            for (jakarta.servlet.http.Cookie cookie : request.getCookies()) {
                if ("jwt".equals(cookie.getName())) {
                    return cookie.getValue();
                }
            }
        }
        String bearerToken = request.getHeader("Authorization");
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        return null;
    }
}
