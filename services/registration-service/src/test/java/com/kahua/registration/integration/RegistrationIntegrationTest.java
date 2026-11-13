package com.kahua.registration.integration;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import com.kahua.registration.entity.AgreementTemplate;
import com.kahua.registration.entity.FormMode;
import com.kahua.registration.entity.RegistrationForm;
import com.kahua.registration.entity.RegistrationFormVersion;
import com.kahua.registration.entity.RegistrationOption;
import com.kahua.registration.repository.*;
import com.kahua.registration.service.EsignAgreementService;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

import java.security.Key;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Date;
import java.util.UUID;

import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@Transactional
class RegistrationIntegrationTest {

    @Autowired
    private WebApplicationContext webApplicationContext;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private RegistrationFormRepository formRepository;

    @Autowired
    private RegistrationFormVersionRepository versionRepository;

    @Autowired
    private RegistrationOptionRepository optionRepository;

    @Autowired
    private AgreementTemplateRepository agreementTemplateRepository;

    @Autowired
    private EsignAgreementService esignAgreementService;

    private MockMvc mockMvc;
    private UUID orgId;
    private UUID otherOrgId;
    private String userAToken;
    private String userBToken;
    private String adminToken;
    private String otherOrgAdminToken;

    private RegistrationForm liveForm;
    private RegistrationOption optionA;
    private RegistrationOption optionB;
    private AgreementTemplate waiverTemplate;

    @BeforeEach
    void setUp() throws Exception {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext)
                .apply(springSecurity())
                .build();

        orgId = UUID.randomUUID();
        otherOrgId = UUID.randomUUID();
        userAToken = buildJwt("maya@example.com", "USER", orgId);
        userBToken = buildJwt("alex@example.com", "USER", orgId);
        adminToken = buildJwt("admin@example.com", "ADMIN", orgId);
        otherOrgAdminToken = buildJwt("other-admin@example.com", "ADMIN", otherOrgId);

        liveForm = formRepository.save(RegistrationForm.builder()
                .orgId(orgId)
                .slug("fiesta-2026")
                .title("Balloon Fiesta Volunteers 2026")
                .formMode(FormMode.SHIFTS_AND_ROLES)
                .eventTimezone("America/Denver")
                .goLiveAtUtc(OffsetDateTime.now(ZoneOffset.UTC).minusDays(1))
                .isActive(true)
                .build());

        RegistrationFormVersion version = versionRepository.save(RegistrationFormVersion.builder()
                .form(liveForm)
                .version(1)
                .schemaJson(objectMapper.readTree("""
                    {
                      "fields": [
                        {"id": "shirtSize", "label": "Shirt Size", "required": true, "visibility": "PUBLIC"},
                        {"id": "bgCheck", "label": "Background Check", "required": false, "visibility": "ADMIN_ONLY"}
                      ]
                    }
                    """))
                .logicRulesJson(objectMapper.readTree("[]"))
                .configJson(objectMapper.readTree("""
                    {"eventStartDate": "2026-10-03", "maxPerAccount": 1}
                    """))
                .cutoffWindowsJson(objectMapper.readTree("[]"))
                .build());

        optionA = optionRepository.save(RegistrationOption.builder()
                .formVersion(version)
                .externalRefId(UUID.randomUUID().toString())
                .optionType("SHIFT_POSITION")
                .label("Morning Gate Scanner")
                .metadataJson(objectMapper.createObjectNode())
                .maxCapacity(1)
                .reservedCount(0)
                .build());

        optionB = optionRepository.save(RegistrationOption.builder()
                .formVersion(version)
                .externalRefId(UUID.randomUUID().toString())
                .optionType("SHIFT_POSITION")
                .label("Evening Gate Seller")
                .metadataJson(objectMapper.createObjectNode())
                .maxCapacity(5)
                .reservedCount(0)
                .build());

        String bodyHtml = "<p>I agree to the Balloon Fiesta volunteer waiver.</p>";
        waiverTemplate = agreementTemplateRepository.save(AgreementTemplate.builder()
                .orgId(orgId)
                .templateKey("liability-waiver")
                .version(1)
                .type("LIABILITY")
                .ageRule("ALL_AGES")
                .requirementMode("REQUIRED")
                .title("Volunteer Liability Waiver")
                .summaryBulletsJson(objectMapper.readTree("[\"Follow safety instructions\"]"))
                .bodyHtml(bodyHtml)
                .contentHash(esignAgreementService.sha256Hex(bodyHtml))
                .isActive(true)
                .build());
    }

    @Test
    void preGoLiveReturnsMetadataOnlyAndLiveStripsAdminOnly() throws Exception {
        formRepository.save(RegistrationForm.builder()
                .orgId(orgId)
                .slug("future-event")
                .title("Future Event")
                .formMode(FormMode.GENERAL_EVENT)
                .eventTimezone("America/Denver")
                .goLiveAtUtc(OffsetDateTime.now(ZoneOffset.UTC).plusDays(10))
                .isActive(true)
                .build());

        // Anonymous pre-go-live request -> metadata only
        mockMvc.perform(get("/api/registration/forms/future-event"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.live").value(false))
                .andExpect(jsonPath("$.title").value("Future Event"))
                .andExpect(jsonPath("$.schemaJson").doesNotExist())
                .andExpect(jsonPath("$.options").doesNotExist());

        // Live public request -> strips ADMIN_ONLY fields
        mockMvc.perform(get("/api/registration/forms/fiesta-2026"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.live").value(true))
                .andExpect(jsonPath("$.schemaJson.fields", hasSize(1)))
                .andExpect(jsonPath("$.schemaJson.fields[0].id").value("shirtSize"));
    }

    @Test
    void submitMinorWithGuardian_EnforcesCapacityQuotaSwapAndIdor() throws Exception {
        String submitPayload = """
            {
              "participantProfile": {"firstName": "Leo", "lastName": "Lin", "dob": "2012-04-10"},
              "answers": {"shirtSize": "M", "bgCheck": "HACKED"},
              "selectedOptionIds": ["%s"],
              "signedAgreements": [
                {
                  "templateVersionId": "%s",
                  "participantId": "minor-1",
                  "participantName": "Leo Lin",
                  "participantDob": "2012-04-10",
                  "esignConsentAccepted": true,
                  "signerName": "Maya Lin",
                  "signerRelationship": "Parent",
                  "guardianAuthorityAttested": true,
                  "minorAssentName": "Leo Lin",
                  "signatureData": "data:image/png;base64,AAA",
                  "expectedSnapshotHash": "%s"
                }
              ]
            }
            """.formatted(optionA.getId(), waiverTemplate.getId(), waiverTemplate.getContentHash());

        MvcResult result = mockMvc.perform(post("/api/registration/forms/fiesta-2026/submit")
                        .header("Authorization", userAToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(submitPayload))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("SUBMITTED"))
                .andExpect(jsonPath("$.confirmationCode", startsWith("REG-")))
                .andExpect(jsonPath("$.adminFieldsJson").doesNotExist())
                .andExpect(jsonPath("$.answersJson.bgCheck").doesNotExist())
                .andExpect(jsonPath("$.signedAgreements[0].isMinor").value(true))
                .andExpect(jsonPath("$.signedAgreements[0].minorAssentName").value("Leo Lin"))
                .andReturn();

        JsonNode created = objectMapper.readTree(result.getResponse().getContentAsString());
        UUID submissionId = UUID.fromString(created.get("id").asText());

        // IDOR check: userB cannot read or cancel userA's submission
        mockMvc.perform(get("/api/registration/my-submissions/" + submissionId)
                        .header("Authorization", userBToken))
                .andExpect(status().isForbidden());

        mockMvc.perform(post("/api/registration/submissions/" + submissionId + "/cancel")
                        .header("Authorization", userBToken))
                .andExpect(status().isForbidden());

        // UserB trying to select full optionA (maxCapacity = 1) -> 409 Conflict
        String fullPayload = """
            {
              "participantProfile": {"firstName": "Alex", "lastName": "Kim", "dob": "1990-01-01"},
              "answers": {"shirtSize": "L"},
              "selectedOptionIds": ["%s"],
              "signedAgreements": [
                {
                  "templateVersionId": "%s",
                  "participantId": "adult-1",
                  "participantName": "Alex Kim",
                  "participantDob": "1990-01-01",
                  "esignConsentAccepted": true,
                  "signerName": "Alex Kim",
                  "signatureData": "Alex Kim"
                }
              ]
            }
            """.formatted(optionA.getId(), waiverTemplate.getId());

        mockMvc.perform(post("/api/registration/forms/fiesta-2026/submit")
                        .header("Authorization", userBToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(fullPayload))
                .andExpect(status().isConflict());

        // UserA swaps optionA -> optionB
        mockMvc.perform(post("/api/registration/submissions/" + submissionId + "/swap")
                        .header("Authorization", userAToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"oldOptionId\":\"%s\",\"newOptionId\":\"%s\"}".formatted(optionA.getId(), optionB.getId())))
                .andExpect(status().isOk());

        // UserA hits maxPerAccount=1 -> 409 Conflict
        String secondPayload = """
            {
              "participantProfile": {"firstName": "Maya", "lastName": "Lin", "dob": "1985-01-01"},
              "answers": {"shirtSize": "S"},
              "selectedOptionIds": ["%s"],
              "signedAgreements": [
                {
                  "templateVersionId": "%s",
                  "participantId": "adult-2",
                  "participantName": "Maya Lin",
                  "participantDob": "1985-01-01",
                  "esignConsentAccepted": true,
                  "signerName": "Maya Lin",
                  "signatureData": "Maya Lin"
                }
              ]
            }
            """.formatted(optionB.getId(), waiverTemplate.getId());

        mockMvc.perform(post("/api/registration/forms/fiesta-2026/submit")
                        .header("Authorization", userAToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(secondPayload))
                .andExpect(status().isConflict());

        // Admin grants quota override -> second submission succeeds
        mockMvc.perform(put("/api/registration/admin/quotas/" + liveForm.getId())
                        .header("Authorization", adminToken)
                        .header("X-Org-Id", orgId.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"accountUserId\":\"maya@example.com\",\"overrideMaxSubmissions\":2}"))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/registration/forms/fiesta-2026/submit")
                        .header("Authorization", userAToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(secondPayload))
                .andExpect(status().isCreated());
    }

    @Test
    void rejectsMassAssignmentAndEnforcesAdminRbac() throws Exception {
        // Public endpoint rejects mass-assignment of paymentStatus / adminFields
        mockMvc.perform(post("/api/registration/forms/fiesta-2026/drafts")
                        .header("Authorization", userAToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"paymentStatus\":\"PAID\",\"answers\":{\"shirtSize\":\"M\"}}"))
                .andExpect(status().isBadRequest());

        // Non-admin rejected from admin endpoint
        mockMvc.perform(get("/api/registration/admin/forms")
                        .header("Authorization", userAToken)
                        .header("X-Org-Id", orgId.toString()))
                .andExpect(status().isForbidden());

        // Admin from another org rejected by TenantSecurityUtil
        mockMvc.perform(get("/api/registration/admin/forms")
                        .header("Authorization", otherOrgAdminToken)
                        .header("X-Org-Id", orgId.toString()))
                .andExpect(status().isForbidden());

        // Admin of same org succeeds
        mockMvc.perform(get("/api/registration/admin/forms")
                        .header("Authorization", adminToken)
                        .header("X-Org-Id", orgId.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].slug").value("fiesta-2026"));
    }

    private String buildJwt(String username, String roles, UUID orgId) {
        long now = System.currentTimeMillis();
        Key key = Keys.hmacShaKeyFor("your-256-bit-secret-your-256-bit-secret".getBytes());
        return "Bearer " + Jwts.builder()
                .setSubject(username)
                .claim("roles", roles)
                .claim("orgIds", orgId.toString())
                .setIssuedAt(new Date(now))
                .setExpiration(new Date(now + 3600000))
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();
    }
}
