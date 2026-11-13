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

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class RegistrationFormService {

    private final RegistrationFormRepository formRepository;
    private final RegistrationFormVersionRepository versionRepository;
    private final RegistrationOptionRepository optionRepository;
    private final AgreementTemplateRepository agreementTemplateRepository;
    private final FormAccountQuotaRepository quotaRepository;
    private final ConditionalLogicEvaluator conditionalLogicEvaluator;
    private final CutoffWindowEvaluator cutoffWindowEvaluator;
    private final EsignAgreementService esignAgreementService;
    private final ObjectMapper objectMapper;

    @Transactional(readOnly = true)
    public PublicFormResponseDto getPublicFormBySlug(String slug, boolean isAdminPreview) {
        RegistrationForm form = formRepository.findBySlugAndIsActiveTrue(slug)
                .orElseThrow(() -> new EntityNotFoundException("Registration form not found: " + slug));

        boolean live = cutoffWindowEvaluator.isFormLive(form);
        if (!live && !isAdminPreview) {
            return PublicFormResponseDto.builder()
                    .id(form.getId())
                    .orgId(form.getOrgId())
                    .slug(form.getSlug())
                    .title(form.getTitle())
                    .description(form.getDescription())
                    .formMode(form.getFormMode())
                    .eventTimezone(form.getEventTimezone())
                    .goLiveAtUtc(form.getGoLiveAtUtc())
                    .live(false)
                    .build();
        }

        RegistrationFormVersion latest = versionRepository.findTopByFormIdOrderByVersionDesc(form.getId())
                .orElseThrow(() -> new EntityNotFoundException("No published version for form: " + slug));

        CutoffWindowResultDto activeWindow = cutoffWindowEvaluator.resolveActiveWindow(latest.getCutoffWindowsJson());
        List<RegistrationOption> allOptions = optionRepository.findByFormVersionId(latest.getId());
        Set<String> allowedIds = activeWindow != null && activeWindow.getAvailableOptionIds() != null
                ? Set.copyOf(activeWindow.getAvailableOptionIds())
                : null;

        List<PublicOptionDto> optionDtos = allOptions.stream()
                .filter(opt -> allowedIds == null || allowedIds.contains(opt.getId().toString())
                        || (opt.getExternalRefId() != null && allowedIds.contains(opt.getExternalRefId())))
                .map(opt -> PublicOptionDto.builder()
                        .id(opt.getId())
                        .externalRefId(opt.getExternalRefId())
                        .optionType(opt.getOptionType())
                        .label(opt.getLabel())
                        .metadataJson(opt.getMetadataJson())
                        .maxCapacity(opt.getMaxCapacity())
                        .reservedCount(opt.getReservedCount())
                        .spotsRemaining(opt.getMaxCapacity() != null ? Math.max(0, opt.getMaxCapacity() - opt.getReservedCount()) : null)
                        .build())
                .toList();

        List<AgreementTemplateDto> agreements = agreementTemplateRepository
                .findByOrgIdAndIsActiveTrueOrderByTemplateKeyAscVersionDesc(form.getOrgId())
                .stream()
                .filter(a -> !"DISABLED".equalsIgnoreCase(a.getRequirementMode()))
                .map(a -> AgreementTemplateDto.builder()
                        .id(a.getId())
                        .templateKey(a.getTemplateKey())
                        .version(a.getVersion())
                        .type(a.getType())
                        .ageRule(a.getAgeRule())
                        .requirementMode(a.getRequirementMode())
                        .title(a.getTitle())
                        .summaryBulletsJson(a.getSummaryBulletsJson())
                        .bodyHtml(a.getBodyHtml())
                        .contentHash(a.getContentHash())
                        .build())
                .toList();

        JsonNode sanitizedSchema = isAdminPreview
                ? latest.getSchemaJson()
                : conditionalLogicEvaluator.stripAdminOnlyFieldsFromSchema(latest.getSchemaJson());

        return PublicFormResponseDto.builder()
                .id(form.getId())
                .orgId(form.getOrgId())
                .slug(form.getSlug())
                .title(form.getTitle())
                .description(form.getDescription())
                .formMode(form.getFormMode())
                .eventTimezone(form.getEventTimezone())
                .goLiveAtUtc(form.getGoLiveAtUtc())
                .live(live)
                .activeVersionId(latest.getId())
                .activeVersion(latest.getVersion())
                .schemaJson(sanitizedSchema)
                .logicRulesJson(latest.getLogicRulesJson())
                .configJson(latest.getConfigJson())
                .activeWindow(activeWindow)
                .options(optionDtos)
                .agreements(agreements)
                .build();
    }

    @Transactional
    public RegistrationForm createForm(UUID orgId, AdminFormCreateRequestDto req) {
        if (formRepository.findByOrgIdAndSlug(orgId, req.getSlug()).isPresent()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Slug already exists in organization: " + req.getSlug());
        }
        RegistrationForm form = RegistrationForm.builder()
                .orgId(orgId)
                .slug(req.getSlug().trim())
                .title(req.getTitle().trim())
                .description(req.getDescription())
                .formMode(req.getFormMode())
                .externalEventId(req.getExternalEventId())
                .eventTimezone(req.getEventTimezone() != null && !req.getEventTimezone().isBlank() ? req.getEventTimezone() : "America/Denver")
                .goLiveAtUtc(req.getGoLiveAtUtc())
                .isActive(req.getIsActive() != null ? req.getIsActive() : true)
                .build();
        return formRepository.save(form);
    }

    @Transactional
    public RegistrationFormVersion createFormVersion(UUID orgId, UUID formId, AdminFormVersionCreateRequestDto req) {
        RegistrationForm form = formRepository.findById(formId)
                .orElseThrow(() -> new EntityNotFoundException("Form not found: " + formId));
        if (!form.getOrgId().equals(orgId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Form belongs to another organization");
        }

        int nextVersion = versionRepository.findTopByFormIdOrderByVersionDesc(formId)
                .map(v -> v.getVersion() + 1)
                .orElse(1);

        RegistrationFormVersion version = RegistrationFormVersion.builder()
                .form(form)
                .version(nextVersion)
                .schemaJson(req.getSchemaJson() != null ? req.getSchemaJson() : objectMapper.createObjectNode())
                .logicRulesJson(req.getLogicRulesJson() != null ? req.getLogicRulesJson() : objectMapper.createArrayNode())
                .configJson(req.getConfigJson() != null ? req.getConfigJson() : objectMapper.createObjectNode())
                .cutoffWindowsJson(req.getCutoffWindowsJson() != null ? req.getCutoffWindowsJson() : objectMapper.createArrayNode())
                .build();

        RegistrationFormVersion savedVersion = versionRepository.save(version);

        if (req.getOptions() != null) {
            List<RegistrationOption> options = new ArrayList<>();
            for (AdminOptionCreateRequestDto optDto : req.getOptions()) {
                options.add(RegistrationOption.builder()
                        .formVersion(savedVersion)
                        .externalRefId(optDto.getExternalRefId())
                        .optionType(optDto.getOptionType())
                        .label(optDto.getLabel())
                        .metadataJson(optDto.getMetadataJson() != null ? optDto.getMetadataJson() : objectMapper.createObjectNode())
                        .maxCapacity(optDto.getMaxCapacity())
                        .reservedCount(0)
                        .build());
            }
            optionRepository.saveAll(options);
        }

        return savedVersion;
    }

    @Transactional(readOnly = true)
    public List<RegistrationForm> listFormsByOrg(UUID orgId) {
        return formRepository.findByOrgIdOrderByCreatedAtUtcDesc(orgId);
    }

    @Transactional
    public AgreementTemplate createAgreementVersion(UUID orgId, AdminAgreementCreateRequestDto req) {
        int nextVersion = agreementTemplateRepository
                .findTopByOrgIdAndTemplateKeyOrderByVersionDesc(orgId, req.getTemplateKey())
                .map(t -> t.getVersion() + 1)
                .orElse(1);

        String hash = esignAgreementService.sha256Hex(req.getBodyHtml());
        AgreementTemplate template = AgreementTemplate.builder()
                .orgId(orgId)
                .templateKey(req.getTemplateKey().trim())
                .version(nextVersion)
                .type(req.getType().trim())
                .ageRule(req.getAgeRule() != null ? req.getAgeRule() : "ALL_AGES")
                .requirementMode(req.getRequirementMode() != null ? req.getRequirementMode() : "REQUIRED")
                .title(req.getTitle().trim())
                .summaryBulletsJson(req.getSummaryBulletsJson() != null ? req.getSummaryBulletsJson() : objectMapper.createArrayNode())
                .bodyHtml(req.getBodyHtml())
                .contentHash(hash)
                .isActive(req.getIsActive() != null ? req.getIsActive() : true)
                .build();
        return agreementTemplateRepository.save(template);
    }

    @Transactional(readOnly = true)
    public List<AgreementTemplate> listAgreementsByOrg(UUID orgId) {
        return agreementTemplateRepository.findByOrgIdOrderByTemplateKeyAscVersionDesc(orgId);
    }

    @Transactional
    public FormAccountQuota upsertAccountQuota(UUID orgId, UUID formId, AdminQuotaUpsertRequestDto req) {
        RegistrationForm form = formRepository.findById(formId)
                .orElseThrow(() -> new EntityNotFoundException("Form not found: " + formId));
        if (!form.getOrgId().equals(orgId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Form belongs to another organization");
        }
        FormAccountQuota quota = quotaRepository.findByFormIdAndAccountUserId(formId, req.getAccountUserId())
                .orElseGet(() -> FormAccountQuota.builder()
                        .form(form)
                        .accountUserId(req.getAccountUserId())
                        .build());
        quota.setOverrideMaxSubmissions(req.getOverrideMaxSubmissions());
        return quotaRepository.save(quota);
    }
}
