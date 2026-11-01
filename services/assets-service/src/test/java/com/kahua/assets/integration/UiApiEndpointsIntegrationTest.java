package com.kahua.assets.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kahua.assets.dto.*;
import com.kahua.assets.entity.*;
import com.kahua.assets.repository.*;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;

import java.math.BigDecimal;
import java.security.Key;
import java.util.*;

import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@Transactional
public class UiApiEndpointsIntegrationTest {

    @Autowired
    private WebApplicationContext webApplicationContext;

    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();

    @Autowired
    private OperationRepository operationRepository;

    @Autowired
    private ShiftRepository shiftRepository;

    @Autowired
    private GateRepository gateRepository;

    @Autowired
    private PersonRepository personRepository;

    @Autowired
    private CashBankRepository cashBankRepository;

    @Autowired
    private TicketBookRepository ticketBookRepository;

    @Autowired
    private CreditReportRepository creditReportRepository;

    @Autowired
    private PersonStickerRepository personStickerRepository;

    private String validToken;
    private Operation seededOperation;
    private Shift seededShift;
    private Gate seededGate;
    private Person seededPerson;

    @BeforeEach
    void setUp() {
        objectMapper.configure(com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext)
                .apply(springSecurity())
                .build();

        long now = System.currentTimeMillis();
        Key key = Keys.hmacShaKeyFor("your-256-bit-secret-your-256-bit-secret".getBytes());
        validToken = "Bearer " + Jwts.builder()
                .setSubject("test-admin")
                .claim("roles", "ADMIN,ASSET-READ,ASSET-WRITE,ASSET-IT,EDITOR,VIEWER,PAGE_/GATE-ASSIGNMENTS,PAGE_/PERSON-STICKERS,PAGE_/INVENTORY,PAGE_/DEPLOY,PAGE_/RECONCILE,PAGE_/ADMIN")
                .setIssuedAt(new Date(now))
                .setExpiration(new Date(now + 3600000))
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();

        // Ensure database has minimal seeded context for happy paths
        List<Operation> operations = operationRepository.findAll();
        if (operations.isEmpty()) {
            Operation op = new Operation();
            op.setName("Test Operation");
            op.setIsActive(true);
            seededOperation = operationRepository.save(op);
        } else {
            seededOperation = operations.get(0);
        }

        List<Shift> shifts = shiftRepository.findAll();
        if (shifts.isEmpty()) {
            Shift s = new Shift();
            s.setName("Test Shift Morning");
            s.setStatus("ACTIVE");
            s.setOperation(seededOperation);
            seededShift = shiftRepository.save(s);
        } else {
            seededShift = shifts.get(0);
        }

        List<Gate> gates = gateRepository.findAll();
        if (gates.isEmpty()) {
            Gate g = new Gate();
            g.setName("North Gate");
            g.setIsActive(true);
            seededGate = gateRepository.save(g);
        } else {
            seededGate = gates.get(0);
        }

        List<Person> persons = personRepository.findAll();
        if (persons.isEmpty()) {
            Person p = new Person();
            p.setFirstName("Jane");
            p.setLastName("Doe");
            p.setShift(seededShift);
            p.setGate(seededGate);
            p.setJobRole("Ticket Seller");
            p.setOrganization("General Public");
            p.setIsActive(true);
            seededPerson = personRepository.save(p);
        } else {
            seededPerson = persons.get(0);
            seededPerson.setJobRole("Ticket Seller");
            seededPerson.setShift(seededShift);
            seededPerson.setGate(seededGate);
            seededPerson = personRepository.save(seededPerson);
        }
    }

    private void assertNot5xx(MvcResult result) {
        int status = result.getResponse().getStatus();
        String body = "";
        try {
            body = result.getResponse().getContentAsString();
        } catch (Exception ignored) {
        }
        assertFalse(status >= 500 && status <= 599,
                "API returned HTTP 500 status: " + status + " with body: " + body);
    }

    // =========================================================================
    // 1. CASH BANKS
    // =========================================================================
    @Nested
    @DisplayName("Cash Banks Endpoints")
    class CashBanksTests {

        @Test
        @DisplayName("GET /api/cash-banks - Happy path returns 200/204 without 500 metamodel error")
        void getCashBanks_happyPath() throws Exception {
            MvcResult res = mockMvc.perform(get("/api/cash-banks")
                            .header("Authorization", validToken))
                    .andExpect(status().is(oneOf(200, 204)))
                    .andReturn();
            assertNot5xx(res);
        }

        @Test
        @DisplayName("GET /api/cash-banks - Unhappy: missing auth returns 401 or 403")
        void getCashBanks_unhappyNoAuth() throws Exception {
            mockMvc.perform(get("/api/cash-banks"))
                    .andExpect(status().is(oneOf(401, 403)));
        }

        @Test
        @DisplayName("GET /api/cash-banks with filters - Happy & Unhappy non-existent UUIDs never return 500")
        void getCashBanks_queryFilters() throws Exception {
            // Happy path with valid operationId
            MvcResult happyRes = mockMvc.perform(get("/api/cash-banks")
                            .header("Authorization", validToken)
                            .param("operationId", seededOperation.getOperationId().toString()))
                    .andExpect(status().is(oneOf(200, 204)))
                    .andReturn();
            assertNot5xx(happyRes);

            // Unhappy path with random non-existent operation UUID returns 204, never 500
            UUID nonExistent = UUID.randomUUID();
            MvcResult unhappyRes = mockMvc.perform(get("/api/cash-banks")
                            .header("Authorization", validToken)
                            .param("operationId", nonExistent.toString()))
                    .andExpect(status().is(oneOf(200, 204)))
                    .andReturn();
            assertNot5xx(unhappyRes);
        }

        @Test
        @DisplayName("POST & PATCH /api/cash-banks - Happy & Unhappy paths")
        void cashBanks_crud() throws Exception {
            // Happy path: Create Cash Bank
            CashBankDto createDto = new CashBankDto();
            createDto.setBankName("Integration Test Bag A");
            createDto.setStartingAmount(new BigDecimal("200.00"));
            createDto.setStatus("AVAILABLE");
            createDto.setOperationId(seededOperation.getOperationId());
            createDto.setShiftId(seededShift.getShiftId());
            createDto.setGateId(seededGate.getGateId());

            MvcResult postRes = mockMvc.perform(post("/api/cash-banks")
                            .header("Authorization", validToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(createDto)))
                    .andExpect(status().isCreated())
                    .andReturn();
            assertNot5xx(postRes);

            CashBank created = objectMapper.readValue(postRes.getResponse().getContentAsString(), CashBank.class);

            // Happy path: Patch Cash Bank
            CashBankPatchDto patchDto = new CashBankPatchDto();
            patchDto.setBankName("Integration Test Bag Renamed");
            MvcResult patchRes = mockMvc.perform(patch("/api/cash-banks/" + created.getCashBankId())
                            .header("Authorization", validToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(patchDto)))
                    .andExpect(status().isOk())
                    .andReturn();
            assertNot5xx(patchRes);

            // Unhappy path: Patch non-existent ID returns 404
            MvcResult unhappyRes = mockMvc.perform(patch("/api/cash-banks/" + UUID.randomUUID())
                            .header("Authorization", validToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(patchDto)))
                    .andExpect(status().isNotFound())
                    .andReturn();
            assertNot5xx(unhappyRes);
        }

        @Test
        @DisplayName("POST /api/cash-banks/bulk - Happy & Unhappy paths")
        void cashBanks_bulk() throws Exception {
            CashBankDto item1 = new CashBankDto();
            item1.setBankName("Bulk Bank 1");
            item1.setStartingAmount(new BigDecimal("100.00"));
            item1.setStatus("AVAILABLE");

            MvcResult res = mockMvc.perform(post("/api/cash-banks/bulk")
                            .header("Authorization", validToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(List.of(item1))))
                    .andExpect(status().isCreated())
                    .andReturn();
            assertNot5xx(res);

            // Unhappy: empty list returns empty 201, never 500
            MvcResult emptyRes = mockMvc.perform(post("/api/cash-banks/bulk")
                            .header("Authorization", validToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("[]"))
                    .andExpect(status().isCreated())
                    .andReturn();
            assertNot5xx(emptyRes);
        }
    }

    // =========================================================================
    // 2. TICKET BOOKS
    // =========================================================================
    @Nested
    @DisplayName("Ticket Books Endpoints")
    class TicketBooksTests {

        @Test
        @DisplayName("GET /api/ticket-books - Happy & Unhappy auth")
        void getTicketBooks_happyAndUnhappy() throws Exception {
            MvcResult res = mockMvc.perform(get("/api/ticket-books")
                            .header("Authorization", validToken))
                    .andExpect(status().is(oneOf(200, 204)))
                    .andReturn();
            assertNot5xx(res);

            // Unhappy: No auth
            mockMvc.perform(get("/api/ticket-books"))
                    .andExpect(status().is(oneOf(401, 403)));
        }

        @Test
        @DisplayName("GET /api/ticket-books with filters - Happy & Unhappy non-existent UUIDs")
        void getTicketBooks_queryFilters() throws Exception {
            // Happy path with valid operationId
            MvcResult happyRes = mockMvc.perform(get("/api/ticket-books")
                            .header("Authorization", validToken)
                            .param("operationId", seededOperation.getOperationId().toString()))
                    .andExpect(status().is(oneOf(200, 204)))
                    .andReturn();
            assertNot5xx(happyRes);

            // Unhappy path with random UUID
            MvcResult unhappyRes = mockMvc.perform(get("/api/ticket-books")
                            .header("Authorization", validToken)
                            .param("operationId", UUID.randomUUID().toString()))
                    .andExpect(status().is(oneOf(200, 204)))
                    .andReturn();
            assertNot5xx(unhappyRes);
        }

        @Test
        @DisplayName("GET /api/ticket-books/max-sold/... - Happy & Unhappy paths")
        void getMaxSold() throws Exception {
            MvcResult res = mockMvc.perform(get("/api/ticket-books/max-sold/" + seededOperation.getOperationId() + "/" + seededShift.getName() + "/" + seededGate.getName())
                            .header("Authorization", validToken))
                    .andExpect(status().isOk())
                    .andReturn();
            assertNot5xx(res);

            // Unhappy: Random non-existent op UUID returns 200 projection null, never 500
            MvcResult unhappyRes = mockMvc.perform(get("/api/ticket-books/max-sold/" + UUID.randomUUID() + "/FakeShift/FakeGate")
                            .header("Authorization", validToken))
                    .andExpect(status().isOk())
                    .andReturn();
            assertNot5xx(unhappyRes);
        }

        @Test
        @DisplayName("POST & PATCH /api/ticket-books - Happy & Unhappy paths")
        void ticketBooks_crud() throws Exception {
            TicketBookDto createDto = new TicketBookDto();
            createDto.setStartNum(1000);
            createDto.setEndNum(1100);
            createDto.setStatus("AVAILABLE");
            createDto.setOperationId(seededOperation.getOperationId());

            MvcResult postRes = mockMvc.perform(post("/api/ticket-books")
                            .header("Authorization", validToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(createDto)))
                    .andExpect(status().isCreated())
                    .andReturn();
            assertNot5xx(postRes);

            TicketBook created = objectMapper.readValue(postRes.getResponse().getContentAsString(), TicketBook.class);

            TicketBookPatchDto patchDto = new TicketBookPatchDto();
            patchDto.setStatus("ASSIGNED");
            MvcResult patchRes = mockMvc.perform(patch("/api/ticket-books/" + created.getTicketBookId())
                            .header("Authorization", validToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(patchDto)))
                    .andExpect(status().isOk())
                    .andReturn();
            assertNot5xx(patchRes);

            // Unhappy: Patch non-existent ID returns 404
            MvcResult unhappyRes = mockMvc.perform(patch("/api/ticket-books/" + UUID.randomUUID())
                            .header("Authorization", validToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(patchDto)))
                    .andExpect(status().isNotFound())
                    .andReturn();
            assertNot5xx(unhappyRes);
        }

        @Test
        @DisplayName("POST /api/ticket-books/bulk - Happy path")
        void ticketBooks_bulk() throws Exception {
            TicketBookDto item = new TicketBookDto();
            item.setStartNum(2000);
            item.setEndNum(2100);
            item.setStatus("AVAILABLE");

            MvcResult res = mockMvc.perform(post("/api/ticket-books/bulk")
                            .header("Authorization", validToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(List.of(item))))
                    .andExpect(status().isCreated())
                    .andReturn();
            assertNot5xx(res);
        }
    }

    // =========================================================================
    // 3. CREDIT REPORTS
    // =========================================================================
    @Nested
    @DisplayName("Credit Reports Endpoints")
    class CreditReportsTests {

        @Test
        @DisplayName("GET /api/credit-reports - Happy & Unhappy paths")
        void getCreditReports() throws Exception {
            MvcResult res = mockMvc.perform(get("/api/credit-reports")
                            .header("Authorization", validToken)
                            .param("shiftId", seededShift.getShiftId().toString()))
                    .andExpect(status().is(oneOf(200, 204)))
                    .andReturn();
            assertNot5xx(res);

            // Unhappy: Missing auth
            mockMvc.perform(get("/api/credit-reports"))
                    .andExpect(status().is(oneOf(401, 403)));

            // Unhappy: Non-existent shift returns 204, never 500
            MvcResult emptyRes = mockMvc.perform(get("/api/credit-reports")
                            .header("Authorization", validToken)
                            .param("shiftId", UUID.randomUUID().toString()))
                    .andExpect(status().is(oneOf(200, 204)))
                    .andReturn();
            assertNot5xx(emptyRes);
        }

        @Test
        @DisplayName("POST & PATCH /api/credit-reports - Happy & Unhappy paths")
        void creditReports_crud() throws Exception {
            CreditReportDto createDto = new CreditReportDto();
            createDto.setTotalAmount(new BigDecimal("150.00"));
            createDto.setShiftId(seededShift.getShiftId());
            createDto.setOperationId(seededOperation.getOperationId());

            MvcResult postRes = mockMvc.perform(post("/api/credit-reports")
                            .header("Authorization", validToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(createDto)))
                    .andExpect(status().isCreated())
                    .andReturn();
            assertNot5xx(postRes);

            CreditReport created = objectMapper.readValue(postRes.getResponse().getContentAsString(), CreditReport.class);

            CreditReportPatchDto patchDto = new CreditReportPatchDto();
            patchDto.setTotalAmount(new BigDecimal("200.00"));
            MvcResult patchRes = mockMvc.perform(patch("/api/credit-reports/" + created.getCreditReportId())
                            .header("Authorization", validToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(patchDto)))
                    .andExpect(status().isOk())
                    .andReturn();
            assertNot5xx(patchRes);

            // Unhappy: Patch non-existent ID returns 404
            MvcResult unhappyRes = mockMvc.perform(patch("/api/credit-reports/" + UUID.randomUUID())
                            .header("Authorization", validToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(patchDto)))
                    .andExpect(status().isNotFound())
                    .andReturn();
            assertNot5xx(unhappyRes);
        }
    }

    // =========================================================================
    // 4. PERSON STICKERS (RECREATED TABLE V19)
    // =========================================================================
    @Nested
    @DisplayName("Person Stickers Endpoints")
    class PersonStickersTests {

        @Test
        @DisplayName("GET /api/person-stickers - Happy returns 200/204 without missing table 500 error")
        void getPersonStickers_happy() throws Exception {
            MvcResult res = mockMvc.perform(get("/api/person-stickers")
                            .header("Authorization", validToken))
                    .andExpect(status().is(oneOf(200, 204)))
                    .andReturn();
            assertNot5xx(res);
        }

        @Test
        @DisplayName("GET /api/person-stickers - Unhappy: missing auth returns 401/403")
        void getPersonStickers_unhappyAuth() throws Exception {
            mockMvc.perform(get("/api/person-stickers"))
                    .andExpect(status().is(oneOf(401, 403)));
        }

        @Test
        @DisplayName("GET /api/person-stickers with shiftId and personStickerId - Happy & Unhappy")
        void getPersonStickers_byFilter() throws Exception {
            // Unhappy: Non-existent shift returns 204, never 500
            MvcResult resShift = mockMvc.perform(get("/api/person-stickers")
                            .header("Authorization", validToken)
                            .param("shiftId", UUID.randomUUID().toString()))
                    .andExpect(status().is(oneOf(200, 204)))
                    .andReturn();
            assertNot5xx(resShift);

            // Unhappy: Non-existent stickerId returns 204, never 500
            MvcResult resSticker = mockMvc.perform(get("/api/person-stickers")
                            .header("Authorization", validToken)
                            .param("personStickerId", UUID.randomUUID().toString()))
                    .andExpect(status().is(oneOf(200, 204)))
                    .andReturn();
            assertNot5xx(resSticker);
        }

        @Test
        @DisplayName("POST & PATCH /api/person-stickers - Happy & Unhappy paths")
        void personStickers_crud() throws Exception {
            PersonStickerDto createDto = new PersonStickerDto();
            createDto.setPersonId(seededPerson.getPersonId());
            createDto.setShiftId(seededShift.getShiftId());
            createDto.setGateId(seededGate.getGateId());

            MvcResult postRes = mockMvc.perform(post("/api/person-stickers")
                            .header("Authorization", validToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(createDto)))
                    .andExpect(status().isCreated())
                    .andReturn();
            assertNot5xx(postRes);

            PersonSticker created = objectMapper.readValue(postRes.getResponse().getContentAsString(), PersonSticker.class);

            PersonStickerPatchDto patchDto = new PersonStickerPatchDto();
            patchDto.setGateId(seededGate.getGateId());
            MvcResult patchRes = mockMvc.perform(patch("/api/person-stickers/" + created.getPersonStickerId())
                            .header("Authorization", validToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(patchDto)))
                    .andExpect(status().isOk())
                    .andReturn();
            assertNot5xx(patchRes);

            // Unhappy: Patch non-existent ID returns 404
            MvcResult unhappyRes = mockMvc.perform(patch("/api/person-stickers/" + UUID.randomUUID())
                            .header("Authorization", validToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(patchDto)))
                    .andExpect(status().isNotFound())
                    .andReturn();
            assertNot5xx(unhappyRes);
        }
    }

    // =========================================================================
    // 5. PERSONS
    // =========================================================================
    @Nested
    @DisplayName("Persons Endpoints")
    class PersonsTests {

        @Test
        @DisplayName("GET /api/persons - Happy & Unhappy auth")
        void getPersons_happyAndUnhappy() throws Exception {
            MvcResult res = mockMvc.perform(get("/api/persons")
                            .header("Authorization", validToken))
                    .andExpect(status().is(oneOf(200, 204)))
                    .andReturn();
            assertNot5xx(res);

            // Unhappy: Missing auth
            mockMvc.perform(get("/api/persons"))
                    .andExpect(status().is(oneOf(401, 403)));
        }

        @Test
        @DisplayName("POST & PATCH /api/persons - Happy & Unhappy paths")
        void persons_crud() throws Exception {
            PersonDto createDto = new PersonDto();
            createDto.setFirstName("Alex");
            createDto.setLastName("Morgan");
            createDto.setNotes("Volunteer");
            createDto.setExternalId("VOL-1234");

            MvcResult postRes = mockMvc.perform(post("/api/persons")
                            .header("Authorization", validToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(createDto)))
                    .andExpect(status().isCreated())
                    .andReturn();
            assertNot5xx(postRes);

            Person created = objectMapper.readValue(postRes.getResponse().getContentAsString(), Person.class);

            PersonPatchDto patchDto = new PersonPatchDto();
            patchDto.setJobRole("Ticket Seller");
            MvcResult patchRes = mockMvc.perform(patch("/api/persons/" + created.getPersonId())
                            .header("Authorization", validToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(patchDto)))
                    .andExpect(status().isOk())
                    .andReturn();
            assertNot5xx(patchRes);

            // Unhappy: Patch non-existent person returns 404
            MvcResult unhappyRes = mockMvc.perform(patch("/api/persons/" + UUID.randomUUID())
                            .header("Authorization", validToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(patchDto)))
                    .andExpect(status().isNotFound())
                    .andReturn();
            assertNot5xx(unhappyRes);
        }

        @Test
        @DisplayName("POST /api/persons/bulk - Happy & Unhappy")
        void persons_bulk() throws Exception {
            PersonBulkDto bulkDto = new PersonBulkDto();
            bulkDto.setFirstName("Chris");
            bulkDto.setLastName("Evans");
            bulkDto.setShiftId(seededShift.getShiftId());
            bulkDto.setOrganization("Marvel");
            bulkDto.setJobRole("Ticket Seller");

            MvcResult res = mockMvc.perform(post("/api/persons/bulk")
                            .header("Authorization", validToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(List.of(bulkDto))))
                    .andExpect(status().isOk())
                    .andReturn();
            assertNot5xx(res);
        }
    }

    // =========================================================================
    // 6. SHIFTS, GATES, GATE CAPTAINS, OPERATIONS
    // =========================================================================
    @Nested
    @DisplayName("Lookup Entities Endpoints")
    class LookupEntitiesTests {

        @Test
        @DisplayName("GET /api/shifts and /api/shifts/active - Happy & Unhappy")
        void shifts() throws Exception {
            MvcResult resAll = mockMvc.perform(get("/api/shifts")
                            .header("Authorization", validToken))
                    .andExpect(status().is(oneOf(200, 204)))
                    .andReturn();
            assertNot5xx(resAll);

            MvcResult resActive = mockMvc.perform(get("/api/shifts/active")
                            .header("Authorization", validToken))
                    .andExpect(status().is(oneOf(200, 204)))
                    .andReturn();
            assertNot5xx(resActive);

            // Unhappy: No auth
            mockMvc.perform(get("/api/shifts"))
                    .andExpect(status().is(oneOf(401, 403)));
        }

        @Test
        @DisplayName("GET /api/gates and /api/gates?isActive=true - Happy & Unhappy")
        void gates() throws Exception {
            MvcResult resAll = mockMvc.perform(get("/api/gates")
                            .header("Authorization", validToken))
                    .andExpect(status().is(oneOf(200, 204)))
                    .andReturn();
            assertNot5xx(resAll);

            MvcResult resActive = mockMvc.perform(get("/api/gates")
                            .header("Authorization", validToken)
                            .param("isActive", "true"))
                    .andExpect(status().is(oneOf(200, 204)))
                    .andReturn();
            assertNot5xx(resActive);

            // Unhappy: No auth
            mockMvc.perform(get("/api/gates"))
                    .andExpect(status().is(oneOf(401, 403)));
        }

        @Test
        @DisplayName("GET /api/gate-captains - Happy & Unhappy")
        void gateCaptains() throws Exception {
            MvcResult res = mockMvc.perform(get("/api/gate-captains")
                            .header("Authorization", validToken))
                    .andExpect(status().is(oneOf(200, 204)))
                    .andReturn();
            assertNot5xx(res);

            // Unhappy: No auth
            mockMvc.perform(get("/api/gate-captains"))
                    .andExpect(status().is(oneOf(401, 403)));
        }

        @Test
        @DisplayName("GET /api/operations?isActive=true - Happy & Unhappy")
        void operations() throws Exception {
            MvcResult res = mockMvc.perform(get("/api/operations")
                            .header("Authorization", validToken)
                            .param("isActive", "true"))
                    .andExpect(status().is(oneOf(200, 204)))
                    .andReturn();
            assertNot5xx(res);

            // Unhappy: No auth
            mockMvc.perform(get("/api/operations"))
                    .andExpect(status().is(oneOf(401, 403)));
        }

        @Test
        @DisplayName("GET /api/assets/{operationId}?search=... - Happy & Unhappy")
        void unifiedSearch() throws Exception {
            MvcResult res = mockMvc.perform(get("/api/assets/" + seededOperation.getOperationId())
                            .header("Authorization", validToken)
                            .param("search", "100"))
                    .andExpect(status().is(oneOf(200, 204)))
                    .andReturn();
            assertNot5xx(res);

            // Unhappy: Non-existent op UUID returns 204, never 500
            MvcResult unhappyRes = mockMvc.perform(get("/api/assets/" + UUID.randomUUID())
                            .header("Authorization", validToken)
                            .param("search", "100"))
                    .andExpect(status().is(oneOf(200, 204)))
                    .andReturn();
            assertNot5xx(unhappyRes);
        }
    }

    // =========================================================================
    // 7. GATE ASSIGNMENTS (ALL WORKERS, QUOTAS, PRESETS, ALLOCATION, STICKERS)
    // =========================================================================
    @Nested
    @DisplayName("Gate Assignments Full Workspace Endpoints")
    class GateAssignmentsTests {

        @Test
        @DisplayName("GET /api/gate-assignments/requirements - Happy & Unhappy")
        void requirements() throws Exception {
            MvcResult res = mockMvc.perform(get("/api/gate-assignments/requirements")
                            .header("Authorization", validToken)
                            .param("shiftId", seededShift.getShiftId().toString()))
                    .andExpect(status().isOk())
                    .andReturn();
            assertNot5xx(res);

            // Unhappy: Non-existent shift returns 404
            MvcResult unhappyRes = mockMvc.perform(get("/api/gate-assignments/requirements")
                            .header("Authorization", validToken)
                            .param("shiftId", UUID.randomUUID().toString()))
                    .andExpect(status().isNotFound())
                    .andReturn();
            assertNot5xx(unhappyRes);
        }

        @Test
        @DisplayName("POST /api/gate-assignments/requirements/template - Happy & Unhappy")
        void applyTemplate() throws Exception {
            MvcResult res = mockMvc.perform(post("/api/gate-assignments/requirements/template")
                            .header("Authorization", validToken)
                            .param("shiftId", seededShift.getShiftId().toString())
                            .param("template", "BUSY"))
                    .andExpect(status().isOk())
                    .andReturn();
            assertNot5xx(res);

            // Unhappy: Non-existent shift returns 404
            MvcResult unhappyRes = mockMvc.perform(post("/api/gate-assignments/requirements/template")
                            .header("Authorization", validToken)
                            .param("shiftId", UUID.randomUUID().toString())
                            .param("template", "BUSY"))
                    .andExpect(status().isNotFound())
                    .andReturn();
            assertNot5xx(unhappyRes);
        }

        @Test
        @DisplayName("GET /api/gate-assignments/presets - Happy & Unhappy")
        void presets() throws Exception {
            MvcResult res = mockMvc.perform(get("/api/gate-assignments/presets")
                            .header("Authorization", validToken)
                            .param("presetType", "BUSY"))
                    .andExpect(status().isOk())
                    .andReturn();
            assertNot5xx(res);

            // Unhappy: Missing auth
            mockMvc.perform(get("/api/gate-assignments/presets"))
                    .andExpect(status().is(oneOf(401, 403)));
        }

        @Test
        @DisplayName("GET /api/gate-assignments/workers - Happy & Unhappy")
        void workers() throws Exception {
            MvcResult res = mockMvc.perform(get("/api/gate-assignments/workers")
                            .header("Authorization", validToken)
                            .param("shiftId", seededShift.getShiftId().toString()))
                    .andExpect(status().isOk())
                    .andReturn();
            assertNot5xx(res);

            // Unhappy: Non-existent shift returns 200 (empty) or 404
            MvcResult unhappyRes = mockMvc.perform(get("/api/gate-assignments/workers")
                            .header("Authorization", validToken)
                            .param("shiftId", UUID.randomUUID().toString()))
                    .andExpect(status().is(oneOf(200, 404)))
                    .andReturn();
            assertNot5xx(unhappyRes);
        }

        @Test
        @DisplayName("POST /api/gate-assignments/workers/parse - Happy & Unhappy")
        void parseWorkers() throws Exception {
            String csvContent = "First Name,Last Name,Job Role,Organization\nJohn,Smith,Ticket Seller,Albuquerque Rotary\n";
            MockMultipartFile file = new MockMultipartFile("file", "roster.csv", "text/csv", csvContent.getBytes());

            MvcResult res = mockMvc.perform(multipart("/api/gate-assignments/workers/parse")
                            .file(file)
                            .header("Authorization", validToken))
                    .andExpect(status().isOk())
                    .andReturn();
            assertNot5xx(res);

            // Unhappy: Missing auth
            mockMvc.perform(multipart("/api/gate-assignments/workers/parse").file(file))
                    .andExpect(status().is(oneOf(401, 403)));
        }

        @Test
        @DisplayName("POST /api/gate-assignments/workers/bulk - Happy & Unhappy")
        void bulkUploadWorkers() throws Exception {
            PersonBulkWorkerDto worker = new PersonBulkWorkerDto();
            worker.setFirstName("Morgan");
            worker.setLastName("Freeman");
            worker.setJobRole("Ticket Scanner");
            worker.setOrganization("Hollywood Org");

            MvcResult res = mockMvc.perform(post("/api/gate-assignments/workers/bulk")
                            .header("Authorization", validToken)
                            .param("shiftId", seededShift.getShiftId().toString())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(List.of(worker))))
                    .andExpect(status().isOk())
                    .andReturn();
            assertNot5xx(res);

            // Unhappy: Non-existent shift returns 404
            MvcResult unhappyRes = mockMvc.perform(post("/api/gate-assignments/workers/bulk")
                            .header("Authorization", validToken)
                            .param("shiftId", UUID.randomUUID().toString())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(List.of(worker))))
                    .andExpect(status().isNotFound())
                    .andReturn();
            assertNot5xx(unhappyRes);
        }

        @Test
        @DisplayName("POST /api/gate-assignments/assign - Happy & Unhappy")
        void assignGates() throws Exception {
            MvcResult res = mockMvc.perform(post("/api/gate-assignments/assign")
                            .header("Authorization", validToken)
                            .param("shiftId", seededShift.getShiftId().toString()))
                    .andExpect(status().isOk())
                    .andReturn();
            assertNot5xx(res);

            // Unhappy: Non-existent shift returns 404
            MvcResult unhappyRes = mockMvc.perform(post("/api/gate-assignments/assign")
                            .header("Authorization", validToken)
                            .param("shiftId", UUID.randomUUID().toString()))
                    .andExpect(status().isNotFound())
                    .andReturn();
            assertNot5xx(unhappyRes);
        }

        @Test
        @DisplayName("GET /api/gate-assignments/summary - Happy & Unhappy")
        void summary() throws Exception {
            MvcResult res = mockMvc.perform(get("/api/gate-assignments/summary")
                            .header("Authorization", validToken)
                            .param("shiftId", seededShift.getShiftId().toString()))
                    .andExpect(status().isOk())
                    .andReturn();
            assertNot5xx(res);

            // Unhappy: Non-existent shift returns 200 (zeroed) or 404
            MvcResult unhappyRes = mockMvc.perform(get("/api/gate-assignments/summary")
                            .header("Authorization", validToken)
                            .param("shiftId", UUID.randomUUID().toString()))
                    .andExpect(status().is(oneOf(200, 404)))
                    .andReturn();
            assertNot5xx(unhappyRes);
        }

        @Test
        @DisplayName("GET /api/gate-assignments/preferences - Happy & Unhappy")
        void preferences() throws Exception {
            MvcResult res = mockMvc.perform(get("/api/gate-assignments/preferences")
                            .header("Authorization", validToken)
                            .param("shiftId", seededShift.getShiftId().toString()))
                    .andExpect(status().isOk())
                    .andReturn();
            assertNot5xx(res);

            // Unhappy: Non-existent shift returns 200 (empty) or 404
            MvcResult unhappyRes = mockMvc.perform(get("/api/gate-assignments/preferences")
                            .header("Authorization", validToken)
                            .param("shiftId", UUID.randomUUID().toString()))
                    .andExpect(status().is(oneOf(200, 404)))
                    .andReturn();
            assertNot5xx(unhappyRes);
        }

        @Test
        @DisplayName("GET /api/gate-assignments/export - Happy & Unhappy (Excel byte stream)")
        void exportExcel() throws Exception {
            MvcResult res = mockMvc.perform(get("/api/gate-assignments/export")
                            .header("Authorization", validToken)
                            .param("shiftId", seededShift.getShiftId().toString()))
                    .andExpect(status().isOk())
                    .andExpect(header().string("Content-Type", containsString("spreadsheetml")))
                    .andReturn();
            assertNot5xx(res);

            // Unhappy: Non-existent shift returns 404
            MvcResult unhappyRes = mockMvc.perform(get("/api/gate-assignments/export")
                            .header("Authorization", validToken)
                            .param("shiftId", UUID.randomUUID().toString()))
                    .andExpect(status().isNotFound())
                    .andReturn();
            assertNot5xx(unhappyRes);
        }

        @Test
        @DisplayName("GET, POST, & POST /bulk /api/gate-assignments/stickers - Happy & Unhappy")
        void gateAssignmentStickers() throws Exception {
            // GET stickers - Happy
            MvcResult getRes = mockMvc.perform(get("/api/gate-assignments/stickers")
                            .header("Authorization", validToken)
                            .param("shiftId", seededShift.getShiftId().toString()))
                    .andExpect(status().isOk())
                    .andReturn();
            assertNot5xx(getRes);

            // GET stickers - Unhappy: Non-existent shift returns 404
            MvcResult unhappyGet = mockMvc.perform(get("/api/gate-assignments/stickers")
                            .header("Authorization", validToken)
                            .param("shiftId", UUID.randomUUID().toString()))
                    .andExpect(status().isNotFound())
                    .andReturn();
            assertNot5xx(unhappyGet);

            // POST sticker - Happy
            PersonStickerDto stickerDto = new PersonStickerDto();
            stickerDto.setPersonId(seededPerson.getPersonId());
            stickerDto.setShiftId(seededShift.getShiftId());
            stickerDto.setGateId(seededGate.getGateId());

            MvcResult postRes = mockMvc.perform(post("/api/gate-assignments/stickers")
                            .header("Authorization", validToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(stickerDto)))
                    .andExpect(status().isOk())
                    .andReturn();
            assertNot5xx(postRes);

            // POST stickers/bulk - Happy
            MvcResult bulkRes = mockMvc.perform(post("/api/gate-assignments/stickers/bulk")
                            .header("Authorization", validToken)
                            .param("shiftId", seededShift.getShiftId().toString()))
                    .andExpect(status().isOk())
                    .andReturn();
            assertNot5xx(bulkRes);

            // POST stickers/bulk - Unhappy: Non-existent shift returns 404
            MvcResult unhappyBulk = mockMvc.perform(post("/api/gate-assignments/stickers/bulk")
                            .header("Authorization", validToken)
                            .param("shiftId", UUID.randomUUID().toString()))
                    .andExpect(status().isNotFound())
                    .andReturn();
            assertNot5xx(unhappyBulk);
        }
    }

    // =========================================================================
    // 8. SALES TRACKER (SNAPSHOTS, ROLES, MULTI-DEVICE PERSISTENCE)
    // =========================================================================
    @Nested
    @DisplayName("Sales Tracker Endpoints")
    class SalesTrackerTests {

        @Test
        @DisplayName("GET /api/sales-tracker/{shiftId}/snapshots - Happy & Unhappy paths")
        void getSnapshots() throws Exception {
            // Happy path: Valid shift returns 200 (empty list or existing snapshots)
            MvcResult res = mockMvc.perform(get("/api/sales-tracker/{shiftId}/snapshots", seededShift.getShiftId())
                            .header("Authorization", validToken))
                    .andExpect(status().isOk())
                    .andReturn();
            assertNot5xx(res);

            // Unhappy path: Non-existent shift returns 200 empty list, never 500
            MvcResult unhappyRes = mockMvc.perform(get("/api/sales-tracker/{shiftId}/snapshots", UUID.randomUUID())
                            .header("Authorization", validToken))
                    .andExpect(status().isOk())
                    .andReturn();
            assertNot5xx(unhappyRes);

            // Unhappy path: Missing auth returns 401/403
            mockMvc.perform(get("/api/sales-tracker/{shiftId}/snapshots", seededShift.getShiftId()))
                    .andExpect(status().is(oneOf(401, 403)));
        }

        @Test
        @DisplayName("POST /api/sales-tracker/{shiftId}/snapshots - Happy & Unhappy paths")
        void recordSnapshot() throws Exception {
            ShiftSalesDto dto = ShiftSalesDto.builder()
                    .shiftId(seededShift.getShiftId())
                    .recordedAt(java.time.Instant.now())
                    .totalSales(new BigDecimal("2500.00"))
                    .laborCost(new BigDecimal("350.00"))
                    .windowSales(List.of(
                            WindowSalesDto.builder()
                                    .windowName("Gate 1 Window 1")
                                    .salesAmount(new BigDecimal("2500.00"))
                                    .build()
                    ))
                    .build();

            // Happy path: Record snapshot
            MvcResult postRes = mockMvc.perform(post("/api/sales-tracker/{shiftId}/snapshots", seededShift.getShiftId())
                            .header("Authorization", validToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(dto)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.totalSales").value(2500.00))
                    .andReturn();
            assertNot5xx(postRes);

            // Unhappy path: Non-existent shift returns 404, never 500
            MvcResult unhappyRes = mockMvc.perform(post("/api/sales-tracker/{shiftId}/snapshots", UUID.randomUUID())
                            .header("Authorization", validToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(dto)))
                    .andExpect(status().isNotFound())
                    .andReturn();
            assertNot5xx(unhappyRes);
        }

        @Test
        @DisplayName("GET & PUT /api/sales-tracker/{shiftId}/roles - Happy & Unhappy paths")
        void rolesCrud() throws Exception {
            // Happy path: GET default or existing roles
            MvcResult getRes = mockMvc.perform(get("/api/sales-tracker/{shiftId}/roles", seededShift.getShiftId())
                            .header("Authorization", validToken))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", not(empty())))
                    .andReturn();
            assertNot5xx(getRes);

            // Happy path: PUT updated roles
            List<ShiftWorkerDto> updateList = List.of(
                    ShiftWorkerDto.builder().roleName("Cashier").hourlyRate(new BigDecimal("16.50")).count(6).build(),
                    ShiftWorkerDto.builder().roleName("Gate Lead").hourlyRate(new BigDecimal("22.00")).count(2).build()
            );

            MvcResult putRes = mockMvc.perform(put("/api/sales-tracker/{shiftId}/roles", seededShift.getShiftId())
                            .header("Authorization", validToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(updateList)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(2)))
                    .andReturn();
            assertNot5xx(putRes);

            // Unhappy path: Non-existent shift returns 404, never 500
            MvcResult unhappyPut = mockMvc.perform(put("/api/sales-tracker/{shiftId}/roles", UUID.randomUUID())
                            .header("Authorization", validToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(updateList)))
                    .andExpect(status().isNotFound())
                    .andReturn();
            assertNot5xx(unhappyPut);
        }

        @Test
        @DisplayName("DELETE /api/sales-tracker/{shiftId}/reset - Happy & Unhappy paths")
        void resetSales() throws Exception {
            // Happy path: Reset shift sales
            MvcResult delRes = mockMvc.perform(delete("/api/sales-tracker/{shiftId}/reset", seededShift.getShiftId())
                            .header("Authorization", validToken))
                    .andExpect(status().isNoContent())
                    .andReturn();
            assertNot5xx(delRes);

            // Unhappy path: Missing auth returns 401/403
            mockMvc.perform(delete("/api/sales-tracker/{shiftId}/reset", seededShift.getShiftId()))
                    .andExpect(status().is(oneOf(401, 403)));
        }
    }

    @Nested
    @DisplayName("Seller Tracking Workspace Endpoints")
    class SellerTrackingEndpoints {

        @Test
        @DisplayName("GET /api/seller-tracking/performance - Happy & Unhappy paths")
        void getSellerPerformance() throws Exception {
            // Happy path: Authorized request returns 200 with list of sellers
            MvcResult res = mockMvc.perform(get("/api/seller-tracking/performance")
                            .header("Authorization", validToken))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", isA(List.class)))
                    .andReturn();
            assertNot5xx(res);

            // Happy path with filters
            MvcResult filteredRes = mockMvc.perform(get("/api/seller-tracking/performance")
                            .header("Authorization", validToken)
                            .param("operationId", seededOperation.getOperationId().toString())
                            .param("shiftId", seededShift.getShiftId().toString())
                            .param("gateId", seededGate.getGateId().toString())
                            .param("search", "test"))
                    .andExpect(status().isOk())
                    .andReturn();
            assertNot5xx(filteredRes);

            // Unhappy path: Missing auth returns 401/403
            mockMvc.perform(get("/api/seller-tracking/performance"))
                    .andExpect(status().is(oneOf(401, 403)));
        }
    }

    @Nested
    @DisplayName("Active Check-in Session Tracking Endpoints")
    class ActiveCheckinSessionEndpoints {

        @Test
        @DisplayName("POST /api/check-in/sessions/heartbeat, GET /active, POST /close - Happy & Unhappy paths")
        void activeCheckinSessionLifecycle() throws Exception {
            ActiveCheckinSessionDto requestDto = ActiveCheckinSessionDto.builder()
                    .gateId(seededGate.getGateId())
                    .shiftId(seededShift.getShiftId())
                    .username("integration_worker")
                    .userDisplayName("Integration Worker")
                    .build();

            // 1. Heartbeat - Happy path
            MvcResult hbRes = mockMvc.perform(post("/api/check-in/sessions/heartbeat")
                            .header("Authorization", validToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(requestDto)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.username").value("integration_worker"))
                    .andExpect(jsonPath("$.status").value("ACTIVE"))
                    .andExpect(jsonPath("$.gateName").value(seededGate.getName()))
                    .andReturn();
            assertNot5xx(hbRes);

            // 2. Active Sessions - Happy path
            MvcResult activeRes = mockMvc.perform(get("/api/check-in/sessions/active")
                            .header("Authorization", validToken))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", isA(List.class)))
                    .andReturn();
            assertNot5xx(activeRes);

            // 3. Close Session - Happy path
            MvcResult closeRes = mockMvc.perform(post("/api/check-in/sessions/close")
                            .header("Authorization", validToken)
                            .param("username", "integration_worker"))
                    .andExpect(status().isNoContent())
                    .andReturn();
            assertNot5xx(closeRes);

            // 4. Unhappy path: Missing auth returns 401/403
            mockMvc.perform(get("/api/check-in/sessions/active"))
                    .andExpect(status().is(oneOf(401, 403)));
        }
    }
}


