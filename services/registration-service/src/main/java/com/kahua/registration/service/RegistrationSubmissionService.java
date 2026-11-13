package com.kahua.registration.service;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import com.kahua.registration.dto.*;
import com.kahua.registration.entity.*;
import com.kahua.registration.repository.*;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.security.SecureRandom;
import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.*;

@Service
@RequiredArgsConstructor
public class RegistrationSubmissionService {

    private static final Set<String> FORBIDDEN_PUBLIC_FIELDS = Set.of(
            "adminFields", "adminFieldsJson", "paymentStatus", "status", "syncStatus", "confirmationCode"
    );
    private static final List<SubmissionStatus> QUOTA_COUNTED_STATUSES =
            List.of(SubmissionStatus.SUBMITTED, SubmissionStatus.WAITLISTED);
    private static final String CODE_CHARS = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
    private static final SecureRandom RANDOM = new SecureRandom();

    private final RegistrationFormRepository formRepository;
    private final RegistrationFormVersionRepository versionRepository;
    private final RegistrationOptionRepository optionRepository;
    private final FormAccountQuotaRepository quotaRepository;
    private final RegistrationSubmissionRepository submissionRepository;
    private final RegistrationSubmissionSelectionRepository selectionRepository;
    private final SignedAgreementRepository signedAgreementRepository;
    private final RegistrationAuditLogRepository auditLogRepository;
    private final RegistrationOutboxRepository outboxRepository;
    private final ConditionalLogicEvaluator conditionalLogicEvaluator;
    private final CutoffWindowEvaluator cutoffWindowEvaluator;
    private final EsignAgreementService esignAgreementService;
    private final CloudTasksService cloudTasksService;
    private final ObjectMapper objectMapper;
    private final Clock clock;

    @Transactional
    public PublicSubmissionResponseDto saveDraft(String slug, String currentUserId, SubmissionDraftRequestDto req) {
        rejectProtectedFields(req.getUnknownFields());

        RegistrationForm form = formRepository.findBySlugAndIsActiveTrue(slug)
                .orElseThrow(() -> new EntityNotFoundException("Registration form not found: " + slug));
        RegistrationFormVersion version = versionRepository.findTopByFormIdOrderByVersionDesc(form.getId())
                .orElseThrow(() -> new EntityNotFoundException("No published version for form: " + slug));

        CutoffWindowResultDto activeWindow = cutoffWindowEvaluator.verifyWindowForAction(
                form, version.getCutoffWindowsJson(), req.getExpectedWindowId(), "REGISTER");

        ConditionalLogicEvaluator.EvaluationResult eval = conditionalLogicEvaluator.evaluateAndSanitize(
                version.getSchemaJson(),
                version.getLogicRulesJson(),
                version.getConfigJson(),
                form.getEventTimezone(),
                req.getParticipantProfile(),
                req.getAnswers(),
                false);

        String participantKey = resolveParticipantKey(req.getParticipantKey(), currentUserId, req.getParticipantProfile());

        RegistrationSubmission submission = RegistrationSubmission.builder()
                .orgId(form.getOrgId())
                .form(form)
                .formVersion(version)
                .accountUserId(currentUserId)
                .participantKey(participantKey)
                .participantProfileJson(req.getParticipantProfile() != null ? req.getParticipantProfile() : objectMapper.createObjectNode())
                .answersJson(eval.sanitizedAnswers())
                .adminFieldsJson(objectMapper.createObjectNode())
                .appliedWindowId(activeWindow != null ? activeWindow.getWindowId() : null)
                .feeSnapshotJson(activeWindow != null && activeWindow.getFeeTier() != null
                        ? activeWindow.getFeeTier() : objectMapper.createObjectNode())
                .paymentStatus(PaymentStatus.NOT_REQUIRED)
                .syncStatus(SyncStatus.PENDING)
                .status(SubmissionStatus.DRAFT)
                .build();

        RegistrationSubmission saved = submissionRepository.save(submission);
        return toPublicDto(saved);
    }

    @Transactional
    public PublicSubmissionResponseDto submitNew(
            String slug,
            String currentUserId,
            SubmissionSubmitRequestDto req,
            String ipAddress,
            String userAgent) {

        rejectProtectedFields(req.getUnknownFields());

        RegistrationForm form = formRepository.findBySlugAndIsActiveTrue(slug)
                .orElseThrow(() -> new EntityNotFoundException("Registration form not found: " + slug));

        RegistrationFormVersion version = req.getFormVersionId() != null
                ? versionRepository.findById(req.getFormVersionId())
                    .orElseThrow(() -> new EntityNotFoundException("Form version not found: " + req.getFormVersionId()))
                : versionRepository.findTopByFormIdOrderByVersionDesc(form.getId())
                    .orElseThrow(() -> new EntityNotFoundException("No published version for form: " + slug));

        if (!version.getForm().getId().equals(form.getId())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Form version does not belong to form: " + slug);
        }

        RegistrationSubmission draft = RegistrationSubmission.builder()
                .orgId(form.getOrgId())
                .form(form)
                .formVersion(version)
                .accountUserId(currentUserId)
                .participantKey(resolveParticipantKey(req.getParticipantKey(), currentUserId, req.getParticipantProfile()))
                .participantProfileJson(req.getParticipantProfile() != null ? req.getParticipantProfile() : objectMapper.createObjectNode())
                .answersJson(objectMapper.createObjectNode())
                .adminFieldsJson(objectMapper.createObjectNode())
                .feeSnapshotJson(objectMapper.createObjectNode())
                .paymentStatus(PaymentStatus.NOT_REQUIRED)
                .syncStatus(SyncStatus.PENDING)
                .status(SubmissionStatus.DRAFT)
                .build();

        draft = submissionRepository.save(draft);
        return finalizeSubmission(draft, req, ipAddress, userAgent);
    }

    @Transactional
    public PublicSubmissionResponseDto submitExistingDraft(
            UUID submissionId,
            String currentUserId,
            SubmissionSubmitRequestDto req,
            String ipAddress,
            String userAgent) {

        rejectProtectedFields(req.getUnknownFields());

        RegistrationSubmission submission = submissionRepository.findById(submissionId)
                .orElseThrow(() -> new EntityNotFoundException("Submission not found: " + submissionId));
        enforceOwnership(submission, currentUserId);

        if (submission.getStatus() != SubmissionStatus.DRAFT) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Only DRAFT submissions can be submitted");
        }
        if (req.getParticipantProfile() != null) {
            submission.setParticipantProfileJson(req.getParticipantProfile());
        }
        if (req.getParticipantKey() != null && !req.getParticipantKey().isBlank()) {
            submission.setParticipantKey(req.getParticipantKey().trim());
        }

        return finalizeSubmission(submission, req, ipAddress, userAgent);
    }

    private PublicSubmissionResponseDto finalizeSubmission(
            RegistrationSubmission submission,
            SubmissionSubmitRequestDto req,
            String ipAddress,
            String userAgent) {

        RegistrationForm form = submission.getForm();
        RegistrationFormVersion version = submission.getFormVersion();

        CutoffWindowResultDto activeWindow = cutoffWindowEvaluator.verifyWindowForAction(
                form, version.getCutoffWindowsJson(), req.getExpectedWindowId(), "REGISTER");

        enforceQuotasWithLock(form, version, submission.getAccountUserId(), submission.getParticipantKey());

        ConditionalLogicEvaluator.EvaluationResult eval = conditionalLogicEvaluator.evaluateAndSanitize(
                version.getSchemaJson(),
                version.getLogicRulesJson(),
                version.getConfigJson(),
                form.getEventTimezone(),
                submission.getParticipantProfileJson(),
                req.getAnswers() != null ? req.getAnswers() : submission.getAnswersJson(),
                true);

        submission.setAnswersJson(eval.sanitizedAnswers());
        submission.setAppliedWindowId(activeWindow != null ? activeWindow.getWindowId() : null);
        submission.setFeeSnapshotJson(activeWindow != null && activeWindow.getFeeTier() != null
                ? activeWindow.getFeeTier() : objectMapper.createObjectNode());

        // Reserve capacity atomically
        List<UUID> optionIds = req.getSelectedOptionIds() != null ? req.getSelectedOptionIds() : List.of();
        boolean anyWaitlisted = false;
        Set<String> allowedOptionIds = activeWindow != null && activeWindow.getAvailableOptionIds() != null
                ? Set.copyOf(activeWindow.getAvailableOptionIds())
                : null;

        for (UUID optionId : optionIds) {
            RegistrationOption option = optionRepository.findById(optionId)
                    .orElseThrow(() -> new EntityNotFoundException("Registration option not found: " + optionId));
            if (!option.getFormVersion().getId().equals(version.getId())) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Option does not belong to active form version");
            }
            if (allowedOptionIds != null
                    && !allowedOptionIds.contains(option.getId().toString())
                    && (option.getExternalRefId() == null || !allowedOptionIds.contains(option.getExternalRefId()))) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "Option is not available in the current registration window: " + option.getLabel());
            }

            int reserved = optionRepository.reserveCapacity(optionId);
            SelectionStatus selStatus;
            if (reserved > 0) {
                selStatus = SelectionStatus.ACTIVE;
            } else if (Boolean.TRUE.equals(req.getWaitlistOnFull())) {
                selStatus = SelectionStatus.WAITLISTED;
                anyWaitlisted = true;
            } else {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "Option is full: " + option.getLabel());
            }

            selectionRepository.save(RegistrationSubmissionSelection.builder()
                    .submission(submission)
                    .option(option)
                    .status(selStatus)
                    .build());
        }

        // Verify & persist ESIGN agreements
        esignAgreementService.verifyAndPersistSignatures(
                submission,
                version.getConfigJson(),
                form.getEventTimezone(),
                submission.getParticipantProfileJson(),
                req.getSignedAgreements(),
                ipAddress,
                userAgent);

        OffsetDateTime now = OffsetDateTime.now(clock);
        submission.setStatus(anyWaitlisted && optionIds.size() == 1 ? SubmissionStatus.WAITLISTED : SubmissionStatus.SUBMITTED);
        submission.setSubmittedAtUtc(now);
        if (submission.getConfirmationCode() == null) {
            submission.setConfirmationCode(generateConfirmationCode());
        }
        RegistrationSubmission saved = submissionRepository.save(submission);

        writeAuditLog(saved.getOrgId(), saved.getId(), saved.getAccountUserId(), "SUBMIT", null, objectMapper.valueToTree(toPublicDto(saved)));
        enqueueOutbox(saved, "SUBMISSION_CONFIRMED");

        return toPublicDto(saved);
    }

    @Transactional
    public PublicSubmissionResponseDto updateSubmission(
            UUID submissionId,
            String currentUserId,
            SubmissionDraftRequestDto req) {

        rejectProtectedFields(req.getUnknownFields());

        RegistrationSubmission submission = submissionRepository.findById(submissionId)
                .orElseThrow(() -> new EntityNotFoundException("Submission not found: " + submissionId));
        enforceOwnership(submission, currentUserId);

        if (submission.getStatus() == SubmissionStatus.CANCELLED) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Cancelled submissions cannot be edited");
        }

        RegistrationForm form = submission.getForm();
        RegistrationFormVersion version = submission.getFormVersion();

        if (submission.getStatus() != SubmissionStatus.DRAFT) {
            cutoffWindowEvaluator.verifyWindowForAction(form, version.getCutoffWindowsJson(), req.getExpectedWindowId(), "EDIT");
        }

        JsonNode before = objectMapper.valueToTree(toPublicDto(submission));
        if (req.getParticipantProfile() != null) {
            submission.setParticipantProfileJson(req.getParticipantProfile());
        }

        ConditionalLogicEvaluator.EvaluationResult eval = conditionalLogicEvaluator.evaluateAndSanitize(
                version.getSchemaJson(),
                version.getLogicRulesJson(),
                version.getConfigJson(),
                form.getEventTimezone(),
                submission.getParticipantProfileJson(),
                req.getAnswers() != null ? req.getAnswers() : submission.getAnswersJson(),
                submission.getStatus() != SubmissionStatus.DRAFT);

        submission.setAnswersJson(eval.sanitizedAnswers());
        RegistrationSubmission saved = submissionRepository.save(submission);

        if (saved.getStatus() != SubmissionStatus.DRAFT) {
            writeAuditLog(saved.getOrgId(), saved.getId(), currentUserId, "UPDATE", before, objectMapper.valueToTree(toPublicDto(saved)));
            enqueueOutbox(saved, "SUBMISSION_UPDATED");
        }

        return toPublicDto(saved);
    }

    @Transactional
    public PublicSubmissionResponseDto swapOption(
            UUID submissionId,
            String currentUserId,
            OptionSwapRequestDto req) {

        RegistrationSubmission submission = submissionRepository.findById(submissionId)
                .orElseThrow(() -> new EntityNotFoundException("Submission not found: " + submissionId));
        enforceOwnership(submission, currentUserId);

        if (submission.getStatus() != SubmissionStatus.SUBMITTED && submission.getStatus() != SubmissionStatus.WAITLISTED) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Only active submissions can swap options");
        }

        RegistrationForm form = submission.getForm();
        RegistrationFormVersion version = submission.getFormVersion();
        cutoffWindowEvaluator.verifyWindowForAction(form, version.getCutoffWindowsJson(), req.getExpectedWindowId(), "SWAP");

        RegistrationSubmissionSelection oldSelection = selectionRepository
                .findBySubmissionIdAndOptionIdAndStatus(submissionId, req.getOldOptionId(), SelectionStatus.ACTIVE)
                .orElseThrow(() -> new EntityNotFoundException("Active selection not found for option: " + req.getOldOptionId()));

        RegistrationOption newOption = optionRepository.findById(req.getNewOptionId())
                .orElseThrow(() -> new EntityNotFoundException("New option not found: " + req.getNewOptionId()));
        if (!newOption.getFormVersion().getId().equals(version.getId())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "New option does not belong to form version");
        }

        int reserved = optionRepository.reserveCapacity(newOption.getId());
        if (reserved == 0) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "New option is at full capacity: " + newOption.getLabel());
        }

        oldSelection.setStatus(SelectionStatus.CANCELLED);
        selectionRepository.save(oldSelection);
        optionRepository.releaseCapacity(req.getOldOptionId());

        selectionRepository.save(RegistrationSubmissionSelection.builder()
                .submission(submission)
                .option(newOption)
                .status(SelectionStatus.ACTIVE)
                .build());

        writeAuditLog(submission.getOrgId(), submission.getId(), currentUserId, "SWAP_OPTION", null,
                objectMapper.valueToTree(Map.of("oldOptionId", req.getOldOptionId(), "newOptionId", req.getNewOptionId())));
        enqueueOutbox(submission, "SUBMISSION_SWAPPED");

        return toPublicDto(submission);
    }

    @Transactional
    public PublicSubmissionResponseDto cancelSubmission(UUID submissionId, String currentUserId) {
        RegistrationSubmission submission = submissionRepository.findById(submissionId)
                .orElseThrow(() -> new EntityNotFoundException("Submission not found: " + submissionId));
        enforceOwnership(submission, currentUserId);

        if (submission.getStatus() == SubmissionStatus.CANCELLED) {
            return toPublicDto(submission);
        }

        RegistrationForm form = submission.getForm();
        RegistrationFormVersion version = submission.getFormVersion();
        if (submission.getStatus() != SubmissionStatus.DRAFT) {
            cutoffWindowEvaluator.verifyWindowForAction(form, version.getCutoffWindowsJson(), null, "CANCEL");
        }

        List<RegistrationSubmissionSelection> activeSelections = selectionRepository
                .findBySubmissionIdAndStatus(submissionId, SelectionStatus.ACTIVE);
        for (RegistrationSubmissionSelection sel : activeSelections) {
            sel.setStatus(SelectionStatus.CANCELLED);
            selectionRepository.save(sel);
            optionRepository.releaseCapacity(sel.getOption().getId());
        }

        submission.setStatus(SubmissionStatus.CANCELLED);
        submission.setCancelledAtUtc(OffsetDateTime.now(clock));
        RegistrationSubmission saved = submissionRepository.save(submission);

        writeAuditLog(saved.getOrgId(), saved.getId(), currentUserId, "CANCEL", null, null);
        enqueueOutbox(saved, "SUBMISSION_CANCELLED");

        return toPublicDto(saved);
    }

    @Transactional(readOnly = true)
    public List<PublicSubmissionResponseDto> getMySubmissions(String currentUserId) {
        return submissionRepository.findByAccountUserIdOrderByCreatedAtUtcDesc(currentUserId).stream()
                .map(this::toPublicDto)
                .toList();
    }

    @Transactional(readOnly = true)
    public PublicSubmissionResponseDto getMySubmission(UUID submissionId, String currentUserId) {
        RegistrationSubmission submission = submissionRepository.findById(submissionId)
                .orElseThrow(() -> new EntityNotFoundException("Submission not found: " + submissionId));
        enforceOwnership(submission, currentUserId);
        return toPublicDto(submission);
    }

    @Transactional(readOnly = true)
    public List<AdminSubmissionResponseDto> listAdminSubmissions(UUID orgId, UUID formId) {
        List<RegistrationSubmission> list = formId != null
                ? submissionRepository.findByOrgIdAndFormIdOrderByCreatedAtUtcDesc(orgId, formId)
                : submissionRepository.findByOrgIdOrderByCreatedAtUtcDesc(orgId);
        return list.stream().map(this::toAdminDto).toList();
    }

    @Transactional(readOnly = true)
    public AdminSubmissionResponseDto getAdminSubmission(UUID orgId, UUID submissionId) {
        RegistrationSubmission submission = submissionRepository.findById(submissionId)
                .orElseThrow(() -> new EntityNotFoundException("Submission not found: " + submissionId));
        if (!submission.getOrgId().equals(orgId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Submission belongs to another organization");
        }
        return toAdminDto(submission);
    }

    @Transactional
    public AdminSubmissionResponseDto patchAdminSubmission(
            UUID orgId,
            UUID submissionId,
            String adminUserId,
            AdminSubmissionPatchRequestDto req) {

        RegistrationSubmission submission = submissionRepository.findById(submissionId)
                .orElseThrow(() -> new EntityNotFoundException("Submission not found: " + submissionId));
        if (!submission.getOrgId().equals(orgId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Submission belongs to another organization");
        }

        JsonNode before = objectMapper.valueToTree(toAdminDto(submission));
        if (req.getAdminFieldsJson() != null) {
            submission.setAdminFieldsJson(req.getAdminFieldsJson());
        }
        if (req.getPaymentStatus() != null) {
            submission.setPaymentStatus(req.getPaymentStatus());
        }
        RegistrationSubmission saved = submissionRepository.save(submission);
        writeAuditLog(orgId, saved.getId(), adminUserId, "ADMIN_PATCH", before, objectMapper.valueToTree(toAdminDto(saved)));
        return toAdminDto(saved);
    }

    private void enforceQuotasWithLock(
            RegistrationForm form,
            RegistrationFormVersion version,
            String accountUserId,
            String participantKey) {

        FormAccountQuota quota = quotaRepository.findForUpdate(form.getId(), accountUserId)
                .orElseGet(() -> quotaRepository.saveAndFlush(FormAccountQuota.builder()
                        .form(form)
                        .accountUserId(accountUserId)
                        .build()));

        Integer configMaxPerAccount = version.getConfigJson() != null && version.getConfigJson().hasNonNull("maxPerAccount")
                ? version.getConfigJson().get("maxPerAccount").asInt()
                : null;
        Integer effectiveMax = quota.getOverrideMaxSubmissions() != null
                ? quota.getOverrideMaxSubmissions()
                : configMaxPerAccount;

        if (effectiveMax != null) {
            long currentCount = submissionRepository.countByFormIdAndAccountUserIdAndStatusIn(
                    form.getId(), accountUserId, QUOTA_COUNTED_STATUSES);
            if (currentCount >= effectiveMax) {
                throw new ResponseStatusException(HttpStatus.CONFLICT,
                        "Account registration limit reached (" + currentCount + " of " + effectiveMax + " used)");
            }
        }

        Integer maxPerParticipant = version.getConfigJson() != null && version.getConfigJson().hasNonNull("maxPerParticipant")
                ? version.getConfigJson().get("maxPerParticipant").asInt()
                : null;
        if (maxPerParticipant != null) {
            long partCount = submissionRepository.countByFormIdAndParticipantKeyAndStatusIn(
                    form.getId(), participantKey, QUOTA_COUNTED_STATUSES);
            if (partCount >= maxPerParticipant) {
                throw new ResponseStatusException(HttpStatus.CONFLICT,
                        "Participant registration limit reached (" + partCount + " of " + maxPerParticipant + " used)");
            }
        }
    }

    private void enforceOwnership(RegistrationSubmission submission, String currentUserId) {
        if (currentUserId == null || !submission.getAccountUserId().equals(currentUserId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You do not have permission to access this submission");
        }
    }

    private void rejectProtectedFields(Map<String, Object> unknownFields) {
        if (unknownFields == null || unknownFields.isEmpty()) return;
        for (String key : unknownFields.keySet()) {
            if (FORBIDDEN_PUBLIC_FIELDS.contains(key)) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Protected field '" + key + "' cannot be set on public endpoints");
            }
        }
    }

    private String resolveParticipantKey(String explicitKey, String currentUserId, JsonNode profile) {
        if (explicitKey != null && !explicitKey.isBlank()) {
            return explicitKey.trim();
        }
        String first = profile != null ? profile.path("firstName").asText("").trim() : "";
        String last = profile != null ? profile.path("lastName").asText("").trim() : "";
        String dob = profile != null ? profile.path("dob").asText("").trim() : "";
        if (!first.isEmpty() || !last.isEmpty()) {
            return (currentUserId + ":" + first + ":" + last + ":" + dob).toLowerCase();
        }
        return currentUserId;
    }

    private void writeAuditLog(UUID orgId, UUID submissionId, String actor, String action, JsonNode before, JsonNode after) {
        auditLogRepository.save(RegistrationAuditLog.builder()
                .orgId(orgId)
                .submissionId(submissionId)
                .actorUserId(actor)
                .action(action)
                .beforeJson(before)
                .afterJson(after)
                .build());
    }

    private void enqueueOutbox(RegistrationSubmission submission, String eventType) {
        RegistrationOutbox outbox = outboxRepository.save(RegistrationOutbox.builder()
                .orgId(submission.getOrgId())
                .submissionId(submission.getId())
                .eventType(eventType)
                .payloadJson(objectMapper.valueToTree(Map.of(
                        "submissionId", submission.getId(),
                        "formId", submission.getForm().getId(),
                        "status", submission.getStatus().name()
                )))
                .status(OutboxStatus.PENDING)
                .nextAttemptAtUtc(OffsetDateTime.now(clock))
                .build());
        cloudTasksService.enqueueOutboxTask(outbox.getId());
    }

    private String generateConfirmationCode() {
        StringBuilder sb = new StringBuilder("REG-");
        for (int i = 0; i < 6; i++) {
            sb.append(CODE_CHARS.charAt(RANDOM.nextInt(CODE_CHARS.length())));
        }
        return sb.toString();
    }

    private PublicSubmissionResponseDto toPublicDto(RegistrationSubmission s) {
        return PublicSubmissionResponseDto.builder()
                .id(s.getId())
                .orgId(s.getOrgId())
                .formId(s.getForm().getId())
                .formVersionId(s.getFormVersion().getId())
                .formSlug(s.getForm().getSlug())
                .formTitle(s.getForm().getTitle())
                .accountUserId(s.getAccountUserId())
                .participantKey(s.getParticipantKey())
                .participantProfileJson(s.getParticipantProfileJson())
                .answersJson(s.getAnswersJson())
                .appliedWindowId(s.getAppliedWindowId())
                .feeSnapshotJson(s.getFeeSnapshotJson())
                .paymentStatus(s.getPaymentStatus())
                .status(s.getStatus())
                .confirmationCode(s.getConfirmationCode())
                .selections(mapSelections(s.getId()))
                .signedAgreements(mapAgreements(s.getId()))
                .createdAtUtc(s.getCreatedAtUtc())
                .updatedAtUtc(s.getUpdatedAtUtc())
                .submittedAtUtc(s.getSubmittedAtUtc())
                .cancelledAtUtc(s.getCancelledAtUtc())
                .build();
    }

    private AdminSubmissionResponseDto toAdminDto(RegistrationSubmission s) {
        return AdminSubmissionResponseDto.builder()
                .id(s.getId())
                .orgId(s.getOrgId())
                .formId(s.getForm().getId())
                .formVersionId(s.getFormVersion().getId())
                .accountUserId(s.getAccountUserId())
                .participantKey(s.getParticipantKey())
                .participantProfileJson(s.getParticipantProfileJson())
                .answersJson(s.getAnswersJson())
                .adminFieldsJson(s.getAdminFieldsJson())
                .appliedWindowId(s.getAppliedWindowId())
                .feeSnapshotJson(s.getFeeSnapshotJson())
                .paymentStatus(s.getPaymentStatus())
                .syncStatus(s.getSyncStatus())
                .status(s.getStatus())
                .confirmationCode(s.getConfirmationCode())
                .selections(mapSelections(s.getId()))
                .signedAgreements(mapAgreements(s.getId()))
                .createdAtUtc(s.getCreatedAtUtc())
                .updatedAtUtc(s.getUpdatedAtUtc())
                .submittedAtUtc(s.getSubmittedAtUtc())
                .cancelledAtUtc(s.getCancelledAtUtc())
                .build();
    }

    private List<SelectionResponseDto> mapSelections(UUID submissionId) {
        return selectionRepository.findBySubmissionId(submissionId).stream()
                .map(sel -> SelectionResponseDto.builder()
                        .id(sel.getId())
                        .optionId(sel.getOption().getId())
                        .externalRefId(sel.getOption().getExternalRefId())
                        .optionType(sel.getOption().getOptionType())
                        .label(sel.getOption().getLabel())
                        .status(sel.getStatus())
                        .build())
                .toList();
    }

    private List<SignedAgreementResponseDto> mapAgreements(UUID submissionId) {
        return signedAgreementRepository.findBySubmissionId(submissionId).stream()
                .map(sa -> SignedAgreementResponseDto.builder()
                        .id(sa.getId())
                        .templateVersionId(sa.getTemplateVersion().getId())
                        .participantId(sa.getParticipantId())
                        .participantName(sa.getParticipantName())
                        .isMinor(sa.getIsMinor())
                        .esignConsentAccepted(sa.getEsignConsentAccepted())
                        .minorAssentName(sa.getMinorAssentName())
                        .minorAssentAtUtc(sa.getMinorAssentAtUtc())
                        .signerName(sa.getSignerName())
                        .signerRelationship(sa.getSignerRelationship())
                        .snapshotHash(sa.getSnapshotHash())
                        .signedAtUtc(sa.getSignedAtUtc())
                        .build())
                .toList();
    }
}
