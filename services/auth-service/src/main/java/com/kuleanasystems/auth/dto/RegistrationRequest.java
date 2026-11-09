package com.kuleanasystems.auth.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import lombok.Data;

import java.util.List;

@Data
public class RegistrationRequest {
    private String username;
    private String password;
    private String firstName;
    private String lastName;

    private String organizationName;
    private String organizationSlug;
    private String inviteCode;

    @JsonAlias({"questions"})
    private List<SecurityQuestionDto> securityQuestions;

    @Data
    public static class SecurityQuestionDto {
        private String question;
        private String answer;
    }
}
