package com.kahua.registration.service;

import tools.jackson.databind.JsonNode;
import com.kahua.registration.entity.RegistrationOutbox;
import com.kahua.registration.entity.RegistrationSubmission;
import com.kahua.registration.entity.RegistrationSubmissionSelection;
import com.kahua.registration.entity.SelectionStatus;
import com.kahua.registration.repository.RegistrationSubmissionRepository;
import com.kahua.registration.repository.RegistrationSubmissionSelectionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class AssetsPersonSyncAdapter {

    private final RegistrationSubmissionRepository submissionRepository;
    private final RegistrationSubmissionSelectionRepository selectionRepository;
    private final RestClient.Builder restClientBuilder;

    @Value("${kahua.assets.base-url:http://localhost:8081}")
    private String assetsBaseUrl;

    @Value("${security.internal-task-api-key:default-internal-key-change-me}")
    private String internalApiKey;

    public void syncSubmission(RegistrationOutbox outbox) {
        RegistrationSubmission submission = submissionRepository.findById(outbox.getSubmissionId())
                .orElseThrow(() -> new IllegalStateException("Submission not found for outbox: " + outbox.getSubmissionId()));

        List<RegistrationSubmissionSelection> activeSelections = selectionRepository
                .findBySubmissionIdAndStatus(submission.getId(), SelectionStatus.ACTIVE);

        JsonNode profile = submission.getParticipantProfileJson();
        String firstName = profile != null ? profile.path("firstName").asText("") : "";
        String lastName = profile != null ? profile.path("lastName").asText("") : "";

        List<Map<String, Object>> payloads = new ArrayList<>();
        if (activeSelections.isEmpty()) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("orgId", submission.getOrgId());
            item.put("firstName", firstName);
            item.put("lastName", lastName);
            item.put("externalId", submission.getId().toString());
            item.put("isActive", submission.getCancelledAtUtc() == null);
            payloads.add(item);
        } else {
            for (RegistrationSubmissionSelection sel : activeSelections) {
                Map<String, Object> item = new LinkedHashMap<>();
                item.put("orgId", submission.getOrgId());
                item.put("firstName", firstName);
                item.put("lastName", lastName);
                item.put("externalId", submission.getId() + ":" + sel.getOption().getId());
                if (sel.getOption().getExternalRefId() != null) {
                    try {
                        item.put("shiftId", UUID.fromString(sel.getOption().getExternalRefId()));
                    } catch (IllegalArgumentException ignored) {
                        // Non-UUID externalRefId stored as jobRole/notes reference
                    }
                }
                item.put("jobRole", sel.getOption().getLabel());
                item.put("isActive", submission.getCancelledAtUtc() == null);
                payloads.add(item);
            }
        }

        RestClient client = restClientBuilder.baseUrl(assetsBaseUrl).build();
        client.post()
                .uri("/api/persons/bulk")
                .contentType(MediaType.APPLICATION_JSON)
                .header("X-Org-Id", submission.getOrgId().toString())
                .header("X-Internal-Task-Key", internalApiKey)
                .body(payloads)
                .retrieve()
                .toBodilessEntity();

        log.info("Synced submission {} ({} records) to assets-service", submission.getId(), payloads.size());
    }
}
