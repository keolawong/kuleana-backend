package com.kahua.tracker.security;

import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

public class PasswordValidator {

    private static final int MIN_LENGTH = 12;
    private static final int MAX_LENGTH = 128;
    private static final Pattern UPPERCASE_PATTERN = Pattern.compile("[A-Z]");
    private static final Pattern LOWERCASE_PATTERN = Pattern.compile("[a-z]");
    private static final Pattern DIGIT_PATTERN = Pattern.compile("[0-9]");
    private static final Pattern SPECIAL_CHAR_PATTERN = Pattern.compile("[!@#$%^&*()_+\\-=\\[\\]{};':\"\\\\|,.<>/?~]");
    
    private static final List<String> BLACKLIST = List.of(
            "password", "admin", "welcome", "qwerty", "kahua"
    );
    
    private static final Map<Character, Character> LEET_MAP = Map.of(
            '@', 'a',
            '0', 'o',
            '1', 'i',
            '!', 'i',
            '3', 'e',
            '5', 's',
            '$', 's',
            '7', 't'
    );

    public static void validate(String password, String username, String firstName, String lastName) {
        if (password == null || password.length() < MIN_LENGTH || password.length() > MAX_LENGTH) {
            throw new IllegalArgumentException("Password must be between " + MIN_LENGTH + " and " + MAX_LENGTH + " characters.");
        }
        
        if (!UPPERCASE_PATTERN.matcher(password).find()) {
            throw new IllegalArgumentException("Password must contain at least one uppercase letter.");
        }
        if (!LOWERCASE_PATTERN.matcher(password).find()) {
            throw new IllegalArgumentException("Password must contain at least one lowercase letter.");
        }
        if (!DIGIT_PATTERN.matcher(password).find()) {
            throw new IllegalArgumentException("Password must contain at least one digit.");
        }
        if (!SPECIAL_CHAR_PATTERN.matcher(password).find()) {
            throw new IllegalArgumentException("Password must contain at least one special character.");
        }
        
        String lowerPassword = password.toLowerCase();
        if (username != null && lowerPassword.contains(username.toLowerCase())) {
            throw new IllegalArgumentException("Password cannot contain your username.");
        }
        if (firstName != null && !firstName.isBlank() && lowerPassword.contains(firstName.toLowerCase())) {
            throw new IllegalArgumentException("Password cannot contain your first name.");
        }
        if (lastName != null && !lastName.isBlank() && lowerPassword.contains(lastName.toLowerCase())) {
            throw new IllegalArgumentException("Password cannot contain your last name.");
        }
        
        String normalized = normalizeLeetspeak(lowerPassword);
        for (String word : BLACKLIST) {
            if (normalized.contains(word)) {
                throw new IllegalArgumentException("Password contains a blacklisted word.");
            }
        }
    }
    
    private static String normalizeLeetspeak(String input) {
        StringBuilder sb = new StringBuilder();
        for (char c : input.toCharArray()) {
            sb.append(LEET_MAP.getOrDefault(c, c));
        }
        return sb.toString();
    }
}
