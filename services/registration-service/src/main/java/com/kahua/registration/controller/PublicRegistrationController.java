package com.kahua.registration.controller;

import com.kahua.registration.dto.*;
import com.kahua.registration.service.RegistrationFormService;
import com.kahua.registration.service.RegistrationSubmissionService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/registration")
@RequiredArgsConstructor
public class PublicRegistrationController {

    private final RegistrationFormService formService;
    private final RegistrationSubmissionService submissionService;

    @GetMapping("/forms/{slug}")
    public ResponseEntity<PublicFormResponseDto> getFormBySlug(
            @PathVariable String slug,
            Authentication auth) {
        boolean isAdminPreview = auth != null && auth.isAuthenticated() && auth.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch(a -> a.equalsIgnoreCase("ROLE_ADMIN") || a.equalsIgnoreCase("ROLE_SUPERADMIN"));
        return ResponseEntity.ok(formService.getPublicFormBySlug(slug, isAdminPreview));
    }

    @PostMapping("/forms/{slug}/drafts")
    public ResponseEntity<PublicSubmissionResponseDto> saveDraft(
            @PathVariable String slug,
            @Valid @RequestBody SubmissionDraftRequestDto request,
            Authentication auth) {
        String userId = requireAuthenticatedUser(auth);
        return ResponseEntity.status(HttpStatus.CREATED).body(submissionService.saveDraft(slug, userId, request));
    }

    @PostMapping("/forms/{slug}/submit")
    public ResponseEntity<PublicSubmissionResponseDto> submitNew(
            @PathVariable String slug,
            @Valid @RequestBody SubmissionSubmitRequestDto request,
            Authentication auth,
            HttpServletRequest httpRequest) {
        String userId = requireAuthenticatedUser(auth);
        return ResponseEntity.status(HttpStatus.CREATED).body(submissionService.submitNew(
                slug, userId, request, extractClientIp(httpRequest), httpRequest.getHeader("User-Agent")));
    }

    @PostMapping("/submissions/{id}/submit")
    public ResponseEntity<PublicSubmissionResponseDto> submitDraft(
            @PathVariable UUID id,
            @Valid @RequestBody SubmissionSubmitRequestDto request,
            Authentication auth,
            HttpServletRequest httpRequest) {
        String userId = requireAuthenticatedUser(auth);
        return ResponseEntity.ok(submissionService.submitExistingDraft(
                id, userId, request, extractClientIp(httpRequest), httpRequest.getHeader("User-Agent")));
    }

    @PutMapping("/submissions/{id}")
    public ResponseEntity<PublicSubmissionResponseDto> updateSubmission(
            @PathVariable UUID id,
            @Valid @RequestBody SubmissionDraftRequestDto request,
            Authentication auth) {
        String userId = requireAuthenticatedUser(auth);
        return ResponseEntity.ok(submissionService.updateSubmission(id, userId, request));
    }

    @PostMapping("/submissions/{id}/swap")
    public ResponseEntity<PublicSubmissionResponseDto> swapOption(
            @PathVariable UUID id,
            @Valid @RequestBody OptionSwapRequestDto request,
            Authentication auth) {
        String userId = requireAuthenticatedUser(auth);
        return ResponseEntity.ok(submissionService.swapOption(id, userId, request));
    }

    @PostMapping("/submissions/{id}/cancel")
    public ResponseEntity<PublicSubmissionResponseDto> cancelSubmission(
            @PathVariable UUID id,
            Authentication auth) {
        String userId = requireAuthenticatedUser(auth);
        return ResponseEntity.ok(submissionService.cancelSubmission(id, userId));
    }

    @GetMapping("/my-submissions")
    public ResponseEntity<List<PublicSubmissionResponseDto>> getMySubmissions(Authentication auth) {
        String userId = requireAuthenticatedUser(auth);
        return ResponseEntity.ok(submissionService.getMySubmissions(userId));
    }

    @GetMapping("/my-submissions/{id}")
    public ResponseEntity<PublicSubmissionResponseDto> getMySubmission(
            @PathVariable UUID id,
            Authentication auth) {
        String userId = requireAuthenticatedUser(auth);
        return ResponseEntity.ok(submissionService.getMySubmission(id, userId));
    }

    private String requireAuthenticatedUser(Authentication auth) {
        if (auth == null || !auth.isAuthenticated() || auth.getName() == null || auth.getName().isBlank()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Authentication required");
        }
        return auth.getName();
    }

    private String extractClientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
