package com.kahua.tracker.security;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class PasswordValidatorTest {

    @Test
    void validPasswordPasses() {
        assertDoesNotThrow(() -> PasswordValidator.validate("Super$ecr3tP@ss!", "user1", "John", "Doe"));
    }

    @Test
    void tooShortPasswordThrows() {
        Exception e = assertThrows(IllegalArgumentException.class, () -> PasswordValidator.validate("Sh0rt!", "user1", "John", "Doe"));
        assertTrue(e.getMessage().contains("must be between"));
    }

    @Test
    void noUppercaseThrows() {
        Exception e = assertThrows(IllegalArgumentException.class, () -> PasswordValidator.validate("super$ecr3tp@ss!", "user1", "John", "Doe"));
        assertTrue(e.getMessage().contains("uppercase"));
    }

    @Test
    void noDigitThrows() {
        Exception e = assertThrows(IllegalArgumentException.class, () -> PasswordValidator.validate("Super$ecretP@ss!", "user1", "John", "Doe"));
        assertTrue(e.getMessage().contains("digit"));
    }

    @Test
    void noSpecialCharThrows() {
        Exception e = assertThrows(IllegalArgumentException.class, () -> PasswordValidator.validate("SuperSecretP4ss", "user1", "John", "Doe"));
        assertTrue(e.getMessage().contains("special character"));
    }

    @Test
    void containsUsernameThrows() {
        Exception e = assertThrows(IllegalArgumentException.class, () -> PasswordValidator.validate("Johnuser1$12345", "user1", "John", "Doe"));
        assertTrue(e.getMessage().contains("username"));
    }

    @Test
    void containsFirstNameThrows() {
        Exception e = assertThrows(IllegalArgumentException.class, () -> PasswordValidator.validate("JohnD0e$123456", "user1", "John", "Doe"));
        assertTrue(e.getMessage().contains("first name"));
    }

    @Test
    void containsBlacklistedWordThrows() {
        Exception e = assertThrows(IllegalArgumentException.class, () -> PasswordValidator.validate("W3lc0m3!123456", "user1", "John", "Doe"));
        assertTrue(e.getMessage().contains("blacklisted word"));
    }
}
