package com.kahua.registration.controller;

import com.kahua.registration.dto.*;
import com.kahua.registration.entity.AgreementTemplate;
import com.kahua.registration.entity.FormAccountQuota;
import com.kahua.registration.entity.RegistrationForm;
import com.kahua.registration.entity.RegistrationFormVersion;
import com.kahua.registration.service.RegistrationFormService;
import com.kahua.registration.service.RegistrationSubmissionService;
import com.kuleanasystems.security.TenantSecurityUtil;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/registration/admin")
@PreAuthorize("hasAnyRole('ADMIN', 'SUPERADMIN')")
@RequiredArgsConstructor
public class AdminRegistrationController {

    private final RegistrationFormService formService;
    private final RegistrationSubmissionService submissionService;

    @GetMapping("/forms")
    public ResponseEntity<List<RegistrationForm>> listForms(
            @RequestHeader(value = "X-Org-Id", required = false) String orgIdHeader,
            Authentication auth) {
        UUID orgId = resolveRequiredOrgId(orgIdHeader, auth);
        return ResponseEntity.ok(formService.listFormsByOrg(orgId));
    }

    @PostMapping("/forms")
    public ResponseEntity<RegistrationForm> createForm(
            @RequestHeader(value = "X-Org-Id", required = false) String orgIdHeader,
            @Valid @RequestBody AdminFormCreateRequestDto request,
            Authentication auth) {
        String targetOrg = request.getOrgId() != null ? request.getOrgId().toString() : orgIdHeader;
        UUID orgId = resolveRequiredOrgId(targetOrg, auth);
        return ResponseEntity.status(HttpStatus.CREATED).body(formService.createForm(orgId, request));
    }

    @PostMapping("/forms/{formId}/versions")
    public ResponseEntity<RegistrationFormVersion> createFormVersion(
            @PathVariable UUID formId,
            @RequestHeader(value = "X-Org-Id", required = false) String orgIdHeader,
            @Valid @RequestBody AdminFormVersionCreateRequestDto request,
            Authentication auth) {
        UUID orgId = resolveRequiredOrgId(orgIdHeader, auth);
        return ResponseEntity.status(HttpStatus.CREATED).body(formService.createFormVersion(orgId, formId, request));
    }

    @PutMapping("/quotas/{formId}")
    public ResponseEntity<FormAccountQuota> upsertQuota(
            @PathVariable UUID formId,
            @RequestHeader(value = "X-Org-Id", required = false) String orgIdHeader,
            @Valid @RequestBody AdminQuotaUpsertRequestDto request,
            Authentication auth) {
        String targetOrg = request.getOrgId() != null ? request.getOrgId().toString() : orgIdHeader;
        UUID orgId = resolveRequiredOrgId(targetOrg, auth);
        return ResponseEntity.ok(formService.upsertAccountQuota(orgId, formId, request));
    }

    @GetMapping("/agreements")
    public ResponseEntity<List<AgreementTemplate>> listAgreements(
            @RequestHeader(value = "X-Org-Id", required = false) String orgIdHeader,
            Authentication auth) {
        UUID orgId = resolveRequiredOrgId(orgIdHeader, auth);
        return ResponseEntity.ok(formService.listAgreementsByOrg(orgId));
    }

    @PostMapping("/agreements")
    public ResponseEntity<AgreementTemplate> createAgreement(
            @RequestHeader(value = "X-Org-Id", required = false) String orgIdHeader,
            @Valid @RequestBody AdminAgreementCreateRequestDto request,
            Authentication auth) {
        String targetOrg = request.getOrgId() != null ? request.getOrgId().toString() : orgIdHeader;
        UUID orgId = resolveRequiredOrgId(targetOrg, auth);
        return ResponseEntity.status(HttpStatus.CREATED).body(formService.createAgreementVersion(orgId, request));
    }

    @GetMapping("/submissions")
    public ResponseEntity<List<AdminSubmissionResponseDto>> listSubmissions(
            @RequestHeader(value = "X-Org-Id", required = false) String orgIdHeader,
            @RequestParam(required = false) UUID formId,
            Authentication auth) {
        UUID orgId = resolveRequiredOrgId(orgIdHeader, auth);
        return ResponseEntity.ok(submissionService.listAdminSubmissions(orgId, formId));
    }

    @GetMapping("/submissions/{id}")
    public ResponseEntity<AdminSubmissionResponseDto> getSubmission(
            @PathVariable UUID id,
            @RequestHeader(value = "X-Org-Id", required = false) String orgIdHeader,
            Authentication auth) {
        UUID orgId = resolveRequiredOrgId(orgIdHeader, auth);
        return ResponseEntity.ok(submissionService.getAdminSubmission(orgId, id));
    }

    @PatchMapping("/submissions/{id}")
    public ResponseEntity<AdminSubmissionResponseDto> patchSubmission(
            @PathVariable UUID id,
            @RequestHeader(value = "X-Org-Id", required = false) String orgIdHeader,
            @Valid @RequestBody AdminSubmissionPatchRequestDto request,
            Authentication auth) {
        UUID orgId = resolveRequiredOrgId(orgIdHeader, auth);
        return ResponseEntity.ok(submissionService.patchAdminSubmission(orgId, id, auth.getName(), request));
    }

    private UUID resolveRequiredOrgId(String orgIdHeader, Authentication auth) {
        UUID orgId = TenantSecurityUtil.resolveEffectiveOrgId(orgIdHeader, auth);
        if (orgId == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "X-Org-Id header is required");
        }
        return orgId;
    }
}
