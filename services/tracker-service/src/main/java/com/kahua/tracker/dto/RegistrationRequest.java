package com.kahua.tracker.dto;

import lombok.Data;

@Data
public class RegistrationRequest {
    private String username;
    private String password;
    private String firstName;
    private String lastName;
    
    @com.fasterxml.jackson.annotation.JsonAlias({"questions"})
    private java.util.List<SecurityQuestionDto> securityQuestions;

    @lombok.Data
    public static class SecurityQuestionDto {
        private String question;
        private String answer;
    }
}
