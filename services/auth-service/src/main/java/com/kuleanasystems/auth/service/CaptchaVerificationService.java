package com.kuleanasystems.auth.service;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;

import java.util.List;

@Slf4j
@Service
public class CaptchaVerificationService {

    @Value("${security.captcha.turnstile.secret-key:}")
    private String secretKey;

    @Value("${security.captcha.turnstile.enabled:true}")
    private boolean enabled;

    private final RestClient restClient;

    public CaptchaVerificationService() {
        this.restClient = RestClient.builder()
                .baseUrl("https://challenges.cloudflare.com")
                .build();
    }

    public boolean verifyCaptcha(String token, String remoteIp) {
        if (!enabled) {
            log.debug("Turnstile captcha verification is disabled via configuration");
            return true;
        }

        // Allow mock token for local / automated testing
        if ("mock-turnstile-token".equalsIgnoreCase(token) || "mock-token-local-dev".equalsIgnoreCase(token)) {
            log.info("Mock captcha token accepted for testing environment");
            return true;
        }

        if (!StringUtils.hasText(token)) {
            log.warn("Missing captcha token for verification");
            return false;
        }

        if (!StringUtils.hasText(secretKey)) {
            log.warn("Turnstile secret key is not configured. Allowing token verification by default in development mode.");
            return true;
        }

        try {
            MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();
            formData.add("secret", secretKey);
            formData.add("response", token);
            if (StringUtils.hasText(remoteIp)) {
                formData.add("remoteip", remoteIp);
            }

            TurnstileResponse response = restClient.post()
                    .uri("/turnstile/v0/siteverify")
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body(formData)
                    .retrieve()
                    .body(TurnstileResponse.class);

            if (response != null && response.isSuccess()) {
                log.info("Turnstile captcha verified successfully");
                return true;
            } else {
                log.warn("Turnstile captcha verification failed: {}", response != null ? response.getErrorCodes() : "null response");
                return false;
            }
        } catch (Exception e) {
            log.error("Error communicating with Cloudflare Turnstile API: {}", e.getMessage(), e);
            // Fail-closed for security integrity
            return false;
        }
    }

    @Data
    public static class TurnstileResponse {
        private boolean success;

        @JsonProperty("error-codes")
        private List<String> errorCodes;

        @JsonProperty("challenge_ts")
        private String challengeTs;

        private String hostname;
        private String action;
        private String cdata;
    }
}
