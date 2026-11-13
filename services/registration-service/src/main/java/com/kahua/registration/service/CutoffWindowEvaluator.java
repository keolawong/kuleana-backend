package com.kahua.registration.service;

import tools.jackson.databind.JsonNode;
import com.kahua.registration.dto.CutoffWindowResultDto;
import com.kahua.registration.entity.RegistrationForm;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.Clock;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CutoffWindowEvaluator {

    private final Clock clock;

    public boolean isFormLive(RegistrationForm form) {
        if (form.getGoLiveAtUtc() == null) {
            return true;
        }
        Instant now = clock.instant();
        return !now.isBefore(form.getGoLiveAtUtc().toInstant());
    }

    public CutoffWindowResultDto resolveActiveWindow(JsonNode cutoffWindowsJson) {
        if (cutoffWindowsJson == null || !cutoffWindowsJson.isArray() || cutoffWindowsJson.isEmpty()) {
            return null;
        }
        Instant now = clock.instant();
        List<JsonNode> matching = new ArrayList<>();
        for (JsonNode w : cutoffWindowsJson) {
            String startStr = w.path("startsAtUtc").asText(null);
            String endStr = w.path("endsAtUtc").asText(null);
            Instant start = startStr != null && !startStr.isBlank() ? OffsetDateTime.parse(startStr).toInstant() : Instant.MIN;
            Instant end = endStr != null && !endStr.isBlank() ? OffsetDateTime.parse(endStr).toInstant() : Instant.MAX;
            if (!now.isBefore(start) && now.isBefore(end)) {
                matching.add(w);
            }
        }
        if (matching.isEmpty()) {
            return null;
        }
        JsonNode best = matching.stream()
                .max(Comparator.comparingInt(w -> w.path("priority").asInt(0)))
                .orElse(matching.get(0));

        List<String> availableOptionIds = new ArrayList<>();
        if (best.has("availableOptionIds") && best.get("availableOptionIds").isArray()) {
            for (JsonNode opt : best.get("availableOptionIds")) {
                availableOptionIds.add(opt.asText());
            }
        }

        String startStr = best.path("startsAtUtc").asText(null);
        String endStr = best.path("endsAtUtc").asText(null);

        return CutoffWindowResultDto.builder()
                .windowId(best.path("id").asText(null))
                .label(best.path("label").asText(null))
                .startsAtUtc(startStr != null && !startStr.isBlank() ? OffsetDateTime.parse(startStr) : null)
                .endsAtUtc(endStr != null && !endStr.isBlank() ? OffsetDateTime.parse(endStr) : null)
                .allowRegister(best.path("allowRegister").asBoolean(true))
                .allowEdit(best.path("allowEdit").asBoolean(true))
                .allowSwapShift(best.path("allowSwapShift").asBoolean(true))
                .allowCancel(best.path("allowCancel").asBoolean(true))
                .availableOptionIds(availableOptionIds.isEmpty() ? null : availableOptionIds)
                .feeTier(best.get("feeTier"))
                .build();
    }

    public CutoffWindowResultDto verifyWindowForAction(
            RegistrationForm form,
            JsonNode cutoffWindowsJson,
            String expectedWindowId,
            String action) {

        if (!Boolean.TRUE.equals(form.getIsActive())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Registration form is not active");
        }
        if (!isFormLive(form)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Registration has not gone live yet");
        }

        CutoffWindowResultDto active = resolveActiveWindow(cutoffWindowsJson);

        if (cutoffWindowsJson != null && cutoffWindowsJson.isArray() && !cutoffWindowsJson.isEmpty() && active == null) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "No active registration window is currently open");
        }

        if (expectedWindowId != null && !expectedWindowId.isBlank()) {
            String currentId = active != null ? active.getWindowId() : null;
            if (!expectedWindowId.equals(currentId)) {
                throw new ResponseStatusException(
                        HttpStatus.CONFLICT,
                        "Registration window changed during your session. Please refresh and review updated pricing and options.");
            }
        }

        if (active != null) {
            boolean allowed = switch (action) {
                case "REGISTER" -> active.isAllowRegister();
                case "EDIT" -> active.isAllowEdit();
                case "SWAP" -> active.isAllowSwapShift();
                case "CANCEL" -> active.isAllowCancel();
                default -> true;
            };
            if (!allowed) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                        "Action '" + action + "' is closed for the current registration window");
            }
        }

        return active;
    }
}
