package com.kahua.assets.service;

import com.kahua.assets.dto.*;
import com.kahua.assets.entity.*;
import com.kahua.assets.repository.*;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class GateAssignmentServiceTest {

    @Mock private PersonRepository personRepository;
    @Mock private ShiftGateRequirementRepository requirementRepository;
    @Mock private OrgGatePreferenceRepository preferenceRepository;
    @Mock private ShiftRepository shiftRepository;
    @Mock private GateRepository gateRepository;
    @Mock private PersonService personService;
    @Mock private PersonStickerRepository personStickerRepository;
    @Mock private GatePresetQuotaRepository gatePresetQuotaRepository;

    @InjectMocks
    private GateAssignmentService gateAssignmentService;

    private UUID shiftId;
    private Shift mockShift;
    private List<Gate> mockGates;

    @BeforeEach
    void setUp() {
        shiftId = UUID.randomUUID();
        mockShift = new Shift();
        mockShift.setShiftId(shiftId);
        mockShift.setName("1");

        mockGates = new ArrayList<>();
        int order = 1;
        for (String name : List.of("32", "30", "9", "12", "20", "21")) {
            Gate g = new Gate();
            g.setGateId(UUID.randomUUID());
            g.setName(name);
            g.setIsActive(true);
            g.setDisplayOrder(order++);
            mockGates.add(g);
        }
    }

    @Test
    @DisplayName("applyTemplate('BUSY') should generate 76 total positions across active gates")
    void testApplyTemplate_busy() {
        when(gatePresetQuotaRepository.findByPresetTypeIgnoreCase("BUSY")).thenReturn(Collections.emptyList());
        when(shiftRepository.findById(shiftId)).thenReturn(Optional.of(mockShift));
        when(gateRepository.findAll()).thenReturn(mockGates);
        when(gateRepository.findById(any(UUID.class))).thenAnswer(i -> {
            UUID id = i.getArgument(0);
            return mockGates.stream().filter(g -> g.getGateId().equals(id)).findFirst();
        });
        when(requirementRepository.findByShift_ShiftIdAndGate_GateIdAndJobRole(any(), any(), any()))
                .thenReturn(Optional.empty());
        when(requirementRepository.save(any(ShiftGateRequirement.class))).thenAnswer(i -> i.getArgument(0));

        List<ShiftGateRequirementDto> results = gateAssignmentService.applyTemplate(shiftId, "BUSY");

        int totalPositions = results.stream().mapToInt(ShiftGateRequirementDto::getWorkersNeeded).sum();
        assertThat(totalPositions).isEqualTo(76);
    }

    @Test
    @DisplayName("applyTemplate('MODERATE') should generate 48 total positions across active gates")
    void testApplyTemplate_moderate() {
        when(gatePresetQuotaRepository.findByPresetTypeIgnoreCase("MODERATE")).thenReturn(Collections.emptyList());
        when(shiftRepository.findById(shiftId)).thenReturn(Optional.of(mockShift));
        when(gateRepository.findAll()).thenReturn(mockGates);
        when(gateRepository.findById(any(UUID.class))).thenAnswer(i -> {
            UUID id = i.getArgument(0);
            return mockGates.stream().filter(g -> g.getGateId().equals(id)).findFirst();
        });
        when(requirementRepository.findByShift_ShiftIdAndGate_GateIdAndJobRole(any(), any(), any()))
                .thenReturn(Optional.empty());
        when(requirementRepository.save(any(ShiftGateRequirement.class))).thenAnswer(i -> i.getArgument(0));

        List<ShiftGateRequirementDto> results = gateAssignmentService.applyTemplate(shiftId, "MODERATE");

        int totalPositions = results.stream().mapToInt(ShiftGateRequirementDto::getWorkersNeeded).sum();
        assertThat(totalPositions).isEqualTo(48);
    }

    @Test
    @DisplayName("parseWorkersFile should strip formula injection characters and sanitize names")
    void testParseWorkersFile_csvSanitization() {
        String csv = "First Name,Last Name,Organization,Job Role,Gate\n"
                + "=Alice,@Smith,+Rotary,-Ticket Seller,32\n"
                + "Bob;DROP TABLE,O'Connor,Lions Club,Scanner,20\n";

        MockMultipartFile file = new MockMultipartFile(
                "file", "volunteers.csv", "text/csv", csv.getBytes(StandardCharsets.UTF_8)
        );

        List<PersonBulkWorkerDto> parsed = gateAssignmentService.parseWorkersFile(file);

        assertThat(parsed).hasSize(2);
        assertThat(parsed.get(0).getFirstName()).isEqualTo("Alice");
        assertThat(parsed.get(0).getLastName()).isEqualTo("Smith");
        assertThat(parsed.get(0).getOrganization()).isEqualTo("Rotary");
        assertThat(parsed.get(0).getJobRole()).isEqualTo("Ticket Seller");
        assertThat(parsed.get(0).getGateName()).isEqualTo("32");

        assertThat(parsed.get(1).getFirstName()).isEqualTo("BobDROP TABLE"); // semicolons and special chars stripped
        assertThat(parsed.get(1).getLastName()).isEqualTo("O'Connor"); // apostrophe preserved
        assertThat(parsed.get(1).getJobRole()).isEqualTo("Ticket Scanner");
    }

    @Test
    @DisplayName("assignGates should honor Companion Clustering (Rule 4) and prevent solitary stranding")
    void testAssignGates_companionClustering() {
        when(shiftRepository.findById(shiftId)).thenReturn(Optional.of(mockShift));
        when(gateRepository.findAll()).thenReturn(mockGates);

        // Gate 32 capacity: 4 sellers; Gate 20 capacity: 4 sellers
        Gate gate32 = mockGates.stream().filter(g -> g.getName().equals("32")).findFirst().get();
        Gate gate20 = mockGates.stream().filter(g -> g.getName().equals("20")).findFirst().get();

        ShiftGateRequirement req32 = new ShiftGateRequirement();
        req32.setGate(gate32);
        req32.setJobRole("Ticket Seller");
        req32.setWorkersNeeded(5);
        req32.setMinWorkers(2);
        req32.setHappyWorkers(4);
        req32.setPriority(1);

        ShiftGateRequirement req20 = new ShiftGateRequirement();
        req20.setGate(gate20);
        req20.setJobRole("Ticket Seller");
        req20.setWorkersNeeded(4);
        req20.setMinWorkers(2);
        req20.setHappyWorkers(4);
        req20.setPriority(2);

        when(requirementRepository.findByShift_ShiftId(shiftId)).thenReturn(List.of(req32, req20));
        when(preferenceRepository.findByShift_ShiftId(shiftId)).thenReturn(Collections.emptyList());

        // Org A has 4 ticket sellers -> should be allocated together (not split 3+1)
        List<Person> workers = new ArrayList<>();
        for (int i = 1; i <= 4; i++) {
            Person p = new Person();
            p.setPersonId(UUID.randomUUID());
            p.setFirstName("OrgA_" + i);
            p.setLastName("Member");
            p.setOrganization("Org A");
            p.setJobRole("Ticket Seller");
            p.setIsActive(true);
            workers.add(p);
        }

        // Org B has 4 ticket sellers -> should fit cleanly into the other gate
        for (int i = 1; i <= 4; i++) {
            Person p = new Person();
            p.setPersonId(UUID.randomUUID());
            p.setFirstName("OrgB_" + i);
            p.setLastName("Member");
            p.setOrganization("Org B");
            p.setJobRole("Ticket Seller");
            p.setIsActive(true);
            workers.add(p);
        }

        // Solitary volunteer Org C (size 1)
        Person solitary = new Person();
        solitary.setPersonId(UUID.randomUUID());
        solitary.setFirstName("Solo");
        solitary.setLastName("Walker");
        solitary.setOrganization("Solitary Org");
        solitary.setJobRole("Ticket Seller");
        solitary.setIsActive(true);
        workers.add(solitary);

        when(personRepository.findByShift_ShiftIdAndIsActiveTrue(shiftId)).thenReturn(workers);

        List<Person> assigned = gateAssignmentService.assignGates(shiftId);

        // Org A members should all be at the same gate
        Set<Gate> orgAGates = new HashSet<>();
        for (int i = 0; i < 4; i++) {
            orgAGates.add(assigned.get(i).getGate());
        }
        assertThat(orgAGates).hasSize(1);

        // Org B members should all be at the same gate
        Set<Gate> orgBGates = new HashSet<>();
        for (int i = 4; i < 8; i++) {
            orgBGates.add(assigned.get(i).getGate());
        }
        assertThat(orgBGates).hasSize(1);

        // Solitary volunteer should be assigned to an active gate
        assertThat(assigned.get(8).getGate()).isNotNull();
    }

    @Test
    @DisplayName("assignGates should honor OrgGatePreference and co-locate scanners (Rule 3)")
    void testAssignGates_orgPreferenceAndScannerCoLocation() {
        when(shiftRepository.findById(shiftId)).thenReturn(Optional.of(mockShift));
        when(gateRepository.findAll()).thenReturn(mockGates);

        Gate gate20 = mockGates.stream().filter(g -> g.getName().equals("20")).findFirst().get();

        ShiftGateRequirement reqSeller = new ShiftGateRequirement();
        reqSeller.setGate(gate20);
        reqSeller.setJobRole("Ticket Seller");
        reqSeller.setWorkersNeeded(4);

        ShiftGateRequirement reqScanner = new ShiftGateRequirement();
        reqScanner.setGate(gate20);
        reqScanner.setJobRole("Scanner");
        reqScanner.setWorkersNeeded(4);

        when(requirementRepository.findByShift_ShiftId(shiftId)).thenReturn(List.of(reqSeller, reqScanner));

        // Preference: Org Alpha prefers Gate 20
        OrgGatePreference pref = new OrgGatePreference();
        pref.setOrganization("Org Alpha");
        pref.setGate(gate20);
        pref.setPriority(1);
        when(preferenceRepository.findByShift_ShiftId(shiftId)).thenReturn(List.of(pref));

        List<Person> workers = new ArrayList<>();
        // 2 Sellers from Org Alpha
        for (int i = 1; i <= 2; i++) {
            Person p = new Person();
            p.setPersonId(UUID.randomUUID());
            p.setFirstName("Seller_" + i);
            p.setLastName("Alpha");
            p.setOrganization("Org Alpha");
            p.setJobRole("Ticket Seller");
            p.setIsActive(true);
            workers.add(p);
        }
        // 2 Scanners from Org Alpha
        for (int i = 1; i <= 2; i++) {
            Person p = new Person();
            p.setPersonId(UUID.randomUUID());
            p.setFirstName("Scanner_" + i);
            p.setLastName("Alpha");
            p.setOrganization("Org Alpha");
            p.setJobRole("Scanner");
            p.setIsActive(true);
            workers.add(p);
        }

        when(personRepository.findByShift_ShiftIdAndIsActiveTrue(shiftId)).thenReturn(workers);

        List<Person> assigned = gateAssignmentService.assignGates(shiftId);

        // Both sellers and scanners from Org Alpha should be placed at Gate 20
        for (Person p : assigned) {
            assertThat(p.getGate().getName()).isEqualTo("20");
        }
    }

    @Test
    @DisplayName("exportAssignmentsExcel should generate valid SXSSFWorkbook with Roster and Summary sheets")
    void testExportAssignmentsExcel() throws Exception {
        when(shiftRepository.findById(shiftId)).thenReturn(Optional.of(mockShift));
        when(gateRepository.findAll()).thenReturn(mockGates);

        Gate gate32 = mockGates.get(0);
        Person p = new Person();
        p.setPersonId(UUID.randomUUID());
        p.setFirstName("John");
        p.setLastName("Doe");
        p.setOrganization("Kiwanis");
        p.setJobRole("Ticket Seller");
        p.setGate(gate32);
        p.setIsActive(true);

        when(personRepository.findByShift_ShiftId(shiftId)).thenReturn(List.of(p));
        when(personRepository.findByShift_ShiftIdAndIsActiveTrue(shiftId)).thenReturn(List.of(p));
        when(requirementRepository.findByShift_ShiftId(shiftId)).thenReturn(Collections.emptyList());

        byte[] bytes = gateAssignmentService.exportAssignmentsExcel(shiftId);
        assertThat(bytes).isNotEmpty();

        try (Workbook wb = WorkbookFactory.create(new ByteArrayInputStream(bytes))) {
            assertThat(wb.getNumberOfSheets()).isEqualTo(2);

            Sheet sheet1 = wb.getSheetAt(0);
            assertThat(sheet1.getSheetName()).contains("AlbuquerqueInternationalBal");
            assertThat(sheet1.getRow(0).getCell(0).getStringCellValue()).isEqualTo("First Name");
            assertThat(sheet1.getRow(1).getCell(0).getStringCellValue()).isEqualTo("John");
            assertThat(sheet1.getRow(1).getCell(1).getStringCellValue()).isEqualTo("Doe");
            assertThat(sheet1.getRow(1).getCell(2).getStringCellValue()).isEqualTo("Kiwanis");

            Sheet sheet2 = wb.getSheet("Summary");
            assertThat(sheet2).isNotNull();
            assertThat(sheet2.getRow(0).getCell(0).getStringCellValue()).isEqualTo("Gate");
        }
    }

    @Test
    @DisplayName("assignGates should allocate both Ticket Seller and Ticket Scanner positions so not everyone is a seller")
    void testAssignGates_allocatesBothRoles() {
        when(shiftRepository.findById(shiftId)).thenReturn(Optional.of(mockShift));
        when(gateRepository.findAll()).thenReturn(mockGates);

        Gate gate32 = mockGates.stream().filter(g -> g.getName().equals("32")).findFirst().get();
        ShiftGateRequirement reqSeller = new ShiftGateRequirement();
        reqSeller.setGate(gate32);
        reqSeller.setJobRole("Ticket Seller");
        reqSeller.setWorkersNeeded(2);

        ShiftGateRequirement reqScanner = new ShiftGateRequirement();
        reqScanner.setGate(gate32);
        reqScanner.setJobRole("Ticket Scanner");
        reqScanner.setWorkersNeeded(2);

        when(requirementRepository.findByShift_ShiftId(shiftId)).thenReturn(List.of(reqSeller, reqScanner));
        when(preferenceRepository.findByShift_ShiftId(shiftId)).thenReturn(Collections.emptyList());

        List<Person> workers = new ArrayList<>();
        for (int i = 1; i <= 2; i++) {
            Person p = new Person();
            p.setPersonId(UUID.randomUUID());
            p.setFirstName("Vol_Seller_" + i);
            p.setLastName("Member");
            p.setOrganization("Civic Group");
            p.setJobRole("Ticket Seller");
            p.setIsActive(true);
            workers.add(p);
        }
        for (int i = 1; i <= 2; i++) {
            Person p = new Person();
            p.setPersonId(UUID.randomUUID());
            p.setFirstName("Vol_Scanner_" + i);
            p.setLastName("Member");
            p.setOrganization("Civic Group");
            p.setJobRole("Ticket Scanner");
            p.setIsActive(true);
            workers.add(p);
        }

        when(personRepository.findByShift_ShiftIdAndIsActiveTrue(shiftId)).thenReturn(workers);

        List<Person> assigned = gateAssignmentService.assignGates(shiftId);

        long sellerCount = assigned.stream().filter(p -> "Ticket Seller".equals(p.getJobRole()) && p.getGate() != null).count();
        long scannerCount = assigned.stream().filter(p -> "Ticket Scanner".equals(p.getJobRole()) && p.getGate() != null).count();

        assertThat(sellerCount).isEqualTo(2);
        assertThat(scannerCount).isEqualTo(2);
    }

    @Test
    @DisplayName("assignGates should fulfill tiers in priority order and route leftover workers to unassigned bucket")
    void testAssignGates_multiTierAllocationAndUnassignedBucket() {
        when(shiftRepository.findById(shiftId)).thenReturn(Optional.of(mockShift));
        when(gateRepository.findAll()).thenReturn(mockGates);

        Gate gate32 = mockGates.stream().filter(g -> g.getName().equals("32")).findFirst().get();
        Gate gate20 = mockGates.stream().filter(g -> g.getName().equals("20")).findFirst().get();

        ShiftGateRequirement req32 = new ShiftGateRequirement();
        req32.setGate(gate32);
        req32.setJobRole("Ticket Seller");
        req32.setMinWorkers(2);
        req32.setHappyWorkers(4);
        req32.setWorkersNeeded(6);
        req32.setPriority(1);

        ShiftGateRequirement req20 = new ShiftGateRequirement();
        req20.setGate(gate20);
        req20.setJobRole("Ticket Seller");
        req20.setMinWorkers(2);
        req20.setHappyWorkers(3);
        req20.setWorkersNeeded(4);
        req20.setPriority(2);

        when(requirementRepository.findByShift_ShiftId(shiftId)).thenReturn(List.of(req32, req20));
        when(preferenceRepository.findByShift_ShiftId(shiftId)).thenReturn(Collections.emptyList());

        // 12 individual workers: 10 should be allocated (6 at Gate 32, 4 at Gate 20), 2 should be in unassigned bucket
        List<Person> workers = new ArrayList<>();
        for (int i = 1; i <= 12; i++) {
            Person p = new Person();
            p.setPersonId(UUID.randomUUID());
            p.setFirstName("Worker_" + i);
            p.setLastName("Test");
            p.setOrganization("Solo_" + i); // separate orgs to avoid companion clustering
            p.setJobRole("Ticket Seller");
            p.setIsActive(true);
            workers.add(p);
        }

        when(personRepository.findByShift_ShiftIdAndIsActiveTrue(shiftId)).thenReturn(workers);

        List<Person> assigned = gateAssignmentService.assignGates(shiftId);

        long gate32Count = assigned.stream().filter(p -> p.getGate() != null && "32".equals(p.getGate().getName())).count();
        long gate20Count = assigned.stream().filter(p -> p.getGate() != null && "20".equals(p.getGate().getName())).count();
        long unassignedCount = assigned.stream().filter(p -> p.getGate() == null).count();

        assertThat(gate32Count).isEqualTo(6);
        assertThat(gate20Count).isEqualTo(4);
        assertThat(unassignedCount).isEqualTo(2);
    }

    @Test
    @DisplayName("assignGates in deficit floor defense should round-robin by priority and establish buddy pairs")
    void testAssignGates_deficitFloorDefenseRoundRobinWithBuddyPairs() {
        when(shiftRepository.findById(shiftId)).thenReturn(Optional.of(mockShift));
        when(gateRepository.findAll()).thenReturn(mockGates);

        // Gate 32 (Priority 1, Min 2, Planned 4)
        // Gate 20 (Priority 2, Min 2, Planned 4)
        // Gate 12 (Priority 3, Min 2, Planned 4)
        Gate gate32 = mockGates.stream().filter(g -> g.getName().equals("32")).findFirst().get();
        Gate gate20 = mockGates.stream().filter(g -> g.getName().equals("20")).findFirst().get();
        Gate gate12 = mockGates.stream().filter(g -> g.getName().equals("12")).findFirst().get();

        ShiftGateRequirement req32 = new ShiftGateRequirement();
        req32.setGate(gate32);
        req32.setJobRole("Ticket Seller");
        req32.setMinWorkers(2);
        req32.setHappyWorkers(3);
        req32.setWorkersNeeded(4);
        req32.setPriority(1);

        ShiftGateRequirement req20 = new ShiftGateRequirement();
        req20.setGate(gate20);
        req20.setJobRole("Ticket Seller");
        req20.setMinWorkers(2);
        req20.setHappyWorkers(3);
        req20.setWorkersNeeded(4);
        req20.setPriority(2);

        ShiftGateRequirement req12 = new ShiftGateRequirement();
        req12.setGate(gate12);
        req12.setJobRole("Ticket Seller");
        req12.setMinWorkers(2);
        req12.setHappyWorkers(3);
        req12.setWorkersNeeded(4);
        req12.setPriority(3);

        when(requirementRepository.findByShift_ShiftId(shiftId)).thenReturn(List.of(req32, req20, req12));
        when(preferenceRepository.findByShift_ShiftId(shiftId)).thenReturn(Collections.emptyList());

        // Total min demand is 2 + 2 + 2 = 6.
        // If we only have 4 workers (deficit of 2):
        // Pass 1 gives 1 to Gate 32, 1 to Gate 20, 1 to Gate 12.
        // Pass 2 gives 2nd to Gate 32 (highest priority) to form a buddy pair.
        // Result: Gate 32 has 2 (buddy pair satisfied!), Gate 20 has 1, Gate 12 has 1.
        List<Person> workers = new ArrayList<>();
        for (int i = 1; i <= 4; i++) {
            Person p = new Person();
            p.setPersonId(UUID.randomUUID());
            p.setFirstName("Worker_" + i);
            p.setLastName("Deficit");
            p.setJobRole("Ticket Seller");
            p.setIsActive(true);
            workers.add(p);
        }

        when(personRepository.findByShift_ShiftIdAndIsActiveTrue(shiftId)).thenReturn(workers);

        List<Person> assigned = gateAssignmentService.assignGates(shiftId);

        long gate32Count = assigned.stream().filter(p -> p.getGate() != null && "32".equals(p.getGate().getName())).count();
        long gate20Count = assigned.stream().filter(p -> p.getGate() != null && "20".equals(p.getGate().getName())).count();
        long gate12Count = assigned.stream().filter(p -> p.getGate() != null && "12".equals(p.getGate().getName())).count();

        assertThat(gate32Count).isEqualTo(2); // Buddy pair established on priority 1 gate
        assertThat(gate20Count).isEqualTo(1); // Covered with 1 person
        assertThat(gate12Count).isEqualTo(1); // Covered with 1 person
    }

    @Test
    @DisplayName("createOrGetSellerSticker should be idempotent and update gate when changed")
    void testCreateOrGetSellerSticker_idempotent() {
        UUID personId = UUID.randomUUID();
        Gate gate32 = mockGates.stream().filter(g -> g.getName().equals("32")).findFirst().get();
        Gate gate20 = mockGates.stream().filter(g -> g.getName().equals("20")).findFirst().get();

        Person p = new Person();
        p.setPersonId(personId);
        p.setFirstName("John");
        p.setLastName("Doe");
        p.setExternalId("V-12345");

        when(personRepository.findById(personId)).thenReturn(Optional.of(p));
        when(shiftRepository.findById(shiftId)).thenReturn(Optional.of(mockShift));
        when(gateRepository.findById(gate32.getGateId())).thenReturn(Optional.of(gate32));
        when(gateRepository.findById(gate20.getGateId())).thenReturn(Optional.of(gate20));

        // 1. Initial creation
        when(personStickerRepository.findFirstByPerson_PersonIdAndShift_ShiftId(personId, shiftId)).thenReturn(null);
        when(personStickerRepository.save(any(PersonSticker.class))).thenAnswer(i -> {
            PersonSticker s = i.getArgument(0);
            s.setPersonStickerId(UUID.randomUUID());
            return s;
        });

        PersonStickerDto created = gateAssignmentService.createOrGetSellerSticker(shiftId, personId, gate32.getGateId());
        assertThat(created).isNotNull();
        assertThat(created.getGateId()).isEqualTo(gate32.getGateId());
        assertThat(created.getExternalId()).isEqualTo("V-12345");

        // 2. Subsequent call with new gate should update existing sticker without error
        PersonSticker existing = new PersonSticker();
        existing.setPersonStickerId(created.getPersonStickerId());
        existing.setPerson(p);
        existing.setShift(mockShift);
        existing.setGate(gate32);

        when(personStickerRepository.findFirstByPerson_PersonIdAndShift_ShiftId(personId, shiftId)).thenReturn(existing);

        PersonStickerDto updated = gateAssignmentService.createOrGetSellerSticker(shiftId, personId, gate20.getGateId());
        assertThat(updated.getGateId()).isEqualTo(gate20.getGateId());
        verify(personStickerRepository, times(2)).save(any(PersonSticker.class));
    }

    @Test
    @DisplayName("assignGates should fill all planned quotas for multiple gates with interleaved mixed pool without premature break")
    void testAssignGates_mixedPoolFillsAllPlannedQuotasWithoutPrematureBreak() {
        when(shiftRepository.findById(shiftId)).thenReturn(Optional.of(mockShift));
        when(gateRepository.findAll()).thenReturn(mockGates);

        Gate gate32 = mockGates.stream().filter(g -> g.getName().equals("32")).findFirst().get();
        Gate gate20 = mockGates.stream().filter(g -> g.getName().equals("20")).findFirst().get();

        // Gate 32: 8 planned sellers (min 4, happy 6), 6 planned scanners (min 2, happy 4)
        ShiftGateRequirement req32Seller = new ShiftGateRequirement();
        req32Seller.setGate(gate32);
        req32Seller.setJobRole("Ticket Seller");
        req32Seller.setMinWorkers(4);
        req32Seller.setHappyWorkers(6);
        req32Seller.setWorkersNeeded(8);
        req32Seller.setPriority(1);

        ShiftGateRequirement req32Scanner = new ShiftGateRequirement();
        req32Scanner.setGate(gate32);
        req32Scanner.setJobRole("Ticket Scanner");
        req32Scanner.setMinWorkers(2);
        req32Scanner.setHappyWorkers(4);
        req32Scanner.setWorkersNeeded(6);
        req32Scanner.setPriority(1);

        // Gate 20: 6 planned sellers (min 2, happy 4), 4 planned scanners (min 2, happy 4)
        ShiftGateRequirement req20Seller = new ShiftGateRequirement();
        req20Seller.setGate(gate20);
        req20Seller.setJobRole("Ticket Seller");
        req20Seller.setMinWorkers(2);
        req20Seller.setHappyWorkers(4);
        req20Seller.setWorkersNeeded(6);
        req20Seller.setPriority(2);

        ShiftGateRequirement req20Scanner = new ShiftGateRequirement();
        req20Scanner.setGate(gate20);
        req20Scanner.setJobRole("Ticket Scanner");
        req20Scanner.setMinWorkers(2);
        req20Scanner.setHappyWorkers(4);
        req20Scanner.setWorkersNeeded(4);
        req20Scanner.setPriority(2);

        when(requirementRepository.findByShift_ShiftId(shiftId))
                .thenReturn(List.of(req32Seller, req32Scanner, req20Seller, req20Scanner));
        when(preferenceRepository.findByShift_ShiftId(shiftId)).thenReturn(Collections.emptyList());

        // Create interleaved pool: 14 sellers and 10 scanners (24 total), starting with scanners
        List<Person> workers = new ArrayList<>();
        int sIdx = 1;
        int scIdx = 1;
        for (int i = 0; i < 24; i++) {
            Person p = new Person();
            p.setPersonId(UUID.randomUUID());
            p.setIsActive(true);
            p.setOrganization("Solo_" + i);

            // Interleave: scanner at even indices when available
            if (i % 2 == 0 && scIdx <= 10) {
                p.setFirstName("Scanner_" + scIdx++);
                p.setLastName("Test");
                p.setJobRole("Ticket Scanner");
            } else if (sIdx <= 14) {
                p.setFirstName("Seller_" + sIdx++);
                p.setLastName("Test");
                p.setJobRole("Ticket Seller");
            } else {
                p.setFirstName("Scanner_" + scIdx++);
                p.setLastName("Test");
                p.setJobRole("Ticket Scanner");
            }
            workers.add(p);
        }

        when(personRepository.findByShift_ShiftIdAndIsActiveTrue(shiftId)).thenReturn(workers);

        List<Person> assigned = gateAssignmentService.assignGates(shiftId);

        long gate32Sellers = assigned.stream()
                .filter(p -> p.getGate() != null && "32".equals(p.getGate().getName()) && "Ticket Seller".equals(p.getJobRole()))
                .count();
        long gate32Scanners = assigned.stream()
                .filter(p -> p.getGate() != null && "32".equals(p.getGate().getName()) && "Ticket Scanner".equals(p.getJobRole()))
                .count();

        long gate20Sellers = assigned.stream()
                .filter(p -> p.getGate() != null && "20".equals(p.getGate().getName()) && "Ticket Seller".equals(p.getJobRole()))
                .count();
        long gate20Scanners = assigned.stream()
                .filter(p -> p.getGate() != null && "20".equals(p.getGate().getName()) && "Ticket Scanner".equals(p.getJobRole()))
                .count();

        long unassigned = assigned.stream().filter(p -> p.getGate() == null).count();

        assertThat(gate32Sellers).isEqualTo(8);
        assertThat(gate32Scanners).isEqualTo(6);
        assertThat(gate20Sellers).isEqualTo(6);
        assertThat(gate20Scanners).isEqualTo(4);
        assertThat(unassigned).isEqualTo(0);
    }

    @Test
    @DisplayName("applyTemplate should use custom preset quotas when configured in database")
    void testApplyTemplate_withCustomPresets() {
        Gate g32 = mockGates.stream().filter(g -> g.getName().equals("32")).findFirst().orElseThrow();
        GatePresetQuota customQ = new GatePresetQuota();
        customQ.setPresetQuotaId(UUID.randomUUID());
        customQ.setPresetType("BUSY");
        customQ.setGate(g32);
        customQ.setJobRole("Ticket Seller");
        customQ.setPlannedWorkers(14);
        customQ.setMinWorkers(4);
        customQ.setHappyWorkers(8);
        customQ.setPriority(1);

        when(gatePresetQuotaRepository.findByPresetTypeIgnoreCase("BUSY")).thenReturn(List.of(customQ));
        when(shiftRepository.findById(shiftId)).thenReturn(Optional.of(mockShift));
        when(gateRepository.findById(g32.getGateId())).thenReturn(Optional.of(g32));
        when(requirementRepository.findByShift_ShiftIdAndGate_GateIdAndJobRole(shiftId, g32.getGateId(), "Ticket Seller"))
                .thenReturn(Optional.empty());
        when(requirementRepository.save(any(ShiftGateRequirement.class))).thenAnswer(i -> i.getArgument(0));

        List<ShiftGateRequirementDto> results = gateAssignmentService.applyTemplate(shiftId, "BUSY");

        assertThat(results).hasSize(1);
        assertThat(results.get(0).getWorkersNeeded()).isEqualTo(14);
        assertThat(results.get(0).getMinWorkers()).isEqualTo(4);
        assertThat(results.get(0).getHappyWorkers()).isEqualTo(8);
    }

    @Test
    @DisplayName("getPresetQuotas should return baseline defaults when no database rows exist")
    void testGetPresetQuotas_baselineFallback() {
        when(gatePresetQuotaRepository.findByPresetTypeIgnoreCase("LITE")).thenReturn(Collections.emptyList());
        when(gateRepository.findAll()).thenReturn(mockGates);

        List<GatePresetQuotaDto> presets = gateAssignmentService.getPresetQuotas("LITE");

        assertThat(presets).isNotEmpty();
        // 6 gates * 2 roles = 12 items
        assertThat(presets).hasSize(12);
        // Gate 32 in LITE has 4 planned
        GatePresetQuotaDto g32Seller = presets.stream()
                .filter(p -> "32".equals(p.getGateName()) && "Ticket Seller".equals(p.getJobRole()))
                .findFirst().orElse(null);
        assertThat(g32Seller).isNotNull();
        assertThat(g32Seller.getPlannedWorkers()).isEqualTo(4);
    }

    @Test
    @DisplayName("updatePresetQuotas should persist customized quotas and return updated DTOs")
    void testUpdatePresetQuotas() {
        Gate g20 = mockGates.stream().filter(g -> g.getName().equals("20")).findFirst().orElseThrow();
        when(gateRepository.findById(g20.getGateId())).thenReturn(Optional.of(g20));
        when(gatePresetQuotaRepository.findByPresetTypeIgnoreCaseAndGate_GateIdAndJobRole("BUSY", g20.getGateId(), "Ticket Seller"))
                .thenReturn(Optional.empty());
        when(gatePresetQuotaRepository.save(any(GatePresetQuota.class))).thenAnswer(i -> {
            GatePresetQuota q = i.getArgument(0);
            q.setPresetQuotaId(UUID.randomUUID());
            return q;
        });

        GatePresetQuotaDto dto = GatePresetQuotaDto.builder()
                .gateId(g20.getGateId())
                .jobRole("Ticket Seller")
                .plannedWorkers(12)
                .minWorkers(3)
                .happyWorkers(7)
                .priority(1)
                .build();

        List<GatePresetQuotaDto> updated = gateAssignmentService.updatePresetQuotas("BUSY", List.of(dto));

        assertThat(updated).hasSize(1);
        assertThat(updated.get(0).getPlannedWorkers()).isEqualTo(12);
        assertThat(updated.get(0).getMinWorkers()).isEqualTo(3);
        assertThat(updated.get(0).getHappyWorkers()).isEqualTo(7);
        assertThat(updated.get(0).getPriority()).isEqualTo(1);
    }

    @Test
    @DisplayName("getRequirements should return all active gates even if only partial requirements exist")
    void testGetRequirements_mergesAllActiveGates() {
        Gate g32 = mockGates.stream().filter(g -> g.getName().equals("32")).findFirst().orElseThrow();
        ShiftGateRequirement req = new ShiftGateRequirement();
        req.setRequirementId(UUID.randomUUID());
        req.setShift(mockShift);
        req.setGate(g32);
        req.setJobRole("Ticket Seller");
        req.setWorkersNeeded(8);
        req.setMinWorkers(4);
        req.setHappyWorkers(6);
        req.setPriority(1);

        when(shiftRepository.findById(shiftId)).thenReturn(Optional.of(mockShift));
        when(requirementRepository.findByShift_ShiftId(shiftId)).thenReturn(List.of(req));
        when(gateRepository.findAll()).thenReturn(mockGates);

        List<ShiftGateRequirementDto> result = gateAssignmentService.getRequirements(shiftId);

        // 6 gates * 2 roles = 12 requirements
        assertThat(result).hasSize(12);
        ShiftGateRequirementDto foundG32Seller = result.stream()
                .filter(r -> r.getGateId().equals(g32.getGateId()) && "Ticket Seller".equals(r.getJobRole()))
                .findFirst().orElse(null);
        assertThat(foundG32Seller).isNotNull();
        assertThat(foundG32Seller.getWorkersNeeded()).isEqualTo(8);
        assertThat(foundG32Seller.getMinWorkers()).isEqualTo(4);
        assertThat(foundG32Seller.getHappyWorkers()).isEqualTo(6);

        // A gate without prior requirements defaults to 0
        Gate g30 = mockGates.stream().filter(g -> g.getName().equals("30")).findFirst().orElseThrow();
        ShiftGateRequirementDto foundG30Scanner = result.stream()
                .filter(r -> r.getGateId().equals(g30.getGateId()) && "Ticket Scanner".equals(r.getJobRole()))
                .findFirst().orElse(null);
        assertThat(foundG30Scanner).isNotNull();
        assertThat(foundG30Scanner.getWorkersNeeded()).isEqualTo(0);
    }

    @Test
    @DisplayName("assignGates should prevent zero-seller starvation across active gates when sellers are available")
    void testAssignGates_preventsZeroSellersWhenSellersAvailable() {
        when(shiftRepository.findById(shiftId)).thenReturn(Optional.of(mockShift));
        when(gateRepository.findAll()).thenReturn(mockGates);

        // 6 gates each requiring 4 sellers (min 2) and 4 scanners (min 2)
        // Total min seller demand = 12.
        List<ShiftGateRequirement> reqs = new ArrayList<>();
        for (Gate g : mockGates) {
            ShiftGateRequirement reqSeller = new ShiftGateRequirement();
            reqSeller.setGate(g);
            reqSeller.setJobRole("Ticket Seller");
            reqSeller.setMinWorkers(2);
            reqSeller.setHappyWorkers(3);
            reqSeller.setWorkersNeeded(4);
            reqSeller.setPriority(List.of("32", "20").contains(g.getName()) ? 1 : 2);
            reqs.add(reqSeller);

            ShiftGateRequirement reqScanner = new ShiftGateRequirement();
            reqScanner.setGate(g);
            reqScanner.setJobRole("Ticket Scanner");
            reqScanner.setMinWorkers(2);
            reqScanner.setHappyWorkers(3);
            reqScanner.setWorkersNeeded(4);
            reqScanner.setPriority(List.of("32", "20").contains(g.getName()) ? 1 : 2);
            reqs.add(reqScanner);
        }

        when(requirementRepository.findByShift_ShiftId(shiftId)).thenReturn(reqs);
        when(preferenceRepository.findByShift_ShiftId(shiftId)).thenReturn(Collections.emptyList());

        // Pool: only 8 Ticket Sellers available (deficit vs 12 min), plus 20 Ticket Scanners
        List<Person> workers = new ArrayList<>();
        for (int i = 1; i <= 8; i++) {
            Person p = new Person();
            p.setPersonId(UUID.randomUUID());
            p.setFirstName("Seller_" + i);
            p.setLastName("Test");
            p.setJobRole("Ticket Seller");
            p.setIsActive(true);
            workers.add(p);
        }
        for (int i = 1; i <= 20; i++) {
            Person p = new Person();
            p.setPersonId(UUID.randomUUID());
            p.setFirstName("Scanner_" + i);
            p.setLastName("Test");
            p.setJobRole("Ticket Scanner");
            p.setIsActive(true);
            workers.add(p);
        }

        when(personRepository.findByShift_ShiftIdAndIsActiveTrue(shiftId)).thenReturn(workers);

        List<Person> assigned = gateAssignmentService.assignGates(shiftId);

        // Crucial Check: Every one of the 6 active gates MUST receive at least 1 Ticket Seller!
        // No gate with a quota > 0 can be starved to 0 sellers while sellers are available.
        for (Gate g : mockGates) {
            long gateSellers = assigned.stream()
                    .filter(p -> p.getGate() != null && g.getGateId().equals(p.getGate().getGateId()) && "Ticket Seller".equals(p.getJobRole()))
                    .count();
            assertThat(gateSellers)
                    .as("Gate %s must have at least 1 seller assigned", g.getName())
                    .isGreaterThanOrEqualTo(1);
        }

        // Priority 1 gates (32 and 20) should have established buddy pairs (2 sellers each)
        long gate32Sellers = assigned.stream()
                .filter(p -> p.getGate() != null && "32".equals(p.getGate().getName()) && "Ticket Seller".equals(p.getJobRole()))
                .count();
        long gate20Sellers = assigned.stream()
                .filter(p -> p.getGate() != null && "20".equals(p.getGate().getName()) && "Ticket Seller".equals(p.getJobRole()))
                .count();
        assertThat(gate32Sellers).isEqualTo(2);
        assertThat(gate20Sellers).isEqualTo(2);
    }

    @Test
    @DisplayName("assignGates should co-locate ticket scanners with ticket sellers from the same organization via org affinity")
    void testAssignGates_scannerOrgAffinityWithSellersAtSameGate() {
        when(shiftRepository.findById(shiftId)).thenReturn(Optional.of(mockShift));
        when(gateRepository.findAll()).thenReturn(mockGates);

        Gate gate32 = mockGates.stream().filter(g -> g.getName().equals("32")).findFirst().get();
        Gate gate20 = mockGates.stream().filter(g -> g.getName().equals("20")).findFirst().get();

        ShiftGateRequirement req32Seller = new ShiftGateRequirement();
        req32Seller.setGate(gate32);
        req32Seller.setJobRole("Ticket Seller");
        req32Seller.setWorkersNeeded(2);
        req32Seller.setMinWorkers(2);
        req32Seller.setPriority(1);

        ShiftGateRequirement req32Scanner = new ShiftGateRequirement();
        req32Scanner.setGate(gate32);
        req32Scanner.setJobRole("Ticket Scanner");
        req32Scanner.setWorkersNeeded(2);
        req32Scanner.setMinWorkers(2);
        req32Scanner.setPriority(1);

        ShiftGateRequirement req20Seller = new ShiftGateRequirement();
        req20Seller.setGate(gate20);
        req20Seller.setJobRole("Ticket Seller");
        req20Seller.setWorkersNeeded(2);
        req20Seller.setMinWorkers(2);
        req20Seller.setPriority(2);

        ShiftGateRequirement req20Scanner = new ShiftGateRequirement();
        req20Scanner.setGate(gate20);
        req20Scanner.setJobRole("Ticket Scanner");
        req20Scanner.setWorkersNeeded(2);
        req20Scanner.setMinWorkers(2);
        req20Scanner.setPriority(2);

        when(requirementRepository.findByShift_ShiftId(shiftId))
                .thenReturn(List.of(req32Seller, req32Scanner, req20Seller, req20Scanner));
        when(preferenceRepository.findByShift_ShiftId(shiftId)).thenReturn(Collections.emptyList());

        // Pool:
        // Kiwanis Club has 2 Sellers and 2 Scanners
        // Rotary Club has 2 Sellers and 2 Scanners
        List<Person> workers = new ArrayList<>();
        for (int i = 1; i <= 2; i++) {
            Person p = new Person();
            p.setPersonId(UUID.randomUUID());
            p.setFirstName("Kiwanis_Seller_" + i);
            p.setLastName("Member");
            p.setOrganization("Kiwanis Club");
            p.setJobRole("Ticket Seller");
            p.setIsActive(true);
            workers.add(p);
        }
        for (int i = 1; i <= 2; i++) {
            Person p = new Person();
            p.setPersonId(UUID.randomUUID());
            p.setFirstName("Rotary_Seller_" + i);
            p.setLastName("Member");
            p.setOrganization("Rotary Club");
            p.setJobRole("Ticket Seller");
            p.setIsActive(true);
            workers.add(p);
        }
        for (int i = 1; i <= 2; i++) {
            Person p = new Person();
            p.setPersonId(UUID.randomUUID());
            p.setFirstName("Kiwanis_Scanner_" + i);
            p.setLastName("Member");
            p.setOrganization("Kiwanis Club");
            p.setJobRole("Ticket Scanner");
            p.setIsActive(true);
            workers.add(p);
        }
        for (int i = 1; i <= 2; i++) {
            Person p = new Person();
            p.setPersonId(UUID.randomUUID());
            p.setFirstName("Rotary_Scanner_" + i);
            p.setLastName("Member");
            p.setOrganization("Rotary Club");
            p.setJobRole("Ticket Scanner");
            p.setIsActive(true);
            workers.add(p);
        }

        when(personRepository.findByShift_ShiftIdAndIsActiveTrue(shiftId)).thenReturn(workers);

        List<Person> assigned = gateAssignmentService.assignGates(shiftId);

        // Find which gate Kiwanis sellers were assigned to
        Set<UUID> kiwanisSellerGates = assigned.stream()
                .filter(p -> "Kiwanis Club".equals(p.getOrganization()) && "Ticket Seller".equals(p.getJobRole()))
                .map(p -> p.getGate().getGateId())
                .collect(Collectors.toSet());
        assertThat(kiwanisSellerGates).hasSize(1);
        UUID kiwanisGateId = kiwanisSellerGates.iterator().next();

        // Kiwanis scanners MUST be assigned to the exact same gate as Kiwanis sellers!
        Set<UUID> kiwanisScannerGates = assigned.stream()
                .filter(p -> "Kiwanis Club".equals(p.getOrganization()) && "Ticket Scanner".equals(p.getJobRole()))
                .map(p -> p.getGate().getGateId())
                .collect(Collectors.toSet());
        assertThat(kiwanisScannerGates).containsExactly(kiwanisGateId);

        // Find which gate Rotary sellers were assigned to
        Set<UUID> rotarySellerGates = assigned.stream()
                .filter(p -> "Rotary Club".equals(p.getOrganization()) && "Ticket Seller".equals(p.getJobRole()))
                .map(p -> p.getGate().getGateId())
                .collect(Collectors.toSet());
        assertThat(rotarySellerGates).hasSize(1);
        UUID rotaryGateId = rotarySellerGates.iterator().next();

        // Rotary scanners MUST be assigned to the exact same gate as Rotary sellers!
        Set<UUID> rotaryScannerGates = assigned.stream()
                .filter(p -> "Rotary Club".equals(p.getOrganization()) && "Ticket Scanner".equals(p.getJobRole()))
                .map(p -> p.getGate().getGateId())
                .collect(Collectors.toSet());
        assertThat(rotaryScannerGates).containsExactly(rotaryGateId);
    }

    @Test
    @DisplayName("Auxiliary roles like 'Flex / Lead' with 0 workers must NOT overwrite Ticket Seller quotas")
    void testAssignGates_auxiliaryRolesDoNotOverwriteSellerQuotas() {
        when(shiftRepository.findById(shiftId)).thenReturn(Optional.of(mockShift));
        when(gateRepository.findAll()).thenReturn(mockGates);

        Gate gate32 = mockGates.get(0); // Gate 32

        // Requirements list containing Ticket Seller (10), followed by Flex / Lead (0), and Ticket Scanner (8)
        List<ShiftGateRequirement> reqs = new ArrayList<>();

        ShiftGateRequirement sellerReq = new ShiftGateRequirement();
        sellerReq.setShift(mockShift);
        sellerReq.setGate(gate32);
        sellerReq.setJobRole("Ticket Seller");
        sellerReq.setWorkersNeeded(10);
        sellerReq.setMinWorkers(2);
        sellerReq.setHappyWorkers(6);
        sellerReq.setPriority(1);
        reqs.add(sellerReq);

        ShiftGateRequirement flexLeadReq = new ShiftGateRequirement();
        flexLeadReq.setShift(mockShift);
        flexLeadReq.setGate(gate32);
        flexLeadReq.setJobRole("Flex / Lead");
        flexLeadReq.setWorkersNeeded(0);
        flexLeadReq.setMinWorkers(0);
        flexLeadReq.setHappyWorkers(0);
        flexLeadReq.setPriority(1);
        reqs.add(flexLeadReq);

        ShiftGateRequirement scannerReq = new ShiftGateRequirement();
        scannerReq.setShift(mockShift);
        scannerReq.setGate(gate32);
        scannerReq.setJobRole("Ticket Scanner");
        scannerReq.setWorkersNeeded(8);
        scannerReq.setMinWorkers(2);
        scannerReq.setHappyWorkers(6);
        scannerReq.setPriority(1);
        reqs.add(scannerReq);

        when(requirementRepository.findByShift_ShiftId(shiftId)).thenReturn(reqs);

        // Workers: 5 sellers, 4 scanners
        List<Person> workers = new ArrayList<>();
        for (int i = 1; i <= 5; i++) {
            Person p = new Person();
            p.setPersonId(UUID.randomUUID());
            p.setFirstName("Seller_" + i);
            p.setLastName("User");
            p.setJobRole("Ticket Seller");
            p.setIsActive(true);
            workers.add(p);
        }
        for (int i = 1; i <= 4; i++) {
            Person p = new Person();
            p.setPersonId(UUID.randomUUID());
            p.setFirstName("Scanner_" + i);
            p.setLastName("User");
            p.setJobRole("Ticket Scanner");
            p.setIsActive(true);
            workers.add(p);
        }

        when(personRepository.findByShift_ShiftIdAndIsActiveTrue(shiftId)).thenReturn(workers);

        List<Person> assigned = gateAssignmentService.assignGates(shiftId);

        // Crucial assertion: All 5 sellers MUST be assigned to gate 32, NOT 0!
        long assignedSellers = assigned.stream()
                .filter(p -> "Ticket Seller".equals(p.getJobRole()) && p.getGate() != null && gate32.getGateId().equals(p.getGate().getGateId()))
                .count();
        assertThat(assignedSellers).isEqualTo(5);

        long assignedScanners = assigned.stream()
                .filter(p -> "Ticket Scanner".equals(p.getJobRole()) && p.getGate() != null && gate32.getGateId().equals(p.getGate().getGateId()))
                .count();
        assertThat(assignedScanners).isEqualTo(4);

        // Also verify getRequirements does not return 0 for gate 32 sellers
        List<ShiftGateRequirementDto> reqDtos = gateAssignmentService.getRequirements(shiftId);
        ShiftGateRequirementDto sellerDto = reqDtos.stream()
                .filter(r -> r.getGateId().equals(gate32.getGateId()) && "Ticket Seller".equals(r.getJobRole()))
                .findFirst().orElseThrow();
        assertThat(sellerDto.getWorkersNeeded()).isEqualTo(10);

        // Also verify getSummary planned sellers is 10
        GateAssignmentSummaryDto summary = gateAssignmentService.getSummary(shiftId);
        GateAssignmentSummaryDto.GateBreakdownDto gate32Breakdown = summary.getGateBreakdowns().stream()
                .filter(b -> b.getGateId().equals(gate32.getGateId()))
                .findFirst().orElseThrow();
        assertThat(gate32Breakdown.getSellersNeeded()).isEqualTo(10);
        assertThat(gate32Breakdown.getSellersAssigned()).isEqualTo(5);
    }

    @Test
    @DisplayName("bulkUploadWorkers must throw 400 Bad Request when jobRole is not Ticket Seller or Ticket Scanner")
    void testBulkUploadWorkers_rejectsInvalidJobRole() {
        when(shiftRepository.findById(shiftId)).thenReturn(Optional.of(mockShift));

        PersonBulkWorkerDto invalidWorker = PersonBulkWorkerDto.builder()
                .firstName("Test")
                .lastName("User")
                .jobRole("Flex / Lead")
                .build();

        org.junit.jupiter.api.Assertions.assertThrows(
                org.springframework.web.server.ResponseStatusException.class,
                () -> gateAssignmentService.bulkUploadWorkers(shiftId, List.of(invalidWorker))
        );
    }

    @Test
    @DisplayName("parseWorkersFile must preserve invalid job role in preview without defaulting to Ticket Seller")
    void testParseWorkersFile_preservesInvalidJobRoleWithoutDefaulting() {
        String csvContent = "First Name,Last Name,Organization,Job Role\nBob,Smith,Rotary,Volunteer\nAlice,Jones,Kiwanis,Flex / Lead\nCharlie,Brown,None,Scanner";
        MockMultipartFile file = new MockMultipartFile(
                "file", "roster.csv", "text/csv", csvContent.getBytes(StandardCharsets.UTF_8)
        );

        List<PersonBulkWorkerDto> results = gateAssignmentService.parseWorkersFile(file);

        assertThat(results).hasSize(3);
        assertThat(results.get(0).getJobRole()).isEqualTo("Volunteer");
        assertThat(results.get(1).getJobRole()).isEqualTo("Flex / Lead");
        assertThat(results.get(2).getJobRole()).isEqualTo("Ticket Scanner");
    }

    @Test
    @DisplayName("assignGates must NEVER flex workers across roles: extra sellers cannot fill scanner slots")
    void testAssignGates_workersAreNeverFlexedAcrossRoles() {
        when(shiftRepository.findById(shiftId)).thenReturn(Optional.of(mockShift));
        when(gateRepository.findAll()).thenReturn(mockGates);

        Gate gate32 = mockGates.get(0);
        ShiftGateRequirement reqSeller = new ShiftGateRequirement();
        reqSeller.setGate(gate32);
        reqSeller.setJobRole("Ticket Seller");
        reqSeller.setWorkersNeeded(2);

        ShiftGateRequirement reqScanner = new ShiftGateRequirement();
        reqScanner.setGate(gate32);
        reqScanner.setJobRole("Ticket Scanner");
        reqScanner.setWorkersNeeded(2);

        when(requirementRepository.findByShift_ShiftId(shiftId)).thenReturn(List.of(reqSeller, reqScanner));
        when(preferenceRepository.findByShift_ShiftId(shiftId)).thenReturn(Collections.emptyList());

        // 4 workers who are ALL Ticket Sellers.
        // Even though Gate 32 needs 2 Scanners, sellers MUST NEVER be flexed into scanner slots!
        List<Person> workers = new ArrayList<>();
        for (int i = 1; i <= 4; i++) {
            Person p = new Person();
            p.setPersonId(UUID.randomUUID());
            p.setFirstName("Seller_" + i);
            p.setLastName("User");
            p.setJobRole("Ticket Seller");
            p.setIsActive(true);
            workers.add(p);
        }

        when(personRepository.findByShift_ShiftIdAndIsActiveTrue(shiftId)).thenReturn(workers);

        List<Person> assigned = gateAssignmentService.assignGates(shiftId);

        long assignedSellers = assigned.stream().filter(p -> p.getGate() != null && "Ticket Seller".equals(p.getJobRole())).count();
        long assignedScanners = assigned.stream().filter(p -> p.getGate() != null && "Ticket Scanner".equals(p.getJobRole())).count();
        long unassignedSellers = assigned.stream().filter(p -> p.getGate() == null && "Ticket Seller".equals(p.getJobRole())).count();

        // All 4 sellers assigned to gate 32 (absorbing open planned headcount), 0 scanners assigned, 0 unassigned.
        // Role is strictly preserved as Ticket Seller; workers are NEVER flexed to Ticket Scanner!
        assertThat(assignedSellers).isEqualTo(4);
        assertThat(assignedScanners).isEqualTo(0);
        assertThat(unassignedSellers).isEqualTo(0);
        for (Person p : assigned) {
            assertThat(p.getJobRole()).isEqualTo("Ticket Seller");
            assertThat(p.getGate()).isEqualTo(gate32);
        }
    }

    @Test
    @DisplayName("assignGates should prioritize Priority 1 gates to planned before Priority 2 gates receive above-min allocation")
    void testAssignGates_priorityProgression_higherPriorityGetsFilledFirst() {
        when(shiftRepository.findById(shiftId)).thenReturn(Optional.of(mockShift));
        when(gateRepository.findAll()).thenReturn(mockGates);

        Gate gate20 = mockGates.stream().filter(g -> g.getName().equals("20")).findFirst().get();
        Gate gate32 = mockGates.stream().filter(g -> g.getName().equals("32")).findFirst().get();

        ShiftGateRequirement req20 = new ShiftGateRequirement();
        req20.setGate(gate20);
        req20.setJobRole("Ticket Seller");
        req20.setMinWorkers(4);
        req20.setHappyWorkers(6);
        req20.setWorkersNeeded(8);
        req20.setPriority(1); // Priority 1 (Highest)

        ShiftGateRequirement req32 = new ShiftGateRequirement();
        req32.setGate(gate32);
        req32.setJobRole("Ticket Seller");
        req32.setMinWorkers(4);
        req32.setHappyWorkers(6);
        req32.setWorkersNeeded(8);
        req32.setPriority(2); // Priority 2 (Lower)

        when(requirementRepository.findByShift_ShiftId(shiftId)).thenReturn(List.of(req20, req32));
        when(preferenceRepository.findByShift_ShiftId(shiftId)).thenReturn(Collections.emptyList());

        // 14 workers available: both gates reach Happy (6 + 6 = 12), leaving 2 extra.
        // Those 2 extra MUST go to Gate 20 (Priority 1) in Level 3 Planned to bring it to 8/8 Planned!
        // Gate 32 (Priority 2) must remain at 6 (Happy).
        List<Person> workers = new ArrayList<>();
        for (int i = 1; i <= 14; i++) {
            Person p = new Person();
            p.setPersonId(UUID.randomUUID());
            p.setFirstName("Seller_" + i);
            p.setLastName("PriorityTest");
            p.setJobRole("Ticket Seller");
            p.setIsActive(true);
            workers.add(p);
        }

        when(personRepository.findByShift_ShiftIdAndIsActiveTrue(shiftId)).thenReturn(workers);

        List<Person> assigned = gateAssignmentService.assignGates(shiftId);

        long gate20Count = assigned.stream().filter(p -> p.getGate() != null && "20".equals(p.getGate().getName())).count();
        long gate32Count = assigned.stream().filter(p -> p.getGate() != null && "32".equals(p.getGate().getName())).count();

        assertThat(gate20Count).isEqualTo(8); // Priority 1 gate reaches 100% planned (8/8)
        assertThat(gate32Count).isEqualTo(6); // Priority 2 gate receives happy level (6/8)
    }

    @Test
    @DisplayName("assignGates with Approach A: 9 sellers for an 8-station gate splits into balanced 5 and 4 pods")
    void testAssignGates_approachABalancedPodSplitting_preventsSolitaryStranding() {
        when(shiftRepository.findById(shiftId)).thenReturn(Optional.of(mockShift));
        when(gateRepository.findAll()).thenReturn(mockGates);

        Gate gate20 = mockGates.stream().filter(g -> g.getName().equals("20")).findFirst().get();
        Gate gate32 = mockGates.stream().filter(g -> g.getName().equals("32")).findFirst().get();

        ShiftGateRequirement req20 = new ShiftGateRequirement();
        req20.setGate(gate20);
        req20.setJobRole("Ticket Seller");
        req20.setMinWorkers(4);
        req20.setHappyWorkers(6);
        req20.setWorkersNeeded(8);
        req20.setPriority(1); // Priority 1

        ShiftGateRequirement req32 = new ShiftGateRequirement();
        req32.setGate(gate32);
        req32.setJobRole("Ticket Seller");
        req32.setMinWorkers(4);
        req32.setHappyWorkers(6);
        req32.setWorkersNeeded(6);
        req32.setPriority(2); // Priority 2

        when(requirementRepository.findByShift_ShiftId(shiftId)).thenReturn(List.of(req20, req32));
        when(preferenceRepository.findByShift_ShiftId(shiftId)).thenReturn(Collections.emptyList());

        // 9 workers from the same organization "Key Club"
        List<Person> workers = new ArrayList<>();
        for (int i = 1; i <= 9; i++) {
            Person p = new Person();
            p.setPersonId(UUID.randomUUID());
            p.setFirstName("Member_" + i);
            p.setLastName("KeyClub");
            p.setOrganization("Key Club");
            p.setJobRole("Ticket Seller");
            p.setIsActive(true);
            workers.add(p);
        }

        // Plus 5 independent workers to cover other gates min
        for (int i = 1; i <= 5; i++) {
            Person p = new Person();
            p.setPersonId(UUID.randomUUID());
            p.setFirstName("Indep_" + i);
            p.setLastName("Worker");
            p.setOrganization("Individual / None");
            p.setJobRole("Ticket Seller");
            p.setIsActive(true);
            workers.add(p);
        }

        when(personRepository.findByShift_ShiftIdAndIsActiveTrue(shiftId)).thenReturn(workers);

        List<Person> assigned = gateAssignmentService.assignGates(shiftId);

        long keyClubAtGate20 = assigned.stream()
                .filter(p -> p.getGate() != null && "20".equals(p.getGate().getName()) && "Key Club".equals(p.getOrganization()))
                .count();
        long keyClubAtGate32 = assigned.stream()
                .filter(p -> p.getGate() != null && "32".equals(p.getGate().getName()) && "Key Club".equals(p.getOrganization()))
                .count();

        // 9 members split into balanced pods (5 at Gate 20, 4 at Gate 32)
        // Solitary stranding (8 and 1) is completely prevented!
        assertThat(keyClubAtGate20).isEqualTo(5);
        assertThat(keyClubAtGate32).isEqualTo(4);
    }

    @Test
    @DisplayName("assignGates should ensure zero unassigned resources when total planned allocation is unmet")
    void testAssignGates_totalHeadcountAbsorption_zeroUnassignedWhenPlannedUnmet() {
        when(shiftRepository.findById(shiftId)).thenReturn(Optional.of(mockShift));
        when(gateRepository.findAll()).thenReturn(mockGates);

        // Gate 20: 8 sellers needed, 10 scanners needed (total planned 18)
        Gate gate20 = mockGates.stream().filter(g -> g.getName().equals("20")).findFirst().get();
        ShiftGateRequirement reqSeller = new ShiftGateRequirement();
        reqSeller.setGate(gate20);
        reqSeller.setJobRole("Ticket Seller");
        reqSeller.setMinWorkers(4);
        reqSeller.setHappyWorkers(6);
        reqSeller.setWorkersNeeded(8);
        reqSeller.setPriority(1);

        ShiftGateRequirement reqScanner = new ShiftGateRequirement();
        reqScanner.setGate(gate20);
        reqScanner.setJobRole("Ticket Scanner");
        reqScanner.setMinWorkers(6);
        reqScanner.setHappyWorkers(8);
        reqScanner.setWorkersNeeded(10);
        reqScanner.setPriority(1);

        when(requirementRepository.findByShift_ShiftId(shiftId)).thenReturn(List.of(reqSeller, reqScanner));
        when(preferenceRepository.findByShift_ShiftId(shiftId)).thenReturn(Collections.emptyList());

        // Roster has 18 workers total (matching 18 planned), but with role skew:
        // 4 Sellers (deficit of 4) and 14 Scanners (surplus of 4)
        List<Person> workers = new ArrayList<>();
        for (int i = 1; i <= 4; i++) {
            Person p = new Person();
            p.setPersonId(UUID.randomUUID());
            p.setFirstName("Seller_" + i);
            p.setLastName("Absorb");
            p.setJobRole("Ticket Seller");
            p.setIsActive(true);
            workers.add(p);
        }
        for (int i = 1; i <= 14; i++) {
            Person p = new Person();
            p.setPersonId(UUID.randomUUID());
            p.setFirstName("Scanner_" + i);
            p.setLastName("Absorb");
            p.setJobRole("Ticket Scanner");
            p.setIsActive(true);
            workers.add(p);
        }

        when(personRepository.findByShift_ShiftIdAndIsActiveTrue(shiftId)).thenReturn(workers);

        List<Person> assigned = gateAssignmentService.assignGates(shiftId);

        long unassigned = assigned.stream().filter(p -> p.getGate() == null).count();
        long assignedTotal = assigned.stream().filter(p -> p.getGate() != null && "20".equals(p.getGate().getName())).count();
        long assignedScanners = assigned.stream().filter(p -> p.getGate() != null && "Ticket Scanner".equals(p.getJobRole())).count();
        long assignedSellers = assigned.stream().filter(p -> p.getGate() != null && "Ticket Seller".equals(p.getJobRole())).count();

        // All 18 workers are assigned! Zero unassigned resources!
        assertThat(unassigned).isEqualTo(0);
        assertThat(assignedTotal).isEqualTo(18);
        // All 14 scanners keep their Ticket Scanner role!
        assertThat(assignedScanners).isEqualTo(14);
        assertThat(assignedSellers).isEqualTo(4);
    }

    @Test
    @DisplayName("createOrGetSellerSticker should create sticker for Ticket Seller")
    void testCreateOrGetSellerSticker_ticketSeller() {
        UUID personId = UUID.randomUUID();
        Person seller = new Person();
        seller.setPersonId(personId);
        seller.setFirstName("Sally");
        seller.setLastName("Seller");
        seller.setJobRole("Ticket Seller");

        when(personRepository.findById(personId)).thenReturn(Optional.of(seller));
        when(shiftRepository.findById(shiftId)).thenReturn(Optional.of(mockShift));
        when(personStickerRepository.findFirstByPerson_PersonIdAndShift_ShiftId(personId, shiftId)).thenReturn(null);
        when(personStickerRepository.save(any(PersonSticker.class))).thenAnswer(i -> {
            PersonSticker s = i.getArgument(0);
            s.setPersonStickerId(UUID.randomUUID());
            return s;
        });

        PersonStickerDto result = gateAssignmentService.createOrGetSellerSticker(shiftId, personId, null);

        assertThat(result).isNotNull();
        assertThat(result.getPersonId()).isEqualTo(personId);
        assertThat(result.getFirstName()).isEqualTo("Sally");
        assertThat(result.getLastName()).isEqualTo("Seller");
    }

    @Test
    @DisplayName("createOrGetSellerSticker should reject sticker creation for Ticket Scanner with 400 Bad Request")
    void testCreateOrGetSellerSticker_rejectsTicketScanner() {
        UUID personId = UUID.randomUUID();
        Person scanner = new Person();
        scanner.setPersonId(personId);
        scanner.setFirstName("Sam");
        scanner.setLastName("Scanner");
        scanner.setJobRole("Ticket Scanner");

        when(personRepository.findById(personId)).thenReturn(Optional.of(scanner));
        when(shiftRepository.findById(shiftId)).thenReturn(Optional.of(mockShift));

        org.assertj.core.api.Assertions.assertThatThrownBy(() ->
                gateAssignmentService.createOrGetSellerSticker(shiftId, personId, null)
        ).isInstanceOf(org.springframework.web.server.ResponseStatusException.class)
         .hasMessageContaining("Only Ticket Sellers can have QR code stickers generated");
    }
}
