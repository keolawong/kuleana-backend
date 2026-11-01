package com.kahua.tracker.service;

import com.kahua.tracker.entity.User;
import com.kahua.tracker.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.OffsetDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private AuthService authService;

    private User testUser;

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setUsername("testuser");
        testUser.setFailedLoginAttempts(0);
        testUser.setLockoutRound(0);
    }

    @Test
    void testFailedLoginIncrementsCount() {
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));
        
        authService.handleFailedLogin("testuser");
        
        assertEquals(1, testUser.getFailedLoginAttempts());
        assertEquals(0, testUser.getLockoutRound());
        verify(userRepository).save(testUser);
    }

    @Test
    void testFailedLoginTriggersLockoutRound1() {
        testUser.setFailedLoginAttempts(4);
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));
        
        authService.handleFailedLogin("testuser");
        
        assertEquals(0, testUser.getFailedLoginAttempts());
        assertEquals(1, testUser.getLockoutRound());
        assertNotNull(testUser.getAccountLockedUntil());
        assertTrue(testUser.getAccountLockedUntil().isAfter(OffsetDateTime.now()));
        assertTrue(testUser.getAccountLockedUntil().isBefore(OffsetDateTime.now().plusMinutes(16)));
        verify(userRepository).save(testUser);
    }

    @Test
    void testFailedLoginTriggersLockoutRound3Permanent() {
        testUser.setFailedLoginAttempts(2);
        testUser.setLockoutRound(2);
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));
        
        authService.handleFailedLogin("testuser");
        
        assertEquals(0, testUser.getFailedLoginAttempts());
        assertEquals(3, testUser.getLockoutRound());
        assertNotNull(testUser.getAccountLockedUntil());
        assertEquals(9999, testUser.getAccountLockedUntil().getYear());
        verify(userRepository).save(testUser);
    }

    @Test
    void testSuccessfulLoginResetsLockout() {
        testUser.setFailedLoginAttempts(2);
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));
        
        authService.handleSuccessfulLogin("testuser");
        
        assertEquals(0, testUser.getFailedLoginAttempts());
        assertEquals(0, testUser.getLockoutRound());
        assertNull(testUser.getAccountLockedUntil());
        verify(userRepository).save(testUser);
    }
}
