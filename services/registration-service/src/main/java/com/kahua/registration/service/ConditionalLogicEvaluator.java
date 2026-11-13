package com.kahua.registration.service;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.ArrayNode;
import tools.jackson.databind.node.ObjectNode;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.*;
import java.time.temporal.ChronoUnit;
import java.util.*;

@Service
@RequiredArgsConstructor
public class ConditionalLogicEvaluator {

    private final ObjectMapper objectMapper;
    private final Clock clock;

    public record EvaluationResult(Set<String> visibleFieldIds, ObjectNode sanitizedAnswers) {}

    public JsonNode stripAdminOnlyFieldsFromSchema(JsonNode schemaJson) {
        if (schemaJson == null || !schemaJson.isObject()) {
            return schemaJson;
        }
        ObjectNode copy = (ObjectNode) schemaJson.deepCopy();
        JsonNode fieldsNode = copy.get("fields");
        if (fieldsNode != null && fieldsNode.isArray()) {
            ArrayNode filtered = objectMapper.createArrayNode();
            for (JsonNode field : fieldsNode) {
                String visibility = field.path("visibility").asText("PUBLIC");
                if (!"ADMIN_ONLY".equalsIgnoreCase(visibility)) {
                    filtered.add(field);
                }
            }
            copy.set("fields", filtered);
        }
        return copy;
    }

    public EvaluationResult evaluateAndSanitize(
            JsonNode schemaJson,
            JsonNode logicRulesJson,
            JsonNode configJson,
            String eventTimezone,
            JsonNode participantProfile,
            JsonNode rawAnswers,
            boolean validateRequired) {

        Map<String, JsonNode> fieldDefs = new LinkedHashMap<>();
        Set<String> adminOnlyFields = new HashSet<>();
        Set<String> visibleFields = new LinkedHashSet<>();

        if (schemaJson != null && schemaJson.has("fields") && schemaJson.get("fields").isArray()) {
            for (JsonNode field : schemaJson.get("fields")) {
                String id = field.path("id").asText(null);
                if (id == null || id.isBlank()) continue;
                fieldDefs.put(id, field);
                String visibility = field.path("visibility").asText("PUBLIC");
                if ("ADMIN_ONLY".equalsIgnoreCase(visibility)) {
                    adminOnlyFields.add(id);
                } else {
                    // Fields targeted by a SHOW rule start hidden unless rule matches
                    visibleFields.add(id);
                }
            }
        }

        // First pass: any field targeted by a SHOW rule starts hidden
        Map<String, List<JsonNode>> showRulesByTarget = new LinkedHashMap<>();
        Map<String, List<JsonNode>> hideRulesByTarget = new LinkedHashMap<>();
        if (logicRulesJson != null && logicRulesJson.isArray()) {
            for (JsonNode rule : logicRulesJson) {
                String targetId = rule.path("targetFieldId").asText(null);
                if (targetId == null || adminOnlyFields.contains(targetId)) continue;
                String action = rule.path("action").asText("SHOW").toUpperCase();
                if ("HIDE".equals(action)) {
                    hideRulesByTarget.computeIfAbsent(targetId, k -> new ArrayList<>()).add(rule);
                } else {
                    visibleFields.remove(targetId);
                    showRulesByTarget.computeIfAbsent(targetId, k -> new ArrayList<>()).add(rule);
                }
            }
        }

        // Evaluate SHOW rules
        for (Map.Entry<String, List<JsonNode>> entry : showRulesByTarget.entrySet()) {
            boolean anyMatched = entry.getValue().stream()
                    .anyMatch(rule -> evaluateRule(rule, configJson, eventTimezone, participantProfile, rawAnswers));
            if (anyMatched) {
                visibleFields.add(entry.getKey());
            }
        }

        // Evaluate HIDE rules
        for (Map.Entry<String, List<JsonNode>> entry : hideRulesByTarget.entrySet()) {
            boolean anyMatched = entry.getValue().stream()
                    .anyMatch(rule -> evaluateRule(rule, configJson, eventTimezone, participantProfile, rawAnswers));
            if (anyMatched) {
                visibleFields.remove(entry.getKey());
            }
        }

        // Stale Data Rule: strip hidden and ADMIN_ONLY fields from answers
        ObjectNode sanitized = objectMapper.createObjectNode();
        if (rawAnswers != null && rawAnswers.isObject()) {
            for (Map.Entry<String, JsonNode> entry : rawAnswers.properties()) {
                String key = entry.getKey();
                if (!adminOnlyFields.contains(key) && (fieldDefs.isEmpty() || visibleFields.contains(key))) {
                    sanitized.set(key, entry.getValue());
                }
            }
        }

        // Deadlock Rule: validate required only on visible, non-ADMIN_ONLY fields
        if (validateRequired) {
            for (String fieldId : visibleFields) {
                JsonNode def = fieldDefs.get(fieldId);
                if (def != null && def.path("required").asBoolean(false)) {
                    JsonNode val = sanitized.get(fieldId);
                    if (isMissingOrBlank(val)) {
                        String label = def.path("label").asText(fieldId);
                        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Required field is missing: " + label);
                    }
                }
            }
        }

        return new EvaluationResult(Collections.unmodifiableSet(visibleFields), sanitized);
    }

    private boolean evaluateRule(JsonNode rule, JsonNode configJson, String eventTimezone,
                                 JsonNode participantProfile, JsonNode rawAnswers) {
        JsonNode conditions = rule.get("conditions");
        if (conditions == null || !conditions.isArray() || conditions.isEmpty()) {
            return true;
        }
        String match = rule.path("match").asText("ALL").toUpperCase();
        if ("ANY".equals(match)) {
            for (JsonNode cond : conditions) {
                if (evaluateCondition(cond, configJson, eventTimezone, participantProfile, rawAnswers)) {
                    return true;
                }
            }
            return false;
        } else {
            for (JsonNode cond : conditions) {
                if (!evaluateCondition(cond, configJson, eventTimezone, participantProfile, rawAnswers)) {
                    return false;
                }
            }
            return true;
        }
    }

    private boolean evaluateCondition(JsonNode cond, JsonNode configJson, String eventTimezone,
                                      JsonNode participantProfile, JsonNode rawAnswers) {
        String field = cond.path("field").asText("");
        String operator = cond.path("operator").asText("equals");
        JsonNode expected = cond.get("value");
        JsonNode actual = resolveFieldValue(field, participantProfile, rawAnswers);

        return switch (operator) {
            case "equals" -> equalsValue(actual, expected);
            case "notEquals" -> !equalsValue(actual, expected);
            case "in" -> inValues(actual, expected);
            case "contains" -> containsValue(actual, expected);
            case "greaterThan" -> compareNumeric(actual, expected) > 0;
            case "lessThan" -> compareNumeric(actual, expected) < 0;
            case "ageAtEventDate" -> evaluateAgeAtEventDate(actual, expected, cond, configJson, eventTimezone);
            default -> false;
        };
    }

    private JsonNode resolveFieldValue(String field, JsonNode participantProfile, JsonNode rawAnswers) {
        if (field.startsWith("participant.") && participantProfile != null) {
            return participantProfile.get(field.substring("participant.".length()));
        }
        if (rawAnswers != null && rawAnswers.has(field)) {
            return rawAnswers.get(field);
        }
        if (participantProfile != null && participantProfile.has(field)) {
            return participantProfile.get(field);
        }
        return null;
    }

    private boolean equalsValue(JsonNode actual, JsonNode expected) {
        if (actual == null || actual.isNull()) return expected == null || expected.isNull();
        if (expected == null || expected.isNull()) return false;
        if (actual.isNumber() && expected.isNumber()) {
            return actual.decimalValue().compareTo(expected.decimalValue()) == 0;
        }
        if (actual.isBoolean() && expected.isBoolean()) {
            return actual.asBoolean() == expected.asBoolean();
        }
        return actual.asText().equalsIgnoreCase(expected.asText());
    }

    private boolean inValues(JsonNode actual, JsonNode expected) {
        if (actual == null || actual.isNull() || expected == null || !expected.isArray()) {
            return false;
        }
        for (JsonNode item : expected) {
            if (equalsValue(actual, item)) return true;
        }
        return false;
    }

    private boolean containsValue(JsonNode actual, JsonNode expected) {
        if (actual == null || actual.isNull() || expected == null || expected.isNull()) {
            return false;
        }
        if (actual.isArray()) {
            for (JsonNode item : actual) {
                if (equalsValue(item, expected)) return true;
            }
            return false;
        }
        return actual.asText().toLowerCase().contains(expected.asText().toLowerCase());
    }

    private int compareNumeric(JsonNode actual, JsonNode expected) {
        if (actual == null || actual.isNull() || expected == null || expected.isNull()) {
            return Integer.MIN_VALUE;
        }
        try {
            double a = actual.isNumber() ? actual.asDouble() : Double.parseDouble(actual.asText());
            double b = expected.isNumber() ? expected.asDouble() : Double.parseDouble(expected.asText());
            return Double.compare(a, b);
        } catch (NumberFormatException e) {
            return Integer.MIN_VALUE;
        }
    }

    private boolean evaluateAgeAtEventDate(JsonNode actualDob, JsonNode expected, JsonNode cond,
                                           JsonNode configJson, String eventTimezone) {
        if (actualDob == null || actualDob.isNull() || actualDob.asText().isBlank()) {
            return false;
        }
        LocalDate dob;
        try {
            dob = LocalDate.parse(actualDob.asText().trim());
        } catch (Exception e) {
            return false;
        }
        LocalDate cutoffDate = resolveCutoffDate(configJson, eventTimezone);
        int age = (int) ChronoUnit.YEARS.between(dob, cutoffDate);

        String comparator = cond.path("comparator").asText("gte").toLowerCase();
        int threshold = expected != null && !expected.isNull() ? expected.asInt() : 18;
        return switch (comparator) {
            case "lt", "lessThan" -> age < threshold;
            case "lte" -> age <= threshold;
            case "gt", "greaterThan" -> age > threshold;
            case "eq", "equals" -> age == threshold;
            default -> age >= threshold;
        };
    }

    public LocalDate resolveCutoffDate(JsonNode configJson, String eventTimezone) {
        ZoneId zone = ZoneId.of(eventTimezone != null && !eventTimezone.isBlank() ? eventTimezone : "America/Denver");
        LocalDate todayInZone = LocalDate.now(clock.withZone(zone));
        if (configJson == null || configJson.isNull()) {
            return todayInZone;
        }
        String strategy = configJson.path("ageCutoffStrategy").asText("EVENT_START_DATE");
        return switch (strategy) {
            case "CALENDAR_YEAR_END" -> {
                int year = configJson.has("eventStartDate") && !configJson.path("eventStartDate").asText().isBlank()
                        ? LocalDate.parse(configJson.path("eventStartDate").asText()).getYear()
                        : todayInZone.getYear();
                yield LocalDate.of(year, 12, 31);
            }
            case "CUSTOM_DATE" -> {
                String custom = configJson.path("ageCutoffDate").asText("");
                yield custom.isBlank() ? todayInZone : LocalDate.parse(custom);
            }
            default -> {
                String start = configJson.path("eventStartDate").asText("");
                yield start.isBlank() ? todayInZone : LocalDate.parse(start);
            }
        };
    }

    private boolean isMissingOrBlank(JsonNode val) {
        if (val == null || val.isNull()) return true;
        if (val.isTextual()) return val.asText().isBlank();
        if (val.isArray()) return val.isEmpty();
        return false;
    }
}
