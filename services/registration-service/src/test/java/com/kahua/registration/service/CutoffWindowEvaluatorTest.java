package com.kahua.registration.service;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;
import com.kahua.registration.dto.CutoffWindowResultDto;
import com.kahua.registration.entity.RegistrationForm;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

import java.time.Clock;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

import static org.junit.jupiter.api.Assertions.*;

class CutoffWindowEvaluatorTest {

    private final ObjectMapper objectMapper = JsonMapper.builder().findAndAddModules().build();

    @Test
    void detectsPreGoLiveAndWindowRolloverConflict() throws Exception {
        Clock fixedClock = Clock.fixed(Instant.parse("2026-08-01T12:00:00Z"), ZoneOffset.UTC);
        CutoffWindowEvaluator evaluator = new CutoffWindowEvaluator(fixedClock);

        RegistrationForm preLiveForm = RegistrationForm.builder()
                .isActive(true)
                .goLiveAtUtc(OffsetDateTime.parse("2026-08-10T00:00:00Z"))
                .build();
        assertFalse(evaluator.isFormLive(preLiveForm));

        RegistrationForm liveForm = RegistrationForm.builder()
                .isActive(true)
                .goLiveAtUtc(OffsetDateTime.parse("2026-07-01T00:00:00Z"))
                .build();
        assertTrue(evaluator.isFormLive(liveForm));

        JsonNode windows = objectMapper.readTree("""
            [
              {
                "id": "early-bird",
                "label": "Early Bird",
                "startsAtUtc": "2026-07-01T00:00:00Z",
                "endsAtUtc": "2026-08-01T00:00:00Z",
                "priority": 1,
                "allowRegister": true
              },
              {
                "id": "regular",
                "label": "Regular",
                "startsAtUtc": "2026-08-01T00:00:00Z",
                "endsAtUtc": "2026-09-01T00:00:00Z",
                "priority": 2,
                "allowRegister": true,
                "allowSwapShift": false,
                "feeTier": {"baseFee": 75}
              }
            ]
            """);

        CutoffWindowResultDto active = evaluator.resolveActiveWindow(windows);
        assertNotNull(active);
        assertEquals("regular", active.getWindowId());

        // Mid-draft rollover from early-bird to regular -> 409 Conflict
        ResponseStatusException rollover = assertThrows(ResponseStatusException.class, () ->
                evaluator.verifyWindowForAction(liveForm, windows, "early-bird", "REGISTER"));
        assertEquals(409, rollover.getStatusCode().value());

        // Swap disallowed in regular window -> 403 Forbidden
        ResponseStatusException swapDenied = assertThrows(ResponseStatusException.class, () ->
                evaluator.verifyWindowForAction(liveForm, windows, "regular", "SWAP"));
        assertEquals(403, swapDenied.getStatusCode().value());
    }
}
