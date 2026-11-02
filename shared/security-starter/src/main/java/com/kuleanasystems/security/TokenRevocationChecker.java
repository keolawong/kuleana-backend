package com.kuleanasystems.security;

import java.util.Date;

/**
 * Optional hook for checking if a token or user's session has been invalidated.
 */
public interface TokenRevocationChecker {
    boolean isRevoked(String token, String username, Date issuedAt);
}
