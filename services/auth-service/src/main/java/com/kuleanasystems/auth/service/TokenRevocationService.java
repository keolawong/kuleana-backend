package com.kuleanasystems.auth.service;

import com.kuleanasystems.auth.entity.RevokedToken;
import com.kuleanasystems.auth.repository.RevokedTokenRepository;
import com.kuleanasystems.security.TokenRevocationChecker;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.Date;

@Service
@RequiredArgsConstructor
public class TokenRevocationService implements TokenRevocationChecker {

    private final TokenBlacklistService tokenBlacklistService;
    private final UserAuthCacheService userAuthCacheService;
    private final RevokedTokenRepository revokedTokenRepository;

    @Transactional
    public void revokeToken(String token, OffsetDateTime expiresAt) {
        tokenBlacklistService.blacklist(token);
        if (!revokedTokenRepository.existsByToken(token)) {
            RevokedToken revokedToken = RevokedToken.builder()
                    .token(token)
                    .revokedAt(OffsetDateTime.now())
                    .expiresAt(expiresAt)
                    .build();
            revokedTokenRepository.save(revokedToken);
        }
    }

    @Override
    public boolean isRevoked(String token, String username, Date issuedAt) {
        if (token != null && tokenBlacklistService.isBlacklisted(token)) {
            return true;
        }
        if (username != null && issuedAt != null && userAuthCacheService.isTokenStale(username, issuedAt)) {
            return true;
        }
        return false;
    }
}
