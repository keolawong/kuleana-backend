package com.kahua.registration.service;

import tools.jackson.databind.JsonNode;
import com.kahua.registration.dto.SignedAgreementRequestDto;
import com.kahua.registration.entity.AgreementTemplate;
import com.kahua.registration.entity.RegistrationSubmission;
import com.kahua.registration.entity.SignedAgreement;
import com.kahua.registration.repository.AgreementTemplateRepository;
import com.kahua.registration.repository.SignedAgreementRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Clock;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;

@Service
@RequiredArgsConstructor
public class EsignAgreementService {

    private final AgreementTemplateRepository agreementTemplateRepository;
    private final SignedAgreementRepository signedAgreementRepository;
    private final ConditionalLogicEvaluator conditionalLogicEvaluator;
    private final Clock clock;

    public String sha256Hex(String content) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest((content != null ? content : "").getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }

    public List<SignedAgreement> verifyAndPersistSignatures(
            RegistrationSubmission submission,
            JsonNode configJson,
            String eventTimezone,
            JsonNode participantProfile,
            List<SignedAgreementRequestDto> requests,
            String ipAddress,
            String userAgent) {

        List<AgreementTemplate> activeTemplates = agreementTemplateRepository
                .findByOrgIdAndIsActiveTrueOrderByTemplateKeyAscVersionDesc(submission.getOrgId());

        if (activeTemplates.isEmpty()) {
            return List.of();
        }

        boolean isMinor = resolveIsMinor(participantProfile, configJson, eventTimezone, requests);

        List<AgreementTemplate> requiredTemplates = activeTemplates.stream()
                .filter(t -> "REQUIRED".equalsIgnoreCase(t.getRequirementMode()))
                .filter(t -> matchesAgeRule(t.getAgeRule(), isMinor))
                .toList();

        Map<UUID, SignedAgreementRequestDto> byTemplateId = new HashMap<>();
        if (requests != null) {
            for (SignedAgreementRequestDto req : requests) {
                byTemplateId.put(req.getTemplateVersionId(), req);
            }
        }

        for (AgreementTemplate required : requiredTemplates) {
            if (!byTemplateId.containsKey(required.getId())) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Missing required signed agreement: " + required.getTitle());
            }
        }

        List<SignedAgreement> saved = new ArrayList<>();
        if (requests == null) {
            return saved;
        }

        Map<UUID, AgreementTemplate> templateMap = new HashMap<>();
        for (AgreementTemplate t : activeTemplates) {
            templateMap.put(t.getId(), t);
        }

        OffsetDateTime now = OffsetDateTime.now(clock);

        for (SignedAgreementRequestDto req : requests) {
            AgreementTemplate template = templateMap.get(req.getTemplateVersionId());
            if (template == null) {
                template = agreementTemplateRepository.findById(req.getTemplateVersionId())
                        .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST,
                                "Agreement template not found: " + req.getTemplateVersionId()));
            }
            if (!template.getOrgId().equals(submission.getOrgId())) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Agreement template belongs to another organization");
            }
            if (!Boolean.TRUE.equals(req.getEsignConsentAccepted())) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "ESIGN electronic consent must be accepted for: " + template.getTitle());
            }
            String expectedHash = sha256Hex(template.getBodyHtml());
            if (req.getExpectedSnapshotHash() != null && !req.getExpectedSnapshotHash().isBlank()
                    && !req.getExpectedSnapshotHash().equalsIgnoreCase(expectedHash)) {
                throw new ResponseStatusException(HttpStatus.CONFLICT,
                        "Agreement text has changed; please review and re-sign: " + template.getTitle());
            }

            boolean recordIsMinor = isMinor;
            if (req.getParticipantDob() != null && !req.getParticipantDob().isBlank()) {
                LocalDate dob = LocalDate.parse(req.getParticipantDob().trim());
                LocalDate cutoff = conditionalLogicEvaluator.resolveCutoffDate(configJson, eventTimezone);
                recordIsMinor = ChronoUnit.YEARS.between(dob, cutoff) < 18;
            }

            if (recordIsMinor) {
                if (req.getSignerRelationship() == null || req.getSignerRelationship().isBlank()) {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                            "Guardian relationship is required when signing for a minor");
                }
                if (!Boolean.TRUE.equals(req.getGuardianAuthorityAttested())) {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                            "Guardian authority attestation is required when signing for a minor");
                }
            }

            SignedAgreement entity = SignedAgreement.builder()
                    .submission(submission)
                    .participantId(req.getParticipantId())
                    .participantName(req.getParticipantName())
                    .isMinor(recordIsMinor)
                    .esignConsentAccepted(true)
                    .minorAssentName(req.getMinorAssentName())
                    .minorAssentAtUtc(req.getMinorAssentName() != null && !req.getMinorAssentName().isBlank() ? now : null)
                    .signerName(req.getSignerName())
                    .signerRelationship(req.getSignerRelationship())
                    .signatureData(req.getSignatureData())
                    .templateVersion(template)
                    .snapshotHash(expectedHash)
                    .ipAddress(ipAddress)
                    .userAgent(userAgent)
                    .signedAtUtc(now)
                    .build();

            saved.add(signedAgreementRepository.save(entity));
        }

        return saved;
    }

    private boolean resolveIsMinor(JsonNode participantProfile, JsonNode configJson, String eventTimezone,
                                   List<SignedAgreementRequestDto> requests) {
        String dobStr = participantProfile != null ? participantProfile.path("dob").asText("") : "";
        if (dobStr.isBlank() && requests != null) {
            dobStr = requests.stream()
                    .map(SignedAgreementRequestDto::getParticipantDob)
                    .filter(s -> s != null && !s.isBlank())
                    .findFirst()
                    .orElse("");
        }
        if (dobStr.isBlank()) {
            return false;
        }
        LocalDate dob = LocalDate.parse(dobStr.trim());
        LocalDate cutoff = conditionalLogicEvaluator.resolveCutoffDate(configJson, eventTimezone);
        return ChronoUnit.YEARS.between(dob, cutoff) < 18;
    }

    private boolean matchesAgeRule(String ageRule, boolean isMinor) {
        if (ageRule == null) return true;
        return switch (ageRule.toUpperCase()) {
            case "MINOR_ONLY", "<18" -> isMinor;
            case "ADULT_ONLY", "18+" -> !isMinor;
            default -> true;
        };
    }
}
