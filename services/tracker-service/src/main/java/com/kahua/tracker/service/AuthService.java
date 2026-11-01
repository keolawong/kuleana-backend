package com.kahua.tracker.service;

import com.kahua.tracker.dto.RegistrationRequest;
import com.kahua.tracker.entity.User;
import com.kahua.tracker.entity.UserSecurityQuestion;
import com.kahua.tracker.repository.UserRepository;
import com.kahua.tracker.repository.UserSecurityQuestionRepository;
import com.kahua.tracker.security.PasswordValidator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class AuthService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserSecurityQuestionRepository userSecurityQuestionRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;
    
    @Autowired
    private UserAuthCacheService userAuthCacheService;

    public void handleFailedLogin(String username) {
        Optional<User> optUser = userRepository.findByUsername(username);
        if (optUser.isEmpty()) return;

        User user = optUser.get();
        if (user.getAccountLockedUntil() != null && user.getAccountLockedUntil().isAfter(OffsetDateTime.now())) {
            return;
        }

        int fails = user.getFailedLoginAttempts() + 1;
        int round = user.getLockoutRound();

        if (round == 0 && fails >= 5) {
            user.setLockoutRound(1);
            user.setAccountLockedUntil(OffsetDateTime.now().plusMinutes(15));
            user.setFailedLoginAttempts(0);
        } else if (round == 1 && fails >= 3) {
            user.setLockoutRound(2);
            user.setAccountLockedUntil(OffsetDateTime.now().plusMinutes(60));
            user.setFailedLoginAttempts(0);
        } else if (round >= 2 && fails >= 3) {
            user.setLockoutRound(3);
            // 9999-12-31T23:59:59Z
            user.setAccountLockedUntil(OffsetDateTime.parse("9999-12-31T23:59:59Z"));
            user.setFailedLoginAttempts(0);
        } else {
            user.setFailedLoginAttempts(fails);
        }
        userRepository.save(user);
    }

    public void handleSuccessfulLogin(String username) {
        userRepository.findByUsername(username).ifPresent(user -> {
            if (user.getFailedLoginAttempts() > 0 || user.getLockoutRound() > 0) {
                user.setFailedLoginAttempts(0);
                user.setLockoutRound(0);
                user.setAccountLockedUntil(null);
                userRepository.save(user);
            }
        });
    }

    public void checkLockoutAndExpiration(User user) {
        if (user.getAccountLockedUntil() != null && user.getAccountLockedUntil().isAfter(OffsetDateTime.now())) {
            if (user.getLockoutRound() >= 3) {
                throw new RuntimeException("Account permanently locked due to excessive failed attempts. Please contact an administrator.");
            } else {
                long minutes = java.time.Duration.between(OffsetDateTime.now(), user.getAccountLockedUntil()).toMinutes();
                throw new RuntimeException("Account locked. Try again in " + (minutes + 1) + " minutes.");
            }
        }
        if (user.getPasswordResetExpiresAt() != null && user.getPasswordResetExpiresAt().isBefore(OffsetDateTime.now())) {
            throw new RuntimeException("Temporary password has expired. Please contact an administrator.");
        }
    }
}
