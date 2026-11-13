package com.kahua.registration.service;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.junit.jupiter.api.Assertions.*;

class ConditionalLogicEvaluatorTest {

    private ObjectMapper objectMapper;
    private ConditionalLogicEvaluator evaluator;

    @BeforeEach
    void setUp() {
        objectMapper = JsonMapper.builder().findAndAddModules().build();
        Clock fixedClock = Clock.fixed(Instant.parse("2026-06-01T12:00:00Z"), ZoneOffset.UTC);
        evaluator = new ConditionalLogicEvaluator(objectMapper, fixedClock);
    }

    @Test
    void evaluatesOperatorsAndStripsHiddenAndAdminOnlyFields() throws Exception {
        JsonNode schema = objectMapper.readTree("""
            {
              "fields": [
                {"id": "role", "label": "Role", "required": true, "visibility": "PUBLIC"},
                {"id": "dietary", "label": "Dietary", "required": true, "visibility": "PUBLIC"},
                {"id": "tshirtSize", "label": "T-Shirt Size", "required": true, "visibility": "PUBLIC"},
                {"id": "guardianNote", "label": "Guardian Note", "required": true, "visibility": "PUBLIC"},
                {"id": "beltRank", "label": "Belt Rank", "required": false, "visibility": "PUBLIC"},
                {"id": "weight", "label": "Weight", "required": false, "visibility": "PUBLIC"},
                {"id": "bgCheck", "label": "Background Check", "required": false, "visibility": "ADMIN_ONLY"}
              ]
            }
            """);

        JsonNode rules = objectMapper.readTree("""
            [
              {
                "targetFieldId": "tshirtSize",
                "action": "SHOW",
                "match": "ALL",
                "conditions": [
                  {"field": "role", "operator": "in", "value": ["VOLUNTEER", "CAPTAIN"]},
                  {"field": "dietary", "operator": "notEquals", "value": "NONE"}
                ]
              },
              {
                "targetFieldId": "guardianNote",
                "action": "SHOW",
                "match": "ALL",
                "conditions": [
                  {"field": "participant.dob", "operator": "ageAtEventDate", "comparator": "lt", "value": 18}
                ]
              },
              {
                "targetFieldId": "beltRank",
                "action": "SHOW",
                "match": "ALL",
                "conditions": [
                  {"field": "tags", "operator": "contains", "value": "sparring"},
                  {"field": "weight", "operator": "greaterThan", "value": 100}
                ]
              }
            ]
            """);

        JsonNode config = objectMapper.readTree("""
            {"eventStartDate": "2026-10-03", "ageCutoffStrategy": "EVENT_START_DATE"}
            """);

        JsonNode profile = objectMapper.readTree("""
            {"firstName": "Maya", "lastName": "Lin", "dob": "2000-05-01"}
            """);

        // Adult (26 at event), role=SPECTATOR -> tshirtSize & guardianNote hidden even if stale values sent
        JsonNode answers = objectMapper.readTree("""
            {
              "role": "SPECTATOR",
              "dietary": "VEGAN",
              "tshirtSize": "L",
              "guardianNote": "stale",
              "tags": ["sparring"],
              "weight": 135,
              "bgCheck": "PASSED"
            }
            """);

        ConditionalLogicEvaluator.EvaluationResult result = evaluator.evaluateAndSanitize(
                schema, rules, config, "America/Denver", profile, answers, true);

        assertFalse(result.visibleFieldIds().contains("tshirtSize"));
        assertFalse(result.visibleFieldIds().contains("guardianNote"));
        assertFalse(result.visibleFieldIds().contains("bgCheck"));
        assertTrue(result.visibleFieldIds().contains("beltRank"));

        // Stale Data Rule & ADMIN_ONLY stripping
        assertNull(result.sanitizedAnswers().get("tshirtSize"));
        assertNull(result.sanitizedAnswers().get("guardianNote"));
        assertNull(result.sanitizedAnswers().get("bgCheck"));
        assertEquals("SPECTATOR", result.sanitizedAnswers().get("role").asText());
    }

    @Test
    void enforcesRequiredOnlyOnVisibleFields_DeadlockRule() throws Exception {
        JsonNode schema = objectMapper.readTree("""
            {
              "fields": [
                {"id": "role", "label": "Role", "required": true, "visibility": "PUBLIC"},
                {"id": "guardianNote", "label": "Guardian Note", "required": true, "visibility": "PUBLIC"}
              ]
            }
            """);

        JsonNode rules = objectMapper.readTree("""
            [
              {
                "targetFieldId": "guardianNote",
                "action": "SHOW",
                "conditions": [
                  {"field": "participant.dob", "operator": "ageAtEventDate", "comparator": "lt", "value": 18}
                ]
              }
            ]
            """);

        JsonNode config = objectMapper.readTree("""
            {"eventStartDate": "2026-10-03", "ageCutoffStrategy": "CALENDAR_YEAR_END"}
            """);

        // Minor: born 2010-01-01 -> age 16 on 2026-12-31 -> guardianNote becomes visible & required
        JsonNode minorProfile = objectMapper.readTree("""
            {"dob": "2010-01-01"}
            """);
        JsonNode answers = objectMapper.readTree("""
            {"role": "COMPETITOR"}
            """);

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () ->
                evaluator.evaluateAndSanitize(schema, rules, config, "America/Denver", minorProfile, answers, true));
        assertEquals(400, ex.getStatusCode().value());
    }

    @Test
    void stripsAdminOnlyFieldsFromSchema() throws Exception {
        JsonNode schema = objectMapper.readTree("""
            {
              "fields": [
                {"id": "shirt", "visibility": "PUBLIC"},
                {"id": "internalNotes", "visibility": "ADMIN_ONLY"}
              ]
            }
            """);

        JsonNode stripped = evaluator.stripAdminOnlyFieldsFromSchema(schema);
        assertEquals(1, stripped.get("fields").size());
        assertEquals("shirt", stripped.get("fields").get(0).get("id").asText());
    }
}
