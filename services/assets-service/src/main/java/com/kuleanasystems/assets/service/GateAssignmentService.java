package com.kuleanasystems.assets.service;

import com.kuleanasystems.assets.dto.*;
import com.kuleanasystems.assets.entity.Gate;
import com.kuleanasystems.assets.entity.GatePresetQuota;
import com.kuleanasystems.assets.entity.OrgGatePreference;
import com.kuleanasystems.assets.entity.Person;
import com.kuleanasystems.assets.entity.PersonSticker;
import com.kuleanasystems.assets.entity.Shift;
import com.kuleanasystems.assets.entity.ShiftGateRequirement;
import com.kuleanasystems.assets.repository.GatePresetQuotaRepository;
import com.kuleanasystems.assets.repository.GateRepository;
import com.kuleanasystems.assets.repository.OrgGatePreferenceRepository;
import com.kuleanasystems.assets.repository.PersonRepository;
import com.kuleanasystems.assets.repository.PersonStickerRepository;
import com.kuleanasystems.assets.repository.ShiftGateRequirementRepository;
import com.kuleanasystems.assets.repository.ShiftRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.streaming.SXSSFRow;
import org.apache.poi.xssf.streaming.SXSSFSheet;
import org.apache.poi.xssf.streaming.SXSSFWorkbook;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class GateAssignmentService {

    private final PersonRepository personRepository;
    private final ShiftGateRequirementRepository requirementRepository;
    private final OrgGatePreferenceRepository preferenceRepository;
    private final ShiftRepository shiftRepository;
    private final GateRepository gateRepository;
    private final PersonService personService;
    private final PersonStickerRepository personStickerRepository;
    private final GatePresetQuotaRepository gatePresetQuotaRepository;

    private static final Pattern NAME_WHITELIST = Pattern.compile("^[A-Za-z\\s\\-\\'.]+$");
    private static final Set<String> PRIMARY_GATE_NAMES = Set.of("20", "32", "12");

    // ==========================================
    // 1. Requirements / Quota Management
    // ==========================================

    @Transactional(readOnly = true)
    public List<ShiftGateRequirementDto> getRequirements(UUID shiftId) {
        Shift shift = shiftRepository.findById(shiftId)
                .orElseThrow(() -> new EntityNotFoundException("Shift not found with id: " + shiftId));

        List<ShiftGateRequirement> existing = requirementRepository.findByShift_ShiftId(shiftId);
        Map<String, ShiftGateRequirement> existingMap = new LinkedHashMap<>();
        for (ShiftGateRequirement r : existing) {
            if (r.getGate() == null || r.getGate().getGateId() == null) continue;
            String role = normalizeJobRole(r.getJobRole());
            if (!"Ticket Seller".equalsIgnoreCase(role) && !"Ticket Scanner".equalsIgnoreCase(role)) {
                continue;
            }
            String key = r.getGate().getGateId() + ":" + role;
            ShiftGateRequirement curr = existingMap.get(key);
            if (curr == null || (r.getWorkersNeeded() != null && (curr.getWorkersNeeded() == null || r.getWorkersNeeded() > curr.getWorkersNeeded()))) {
                existingMap.put(key, r);
            }
        }

        // Always return all active gates, merging with existing where present
        List<Gate> activeGates = gateRepository.findAll().stream()
                .filter(g -> Boolean.TRUE.equals(g.getIsActive()))
                .sorted(Comparator.comparing(Gate::getDisplayOrder, Comparator.nullsLast(Comparator.naturalOrder())))
                .toList();

        List<ShiftGateRequirementDto> results = new ArrayList<>();
        for (Gate gate : activeGates) {
            int gatePriority = PRIMARY_GATE_NAMES.contains(gate.getName()) ? 1 : 2;
            for (String role : List.of("Ticket Seller", "Ticket Scanner")) {
                String key = gate.getGateId() + ":" + role;
                if (existingMap.containsKey(key)) {
                    results.add(toRequirementDto(existingMap.get(key)));
                } else {
                    results.add(ShiftGateRequirementDto.builder()
                            .shiftId(shiftId)
                            .gateId(gate.getGateId())
                            .gateName(gate.getName())
                            .jobRole(role)
                            .workersNeeded(0)
                            .minWorkers(0)
                            .happyWorkers(0)
                            .priority(gatePriority)
                            .build());
                }
            }
        }
        return results;
    }

    @Transactional
    public List<ShiftGateRequirementDto> updateRequirements(UUID shiftId, List<ShiftGateRequirementDto> dtos) {
        Shift shift = shiftRepository.findById(shiftId)
                .orElseThrow(() -> new EntityNotFoundException("Shift not found with id: " + shiftId));

        List<ShiftGateRequirementDto> results = new ArrayList<>();
        for (ShiftGateRequirementDto dto : dtos) {
            if (dto.getGateId() == null || dto.getJobRole() == null) {
                continue;
            }
            Gate gate = gateRepository.findById(dto.getGateId())
                    .orElseThrow(() -> new EntityNotFoundException("Gate not found with id: " + dto.getGateId()));

            String normalizedRole = normalizeJobRole(dto.getJobRole());
            ShiftGateRequirement req = requirementRepository
                    .findByShift_ShiftIdAndGate_GateIdAndJobRole(shiftId, dto.getGateId(), normalizedRole)
                    .orElseGet(() -> {
                        ShiftGateRequirement newReq = new ShiftGateRequirement();
                        newReq.setShift(shift);
                        newReq.setGate(gate);
                        newReq.setJobRole(normalizedRole);
                        newReq.setCreatedBy("System");
                        return newReq;
                    });

            int planned = dto.getWorkersNeeded() != null ? Math.max(0, dto.getWorkersNeeded()) : (dto.getPlannedWorkers() != null ? Math.max(0, dto.getPlannedWorkers()) : 0);
            req.setWorkersNeeded(planned);
            req.setMinWorkers(dto.getMinWorkers() != null ? Math.max(0, dto.getMinWorkers()) : (planned >= 2 ? 2 : planned));
            req.setHappyWorkers(dto.getHappyWorkers() != null ? Math.max(0, dto.getHappyWorkers()) : (planned >= 4 ? (int) Math.ceil(planned * 0.7) : planned));
            req.setPriority(dto.getPriority() != null ? Math.max(1, dto.getPriority()) : (PRIMARY_GATE_NAMES.contains(gate.getName()) ? 1 : 2));
            req.setUpdatedBy("System");
            req = requirementRepository.save(req);
            results.add(toRequirementDto(req));
        }
        return results;
    }

    @Transactional
    public List<ShiftGateRequirementDto> applyTemplate(UUID shiftId, String templateName) {
        Shift shift = shiftRepository.findById(shiftId)
                .orElseThrow(() -> new EntityNotFoundException("Shift not found with id: " + shiftId));

        String mode = templateName != null ? templateName.trim().toUpperCase() : "BUSY";
        List<GatePresetQuota> customPresets = gatePresetQuotaRepository.findByPresetTypeIgnoreCase(mode);

        List<ShiftGateRequirementDto> dtoList = new ArrayList<>();
        if (!customPresets.isEmpty()) {
            for (GatePresetQuota q : customPresets) {
                dtoList.add(ShiftGateRequirementDto.builder()
                        .shiftId(shiftId)
                        .gateId(q.getGate().getGateId())
                        .gateName(q.getGate().getName())
                        .jobRole(q.getJobRole())
                        .workersNeeded(q.getPlannedWorkers())
                        .minWorkers(q.getMinWorkers())
                        .happyWorkers(q.getHappyWorkers())
                        .priority(q.getPriority())
                        .build());
            }
        } else {
            List<GatePresetQuotaDto> baseline = getBaselineTemplate(mode);
            for (GatePresetQuotaDto b : baseline) {
                dtoList.add(ShiftGateRequirementDto.builder()
                        .shiftId(shiftId)
                        .gateId(b.getGateId())
                        .gateName(b.getGateName())
                        .jobRole(b.getJobRole())
                        .workersNeeded(b.getPlannedWorkers())
                        .minWorkers(b.getMinWorkers())
                        .happyWorkers(b.getHappyWorkers())
                        .priority(b.getPriority())
                        .build());
            }
        }

        return updateRequirements(shiftId, dtoList);
    }

    @Transactional(readOnly = true)
    public List<GatePresetQuotaDto> getPresetQuotas(String presetType) {
        String type = presetType != null ? presetType.trim().toUpperCase() : "BUSY";
        List<GatePresetQuota> quotas = gatePresetQuotaRepository.findByPresetTypeIgnoreCase(type);
        if (quotas.isEmpty()) {
            return getBaselineTemplate(type);
        }

        Map<String, GatePresetQuota> quotaMap = new LinkedHashMap<>();
        for (GatePresetQuota q : quotas) {
            if (q.getGate() == null || q.getGate().getGateId() == null) continue;
            String role = normalizeJobRole(q.getJobRole());
            if (!"Ticket Seller".equalsIgnoreCase(role) && !"Ticket Scanner".equalsIgnoreCase(role)) {
                continue;
            }
            String key = q.getGate().getGateId() + ":" + role;
            GatePresetQuota curr = quotaMap.get(key);
            if (curr == null || (q.getPlannedWorkers() != null && (curr.getPlannedWorkers() == null || q.getPlannedWorkers() > curr.getPlannedWorkers()))) {
                quotaMap.put(key, q);
            }
        }

        List<Gate> activeGates = gateRepository.findAll().stream()
                .filter(g -> Boolean.TRUE.equals(g.getIsActive()))
                .sorted(Comparator.comparing(Gate::getDisplayOrder, Comparator.nullsLast(Comparator.naturalOrder())))
                .toList();

        List<GatePresetQuotaDto> results = new ArrayList<>();
        for (Gate gate : activeGates) {
            int gatePriority = PRIMARY_GATE_NAMES.contains(gate.getName()) ? 1 : 2;
            for (String role : List.of("Ticket Seller", "Ticket Scanner")) {
                String key = gate.getGateId() + ":" + role;
                if (quotaMap.containsKey(key)) {
                    GatePresetQuota q = quotaMap.get(key);
                    results.add(GatePresetQuotaDto.builder()
                            .presetQuotaId(q.getPresetQuotaId())
                            .presetType(type)
                            .gateId(gate.getGateId())
                            .gateName(gate.getName())
                            .jobRole(role)
                            .plannedWorkers(q.getPlannedWorkers())
                            .minWorkers(q.getMinWorkers())
                            .happyWorkers(q.getHappyWorkers())
                            .priority(q.getPriority())
                            .build());
                } else {
                    results.add(GatePresetQuotaDto.builder()
                            .presetType(type)
                            .gateId(gate.getGateId())
                            .gateName(gate.getName())
                            .jobRole(role)
                            .plannedWorkers(0)
                            .minWorkers(0)
                            .happyWorkers(0)
                            .priority(gatePriority)
                            .build());
                }
            }
        }
        return results;
    }

    @Transactional
    public List<GatePresetQuotaDto> updatePresetQuotas(String presetType, List<GatePresetQuotaDto> dtos) {
        String type = presetType != null ? presetType.trim().toUpperCase() : "BUSY";
        List<GatePresetQuotaDto> results = new ArrayList<>();
        for (GatePresetQuotaDto dto : dtos) {
            if (dto.getGateId() == null || dto.getJobRole() == null) {
                continue;
            }
            Gate gate = gateRepository.findById(dto.getGateId())
                    .orElseThrow(() -> new EntityNotFoundException("Gate not found with id: " + dto.getGateId()));
            String role = normalizeJobRole(dto.getJobRole());
            GatePresetQuota quota = gatePresetQuotaRepository.findByPresetTypeIgnoreCaseAndGate_GateIdAndJobRole(type, dto.getGateId(), role)
                    .orElseGet(() -> {
                        GatePresetQuota newQuota = new GatePresetQuota();
                        newQuota.setPresetType(type);
                        newQuota.setGate(gate);
                        newQuota.setJobRole(role);
                        return newQuota;
                    });

            int planned = dto.getPlannedWorkers() != null ? Math.max(0, dto.getPlannedWorkers()) : 0;
            int min = dto.getMinWorkers() != null ? Math.max(0, dto.getMinWorkers()) : (planned >= 2 ? 2 : planned);
            int happy = dto.getHappyWorkers() != null ? Math.max(0, dto.getHappyWorkers()) : (planned >= 4 ? (int) Math.ceil(planned * 0.7) : planned);
            int priority = dto.getPriority() != null ? Math.max(1, dto.getPriority()) : (PRIMARY_GATE_NAMES.contains(gate.getName()) ? 1 : 2);

            quota.setPlannedWorkers(planned);
            quota.setMinWorkers(min);
            quota.setHappyWorkers(happy);
            quota.setPriority(priority);
            quota = gatePresetQuotaRepository.save(quota);

            results.add(GatePresetQuotaDto.builder()
                    .presetQuotaId(quota.getPresetQuotaId())
                    .presetType(quota.getPresetType())
                    .gateId(gate.getGateId())
                    .gateName(gate.getName())
                    .jobRole(role)
                    .plannedWorkers(planned)
                    .minWorkers(min)
                    .happyWorkers(happy)
                    .priority(priority)
                    .build());
        }
        return results;
    }

    public List<GatePresetQuotaDto> getBaselineTemplate(String mode) {
        String preset = mode != null ? mode.trim().toUpperCase() : "BUSY";
        List<Gate> gates = gateRepository.findAll().stream()
                .filter(g -> Boolean.TRUE.equals(g.getIsActive()))
                .sorted(Comparator.comparing(Gate::getDisplayOrder, Comparator.nullsLast(Comparator.naturalOrder())))
                .toList();

        Map<String, int[]> busyTemplate = Map.of(
                "32", new int[]{10, 2, 6, 1},
                "20", new int[]{10, 2, 6, 1},
                "12", new int[]{6, 2, 4, 2},
                "9", new int[]{5, 2, 3, 2},
                "21", new int[]{5, 2, 3, 2},
                "30", new int[]{2, 1, 2, 3}
        );

        Map<String, int[]> moderateTemplate = Map.of(
                "32", new int[]{6, 2, 4, 1},
                "20", new int[]{6, 2, 4, 1},
                "12", new int[]{4, 2, 3, 2},
                "9", new int[]{3, 2, 2, 2},
                "21", new int[]{3, 2, 2, 2},
                "30", new int[]{2, 1, 2, 3}
        );

        Map<String, int[]> liteTemplate = Map.of(
                "32", new int[]{4, 2, 3, 1},
                "20", new int[]{4, 2, 3, 1},
                "12", new int[]{2, 1, 2, 2},
                "9", new int[]{2, 1, 2, 2},
                "21", new int[]{2, 1, 2, 2},
                "30", new int[]{1, 1, 1, 3}
        );

        Map<String, int[]> selectedTemplate = switch (preset) {
            case "MODERATE" -> moderateTemplate;
            case "LITE" -> liteTemplate;
            default -> busyTemplate;
        };

        List<GatePresetQuotaDto> results = new ArrayList<>();
        for (Gate gate : gates) {
            int[] cfg = selectedTemplate.getOrDefault(gate.getName(), new int[]{2, 1, 2, 2});
            for (String role : List.of("Ticket Seller", "Ticket Scanner")) {
                results.add(GatePresetQuotaDto.builder()
                        .presetType(preset)
                        .gateId(gate.getGateId())
                        .gateName(gate.getName())
                        .jobRole(role)
                        .plannedWorkers(cfg[0])
                        .minWorkers(cfg[1])
                        .happyWorkers(cfg[2])
                        .priority(cfg[3])
                        .build());
            }
        }
        return results;
    }

    private ShiftGateRequirementDto toRequirementDto(ShiftGateRequirement req) {
        return ShiftGateRequirementDto.builder()
                .requirementId(req.getRequirementId())
                .shiftId(req.getShift().getShiftId())
                .gateId(req.getGate().getGateId())
                .gateName(req.getGate().getName())
                .jobRole(normalizeJobRole(req.getJobRole()))
                .workersNeeded(req.getWorkersNeeded())
                .minWorkers(req.getMinWorkers() != null ? req.getMinWorkers() : 0)
                .happyWorkers(req.getHappyWorkers() != null ? req.getHappyWorkers() : 0)
                .priority(req.getPriority() != null ? req.getPriority() : 1)
                .build();
    }

    // ==========================================
    // 2. Worker Listing, Parsing, and Ingress
    // ==========================================

    @Transactional(readOnly = true)
    public List<Person> getWorkers(UUID shiftId) {
        return personRepository.findByShift_ShiftId(shiftId).stream()
                .sorted(Comparator.comparing((Person p) -> p.getGate() != null ? (p.getGate().getDisplayOrder() != null ? p.getGate().getDisplayOrder() : 999) : 9999)
                        .thenComparing((Person p) -> "Ticket Seller".equalsIgnoreCase(normalizeJobRole(p.getJobRole())) ? 0 : 1)
                        .thenComparing(Person::getLastName, Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER))
                        .thenComparing(Person::getFirstName, Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER)))
                .collect(Collectors.toList());
    }

    public List<PersonBulkWorkerDto> parseWorkersFile(MultipartFile file) {
        String filename = file.getOriginalFilename() != null ? file.getOriginalFilename().toLowerCase() : "";
        try {
            if (filename.endsWith(".xlsx") || filename.endsWith(".xls")) {
                return parseExcelWorkers(file.getInputStream());
            } else {
                return parseCsvWorkers(file.getInputStream());
            }
        } catch (Exception e) {
            log.error("Failed to parse workers file", e);
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Failed to parse file: " + e.getMessage());
        }
    }

    private List<PersonBulkWorkerDto> parseCsvWorkers(InputStream inputStream) throws Exception {
        List<PersonBulkWorkerDto> results = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {
            String headerLine = reader.readLine();
            if (headerLine == null) {
                return results;
            }

            String[] headers = parseCsvLine(headerLine);
            int firstIdx = -1, lastIdx = -1, orgIdx = -1, jobIdx = -1, gateIdx = -1, extIdIdx = -1;

            for (int i = 0; i < headers.length; i++) {
                String h = headers[i].trim().toLowerCase();
                if (h.contains("first")) firstIdx = i;
                else if (h.contains("last")) lastIdx = i;
                else if (h.contains("org")) orgIdx = i;
                else if (h.contains("job") || h.contains("role")) jobIdx = i;
                else if (h.contains("gate")) gateIdx = i;
                else if (h.contains("external") || h.equals("ext_id") || h.equals("extid") || h.contains("barcode") || h.equals("qr") || h.equals("qr_code")) extIdIdx = i;
            }

            String line;
            while ((line = reader.readLine()) != null) {
                if (line.isBlank()) continue;
                String[] parts = parseCsvLine(line);

                String first = getPartSafe(parts, firstIdx);
                String last = getPartSafe(parts, lastIdx);
                String org = getPartSafe(parts, orgIdx);
                String job = sanitizeField(getPartSafe(parts, jobIdx));
                String gate = getPartSafe(parts, gateIdx);
                String extId = getPartSafe(parts, extIdIdx);

                if (first.isBlank() && last.isBlank()) continue;

                String normalizedJob = normalizeJobRole(job);
                results.add(PersonBulkWorkerDto.builder()
                        .firstName(sanitizeName(first))
                        .lastName(sanitizeName(last))
                        .organization(sanitizeField(org.isEmpty() ? "Individual / None" : org))
                        .jobRole(normalizedJob != null ? normalizedJob : sanitizeField(job))
                        .gateName(gate.isEmpty() ? null : gate)
                        .externalId(extId.isEmpty() ? null : sanitizeField(extId))
                        .build());
            }
        }
        return results;
    }

    private List<PersonBulkWorkerDto> parseExcelWorkers(InputStream inputStream) throws Exception {
        List<PersonBulkWorkerDto> results = new ArrayList<>();
        try (Workbook workbook = WorkbookFactory.create(inputStream)) {
            Sheet sheet = workbook.getSheetAt(0);
            if (sheet.getPhysicalNumberOfRows() == 0) return results;

            Row headerRow = sheet.getRow(0);
            if (headerRow == null) return results;

            int firstIdx = -1, lastIdx = -1, orgIdx = -1, jobIdx = -1, gateIdx = -1, extIdIdx = -1;
            for (int i = 0; i < headerRow.getLastCellNum(); i++) {
                Cell c = headerRow.getCell(i);
                if (c == null) continue;
                String h = c.getStringCellValue().trim().toLowerCase();
                if (h.contains("first")) firstIdx = i;
                else if (h.contains("last")) lastIdx = i;
                else if (h.contains("org")) orgIdx = i;
                else if (h.contains("job") || h.contains("role")) jobIdx = i;
                else if (h.contains("gate")) gateIdx = i;
                else if (h.contains("external") || h.equals("ext_id") || h.equals("extid") || h.contains("barcode") || h.equals("qr") || h.equals("qr_code")) extIdIdx = i;
            }

            for (int r = 1; r <= sheet.getLastRowNum(); r++) {
                Row row = sheet.getRow(r);
                if (row == null) continue;

                String first = getCellValueSafe(row.getCell(firstIdx));
                String last = getCellValueSafe(row.getCell(lastIdx));
                String org = getCellValueSafe(row.getCell(orgIdx));
                String job = sanitizeField(getCellValueSafe(row.getCell(jobIdx)));
                String gate = getCellValueSafe(row.getCell(gateIdx));
                String extId = getCellValueSafe(row.getCell(extIdIdx));

                if (first.isBlank() && last.isBlank()) continue;

                String normalizedJob = normalizeJobRole(job);
                results.add(PersonBulkWorkerDto.builder()
                        .firstName(sanitizeName(first))
                        .lastName(sanitizeName(last))
                        .organization(sanitizeField(org.isEmpty() ? "Individual / None" : org))
                        .jobRole(normalizedJob != null ? normalizedJob : sanitizeField(job))
                        .gateName(gate.isEmpty() ? null : gate)
                        .externalId(extId.isEmpty() ? null : sanitizeField(extId))
                        .build());
            }
        }
        return results;
    }

    @Transactional
    public List<Person> bulkUploadWorkers(UUID shiftId, List<PersonBulkWorkerDto> workerDtos) {
        Shift shift = shiftRepository.findById(shiftId)
                .orElseThrow(() -> new EntityNotFoundException("Shift not found with id: " + shiftId));

        Map<String, Gate> gateMap = gateRepository.findAll().stream()
                .collect(Collectors.toMap(g -> g.getName().toLowerCase().trim(), g -> g, (a, b) -> a));

        List<Person> savedWorkers = new ArrayList<>();
        String currentUser = "System";

        for (PersonBulkWorkerDto dto : workerDtos) {
            String first = sanitizeName(dto.getFirstName());
            String last = sanitizeName(dto.getLastName());
            if (first.isBlank() && last.isBlank()) continue;

            Person person = personRepository
                    .findFirstByFirstNameIgnoreCaseAndLastNameIgnoreCaseAndShift_ShiftId(first, last, shiftId)
                    .orElseGet(Person::new);

            String normalizedRole = normalizeJobRole(dto.getJobRole());
            if (!"Ticket Seller".equals(normalizedRole) && !"Ticket Scanner".equals(normalizedRole)) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Invalid job role '" + dto.getJobRole() + "' for volunteer " + first + " " + last +
                        ". Role must be either 'Ticket Seller' or 'Ticket Scanner'.");
            }

            person.setFirstName(first);
            person.setLastName(last.isBlank() ? null : last);
            person.setShift(shift);
            person.setOrganization(dto.getOrganization() != null && !dto.getOrganization().isBlank()
                    ? sanitizeField(dto.getOrganization().trim())
                    : "Individual / None");
            person.setJobRole(normalizedRole);
            person.setIsActive(true);
            person.setNotes(dto.getNotes() != null ? sanitizeField(dto.getNotes()) : null);
            person.setExternalId(dto.getExternalId() != null ? sanitizeField(dto.getExternalId()) : null);

            // Match gate if provided
            if (dto.getGateId() != null) {
                gateRepository.findById(dto.getGateId()).ifPresent(person::setGate);
            } else if (dto.getGateName() != null && !dto.getGateName().isBlank()) {
                Gate matched = gateMap.get(dto.getGateName().toLowerCase().trim());
                if (matched != null) {
                    person.setGate(matched);
                }
            }

            if (person.getCreatedBy() == null) {
                person.setCreatedBy(currentUser);
            }
            person.setUpdatedBy(currentUser);

            savedWorkers.add(personRepository.save(person));
        }

        return savedWorkers;
    }

    @Transactional
    public Person patchWorker(UUID personId, PersonPatchDto patchDto) {
        return personService.updatePerson(personId, patchDto);
    }

    // ==========================================
    // 3. Organization Gate Preferences
    // ==========================================

    @Transactional(readOnly = true)
    public List<OrgGatePreferenceDto> getPreferences(UUID shiftId) {
        return preferenceRepository.findByShift_ShiftId(shiftId).stream()
                .map(p -> OrgGatePreferenceDto.builder()
                        .preferenceId(p.getPreferenceId())
                        .shiftId(p.getShift().getShiftId())
                        .gateId(p.getGate().getGateId())
                        .gateName(p.getGate().getName())
                        .organization(p.getOrganization())
                        .priority(p.getPriority())
                        .notes(p.getNotes())
                        .build())
                .collect(Collectors.toList());
    }

    @Transactional
    public List<OrgGatePreferenceDto> updatePreferences(UUID shiftId, List<OrgGatePreferenceDto> dtos) {
        Shift shift = shiftRepository.findById(shiftId)
                .orElseThrow(() -> new EntityNotFoundException("Shift not found with id: " + shiftId));

        preferenceRepository.deleteByShift_ShiftId(shiftId);
        preferenceRepository.flush();

        List<OrgGatePreferenceDto> results = new ArrayList<>();
        if (dtos == null) {
            return results;
        }

        for (OrgGatePreferenceDto dto : dtos) {
            if (dto.getGateId() == null || dto.getOrganization() == null || dto.getOrganization().isBlank()) {
                continue;
            }
            Gate gate = gateRepository.findById(dto.getGateId())
                    .orElseThrow(() -> new EntityNotFoundException("Gate not found with id: " + dto.getGateId()));

            OrgGatePreference pref = new OrgGatePreference();
            pref.setShift(shift);
            pref.setGate(gate);
            pref.setOrganization(dto.getOrganization().trim());
            pref.setPriority(dto.getPriority() != null ? dto.getPriority() : 1);
            pref.setNotes(dto.getNotes() != null ? sanitizeField(dto.getNotes()) : null);
            pref.setCreatedBy("System");
            pref.setUpdatedBy("System");
            pref = preferenceRepository.save(pref);

            results.add(OrgGatePreferenceDto.builder()
                    .preferenceId(pref.getPreferenceId())
                    .shiftId(pref.getShift().getShiftId())
                    .gateId(pref.getGate().getGateId())
                    .gateName(pref.getGate().getName())
                    .organization(pref.getOrganization())
                    .priority(pref.getPriority())
                    .notes(pref.getNotes())
                    .build());
        }
        return results;
    }

    // ==========================================
    // 4. Smart Gate Assignment Algorithm
    // ==========================================

    @Transactional
    public List<Person> assignGates(UUID shiftId) {
        Shift shift = shiftRepository.findById(shiftId)
                .orElseThrow(() -> new EntityNotFoundException("Shift not found with id: " + shiftId));

        List<Person> activeWorkers = personRepository.findByShift_ShiftIdAndIsActiveTrue(shiftId);
        if (activeWorkers.isEmpty()) {
            return activeWorkers;
        }

        List<Gate> activeGates = gateRepository.findAll().stream()
                .filter(g -> Boolean.TRUE.equals(g.getIsActive()))
                .sorted(Comparator.comparing(Gate::getDisplayOrder, Comparator.nullsLast(Comparator.naturalOrder())))
                .toList();

        if (activeGates.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "No active gates configured in the system.");
        }

        List<ShiftGateRequirement> requirements = requirementRepository.findByShift_ShiftId(shiftId);
        List<OrgGatePreference> preferences = preferenceRepository.findByShift_ShiftId(shiftId);

        // Reset previous gate assignments on active workers to support clean re-allocation
        for (Person p : activeWorkers) {
            p.setGate(null);
        }

        // Build gate quota trackers
        Map<UUID, GateQuota> quotaMap = new LinkedHashMap<>();
        for (Gate g : activeGates) {
            GateQuota q = new GateQuota();
            q.gate = g;
            int defaultPriority = PRIMARY_GATE_NAMES.contains(g.getName()) ? 1 : 2;
            q.priority = defaultPriority;
            q.sellersPriority = defaultPriority;
            q.scannersPriority = defaultPriority;
            quotaMap.put(g.getGateId(), q);
        }

        // Deduplicate and filter requirements by gate and normalized role, keeping the highest planned workers
        Map<String, ShiftGateRequirement> effectiveReqs = new LinkedHashMap<>();
        for (ShiftGateRequirement req : requirements) {
            if (req.getGate() == null || req.getGate().getGateId() == null) continue;
            String role = normalizeJobRole(req.getJobRole());
            if (!"Ticket Seller".equalsIgnoreCase(role) && !"Ticket Scanner".equalsIgnoreCase(role)) {
                continue; // Ignore auxiliary roles (e.g., Flex / Lead)
            }
            String key = req.getGate().getGateId() + ":" + role;
            ShiftGateRequirement existing = effectiveReqs.get(key);
            if (existing == null || (req.getWorkersNeeded() != null && (existing.getWorkersNeeded() == null || req.getWorkersNeeded() > existing.getWorkersNeeded()))) {
                effectiveReqs.put(key, req);
            }
        }

        for (ShiftGateRequirement req : effectiveReqs.values()) {
            GateQuota q = quotaMap.get(req.getGate().getGateId());
            if (q != null) {
                String role = normalizeJobRole(req.getJobRole());
                int priority = req.getPriority() != null ? req.getPriority() : (PRIMARY_GATE_NAMES.contains(q.gate.getName()) ? 1 : 2);
                int planned = req.getWorkersNeeded() != null ? req.getWorkersNeeded() : 0;
                int min = req.getMinWorkers() != null ? req.getMinWorkers() : (planned >= 2 ? 2 : planned);
                int happy = req.getHappyWorkers() != null ? req.getHappyWorkers() : (planned >= 4 ? (int) Math.ceil(planned * 0.7) : planned);
                if ("Ticket Seller".equalsIgnoreCase(role)) {
                    q.sellersPlanned = planned;
                    q.sellersMin = min;
                    q.sellersHappy = happy;
                    q.sellersPriority = priority;
                } else if ("Ticket Scanner".equalsIgnoreCase(role)) {
                    q.scannersPlanned = planned;
                    q.scannersMin = min;
                    q.scannersHappy = happy;
                    q.scannersPriority = priority;
                }
                q.priority = Math.min(q.sellersPriority, q.scannersPriority);
            }
        }

        // 1. Explicit Org Gate Preferences: only include orgs with a valid saved gate
        Map<String, UUID> orgGatePrefMap = preferences.stream()
                .filter(pref -> pref.getGate() != null && pref.getOrganization() != null && !pref.getOrganization().isBlank())
                .sorted(Comparator.comparing(OrgGatePreference::getPriority, Comparator.nullsLast(Comparator.naturalOrder())))
                .collect(Collectors.toMap(
                        p -> p.getOrganization().toLowerCase().trim(),
                        p -> p.getGate().getGateId(),
                        (existing, replacement) -> existing
                ));

        List<Person> unassignedWorkers = new ArrayList<>(activeWorkers);

        // Step 1: Assign preferred org members to their preferred gate up to Planned capacity
        for (Map.Entry<String, UUID> prefEntry : orgGatePrefMap.entrySet()) {
            GateQuota prefGate = quotaMap.get(prefEntry.getValue());
            if (prefGate != null) {
                List<Person> orgMembers = unassignedWorkers.stream()
                        .filter(p -> p.getOrganization() != null && p.getOrganization().toLowerCase().trim().equals(prefEntry.getKey()))
                        .toList();
                for (Person p : orgMembers) {
                    String role = normalizeJobRole(p.getJobRole());
                    if (role != null && prefGate.hasRoleCapacity(role, "PLANNED") && assignPersonToGate(p, prefGate, role, "PLANNED")) {
                        unassignedWorkers.remove(p);
                    }
                }
            }
        }

        // Step 2: Companion groups (size >= 2) without explicit preferences stay together within capacity
        Map<String, List<Person>> companionOrgs = unassignedWorkers.stream()
                .filter(p -> p.getOrganization() != null && !p.getOrganization().isBlank() && !p.getOrganization().equalsIgnoreCase("Individual / None"))
                .collect(Collectors.groupingBy(p -> p.getOrganization().trim(), LinkedHashMap::new, Collectors.toList()));

        for (Map.Entry<String, List<Person>> entry : companionOrgs.entrySet()) {
            List<Person> group = entry.getValue();
            if (group.size() >= 2) {
                if (orgGatePrefMap.containsKey(entry.getKey().toLowerCase().trim())) {
                    continue; // Already handled in Step 1
                }

                List<Person> sellers = group.stream()
                        .filter(p -> "Ticket Seller".equalsIgnoreCase(normalizeJobRole(p.getJobRole())))
                        .toList();
                List<Person> scanners = group.stream()
                        .filter(p -> "Ticket Scanner".equalsIgnoreCase(normalizeJobRole(p.getJobRole())))
                        .toList();

                if (!sellers.isEmpty()) {
                    assignCompanionRolePods(sellers, "Ticket Seller", quotaMap, unassignedWorkers);
                }
                if (!scanners.isEmpty()) {
                    assignCompanionRolePods(scanners, "Ticket Scanner", quotaMap, unassignedWorkers);
                }
            }
        }

        // ============================================================
        // LEVEL 1: Minimum Floor Defense
        // ============================================================
        allocateFloorDefense("Ticket Seller", unassignedWorkers, quotaMap, orgGatePrefMap);
        allocateFloorDefense("Ticket Scanner", unassignedWorkers, quotaMap, orgGatePrefMap);

        // ============================================================
        // LEVEL 2: Comfort Level (Happy Allocation)
        // ============================================================
        allocateLevelForRole("Ticket Seller", "HAPPY", unassignedWorkers, quotaMap, orgGatePrefMap);
        allocateLevelForRole("Ticket Scanner", "HAPPY", unassignedWorkers, quotaMap, orgGatePrefMap);

        // ============================================================
        // LEVEL 3: Planned Target Allocation
        // ============================================================
        allocateLevelForRole("Ticket Seller", "PLANNED", unassignedWorkers, quotaMap, orgGatePrefMap);
        allocateLevelForRole("Ticket Scanner", "PLANNED", unassignedWorkers, quotaMap, orgGatePrefMap);

        // ============================================================
        // LEVEL 4: Total Planned Headcount Absorption
        // If workers remain and planned allocation is not met, no unassigned resources
        // ============================================================
        allocateAbsorptionLevel(unassignedWorkers, quotaMap, orgGatePrefMap);

        personRepository.saveAll(activeWorkers);
        return activeWorkers;
    }

    private void allocateFloorDefense(String role, List<Person> pool, Map<UUID, GateQuota> quotaMap, Map<String, UUID> orgGatePrefMap) {
        if (pool.isEmpty()) return;

        List<GateQuota> sortedGates = quotaMap.values().stream()
                .filter(q -> q.getRoleTarget(role, "MIN") > 0)
                .sorted(Comparator.comparingInt((GateQuota q) -> q.getRolePriority(role))
                        .thenComparing(q -> q.gate.getDisplayOrder() != null ? q.gate.getDisplayOrder() : 999))
                .toList();

        // Pass 1: Give 1 worker to each gate with demand in priority order
        for (GateQuota q : sortedGates) {
            if (q.getAssigned(role) == 0 && q.hasRoleCapacity(role, "MIN") && !pool.isEmpty()) {
                Person p = findBestWorkerForGate(pool, q, role, "MIN", orgGatePrefMap);
                if (p != null && assignPersonToGate(p, q, role, "MIN")) {
                    pool.remove(p);
                }
            }
        }

        // Pass 2: For gates where min >= 2, allocate 2nd worker to establish buddy pairs in priority order
        for (GateQuota q : sortedGates) {
            if (q.getRoleTarget(role, "MIN") >= 2 && q.getAssigned(role) < 2 && q.hasRoleCapacity(role, "MIN") && !pool.isEmpty()) {
                Person p = findBestWorkerForGate(pool, q, role, "MIN", orgGatePrefMap);
                if (p != null && assignPersonToGate(p, q, role, "MIN")) {
                    pool.remove(p);
                }
            }
        }

        // Pass 3: Continue round-robin in priority order for gates still needing minimum
        boolean allocatedInRound = true;
        while (allocatedInRound && !pool.isEmpty()) {
            allocatedInRound = false;
            for (GateQuota q : sortedGates) {
                if (q.getRemainingRoleTarget(role, "MIN") > 0 && !pool.isEmpty()) {
                    Person p = findBestWorkerForGate(pool, q, role, "MIN", orgGatePrefMap);
                    if (p != null && assignPersonToGate(p, q, role, "MIN")) {
                        pool.remove(p);
                        allocatedInRound = true;
                    }
                }
            }
        }
    }

    private void assignCompanionRolePods(List<Person> members, String role, Map<UUID, GateQuota> quotaMap, List<Person> unassignedWorkers) {
        if (members.isEmpty()) return;

        long totalRolePool = unassignedWorkers.stream().filter(p -> role.equalsIgnoreCase(normalizeJobRole(p.getJobRole()))).count();
        String orgName = members.get(0).getOrganization().toLowerCase().trim();

        List<GateQuota> candidateGates = quotaMap.values().stream()
                .filter(q -> q.getRemainingRoleTarget(role, "PLANNED") > 0)
                .sorted(Comparator.comparing((GateQuota q) -> q.assignedOrganizations.contains(orgName) ? 0 : 1)
                        .thenComparingInt(q -> q.getRolePriority(role))
                        .thenComparing(q -> q.gate.getDisplayOrder() != null ? q.gate.getDisplayOrder() : 999))
                .toList();

        if (candidateGates.isEmpty()) return;

        GateQuota primaryGate = candidateGates.get(0);
        long otherGatesMin = quotaMap.values().stream()
                .filter(g -> g != primaryGate)
                .mapToInt(g -> g.getRemainingRoleTarget(role, "MIN"))
                .sum();

        // Only pre-assign companion group if remaining pool satisfies minimum floor for all other gates
        if (totalRolePool - members.size() < otherGatesMin) {
            return;
        }

        int cap = primaryGate.getRemainingRoleTarget(role, "PLANNED");
        List<List<Person>> pods = partitionIntoPods(members, cap);

        List<Person> pod1 = pods.get(0);
        for (Person p : pod1) {
            if (assignPersonToGate(p, primaryGate, role, "PLANNED")) {
                unassignedWorkers.remove(p);
            }
        }

        if (pods.size() > 1) {
            List<Person> pod2 = pods.get(1);
            List<GateQuota> secondaryCandidates = candidateGates.stream()
                    .filter(g -> g != primaryGate && g.getRemainingRoleTarget(role, "PLANNED") >= pod2.size())
                    .toList();
            if (!secondaryCandidates.isEmpty()) {
                GateQuota secondaryGate = secondaryCandidates.get(0);
                for (Person p : pod2) {
                    if (assignPersonToGate(p, secondaryGate, role, "PLANNED")) {
                        unassignedWorkers.remove(p);
                    }
                }
            }
        }
    }

    private List<List<Person>> partitionIntoPods(List<Person> members, int targetCapacity) {
        int n = members.size();
        if (n <= targetCapacity) {
            return List.of(members);
        }
        int remainder = n - targetCapacity;
        if (remainder == 1 && targetCapacity > 2) {
            // Balanced split to prevent solitary stranding
            int pod1Size = (int) Math.ceil(n / 2.0);
            return List.of(members.subList(0, pod1Size), members.subList(pod1Size, n));
        } else {
            return List.of(members.subList(0, targetCapacity), members.subList(targetCapacity, n));
        }
    }

    private void allocateLevelForRole(String role, String level, List<Person> pool, Map<UUID, GateQuota> quotaMap, Map<String, UUID> orgGatePrefMap) {
        if (pool.isEmpty()) return;

        List<GateQuota> eligibleGates = quotaMap.values().stream()
                .filter(q -> q.getRemainingRoleTarget(role, level) > 0)
                .toList();
        if (eligibleGates.isEmpty()) return;

        // Group by role priority ascending
        Map<Integer, List<GateQuota>> priorityTiers = eligibleGates.stream()
                .collect(Collectors.groupingBy(q -> q.getRolePriority(role), TreeMap::new, Collectors.toList()));

        for (Map.Entry<Integer, List<GateQuota>> tierEntry : priorityTiers.entrySet()) {
            List<GateQuota> tierGates = tierEntry.getValue();
            tierGates.sort(Comparator.comparing(q -> q.gate.getDisplayOrder() != null ? q.gate.getDisplayOrder() : 999));

            boolean allocatedInRound = true;
            while (allocatedInRound && !pool.isEmpty()) {
                allocatedInRound = false;
                for (GateQuota q : tierGates) {
                    if (q.getRemainingRoleTarget(role, level) > 0 && !pool.isEmpty()) {
                        Person p = findBestWorkerForGate(pool, q, role, level, orgGatePrefMap);
                        if (p != null && assignPersonToGate(p, q, role, level)) {
                            pool.remove(p);
                            allocatedInRound = true;
                        }
                    }
                }
            }
        }
    }

    private void allocateAbsorptionLevel(List<Person> pool, Map<UUID, GateQuota> quotaMap, Map<String, UUID> orgGatePrefMap) {
        if (pool.isEmpty()) return;

        List<GateQuota> eligibleGates = quotaMap.values().stream()
                .filter(q -> q.getTotalAssigned() < q.getPlannedTotal())
                .toList();
        if (eligibleGates.isEmpty()) return;

        // Group by gate priority ascending (using minimum priority between roles)
        Map<Integer, List<GateQuota>> priorityTiers = eligibleGates.stream()
                .collect(Collectors.groupingBy(q -> Math.min(q.sellersPriority, q.scannersPriority), TreeMap::new, Collectors.toList()));

        for (Map.Entry<Integer, List<GateQuota>> tierEntry : priorityTiers.entrySet()) {
            List<GateQuota> tierGates = tierEntry.getValue();
            tierGates.sort(Comparator.comparing(q -> q.gate.getDisplayOrder() != null ? q.gate.getDisplayOrder() : 999));

            boolean allocatedInRound = true;
            while (allocatedInRound && !pool.isEmpty()) {
                allocatedInRound = false;
                for (GateQuota q : tierGates) {
                    if (q.getTotalAssigned() < q.getPlannedTotal() && !pool.isEmpty()) {
                        Person p = findBestAbsorptionWorkerForGate(pool, q, orgGatePrefMap);
                        if (p != null) {
                            p.setGate(q.gate);
                            String role = normalizeJobRole(p.getJobRole());
                            if ("Ticket Seller".equalsIgnoreCase(role)) {
                                q.sellersAssigned++;
                            } else {
                                q.scannersAssigned++;
                            }
                            q.recordPerson(p);
                            pool.remove(p);
                            allocatedInRound = true;
                        }
                    }
                }
            }
        }
    }

    private Person findBestAbsorptionWorkerForGate(List<Person> pool, GateQuota q, Map<String, UUID> orgGatePrefMap) {
        if (pool.isEmpty()) return null;
        UUID targetGateId = q.gate.getGateId();

        // Priority 1: Preferred org match
        for (Person p : pool) {
            if (p.getOrganization() != null && !p.getOrganization().isBlank()) {
                UUID prefGate = orgGatePrefMap.get(p.getOrganization().toLowerCase().trim());
                if (targetGateId.equals(prefGate)) {
                    return p;
                }
            }
        }

        // Priority 2: Org Affinity (org already stationed at this gate)
        for (Person p : pool) {
            if (p.getOrganization() != null && !p.getOrganization().isBlank() && !p.getOrganization().equalsIgnoreCase("Individual / None")) {
                if (q.assignedOrganizations.contains(p.getOrganization().toLowerCase().trim())) {
                    return p;
                }
            }
        }

        // Priority 3: First available worker in pool
        return pool.get(0);
    }

    private Person findBestWorkerForGate(List<Person> pool, GateQuota q, String targetRole, String phase, Map<String, UUID> orgGatePrefMap) {
        if (pool.isEmpty()) return null;

        if (targetRole != null) {
            if (!q.hasRoleCapacity(targetRole, phase)) {
                return null;
            }
        } else {
            boolean needsSeller = q.hasRoleCapacity("Ticket Seller", phase);
            boolean needsScanner = q.hasRoleCapacity("Ticket Scanner", phase);
            if (!needsSeller && !needsScanner) {
                return null;
            }
        }

        boolean needsSeller = targetRole != null ? "Ticket Seller".equalsIgnoreCase(targetRole) : q.hasRoleCapacity("Ticket Seller", phase);
        boolean needsScanner = targetRole != null ? "Ticket Scanner".equalsIgnoreCase(targetRole) : q.hasRoleCapacity("Ticket Scanner", phase);

        UUID targetGateId = q.gate.getGateId();

        // Priority 1: Preferred organization match that directly matches the open role
        for (Person p : pool) {
            if (p.getOrganization() != null && !p.getOrganization().isBlank()) {
                UUID prefGate = orgGatePrefMap.get(p.getOrganization().toLowerCase().trim());
                if (targetGateId.equals(prefGate)) {
                    String role = normalizeJobRole(p.getJobRole());
                    boolean isSeller = "Ticket Seller".equalsIgnoreCase(role);
                    boolean isScanner = "Ticket Scanner".equalsIgnoreCase(role);
                    if ((isSeller && needsSeller) || (isScanner && needsScanner)) {
                        return p;
                    }
                }
            }
        }

        // Priority 2 (Org Affinity): Worker whose organization is already stationed at this gate AND matches role
        for (Person p : pool) {
            if (p.getOrganization() != null && !p.getOrganization().isBlank() && !p.getOrganization().equalsIgnoreCase("Individual / None")) {
                if (q.assignedOrganizations.contains(p.getOrganization().toLowerCase().trim())) {
                    String role = normalizeJobRole(p.getJobRole());
                    boolean isSeller = "Ticket Seller".equalsIgnoreCase(role);
                    boolean isScanner = "Ticket Scanner".equalsIgnoreCase(role);
                    if ((isSeller && needsSeller) || (isScanner && needsScanner)) {
                        return p;
                    }
                }
            }
        }

        // Priority 3: Non-preferred worker who directly matches the open role
        for (Person p : pool) {
            String role = normalizeJobRole(p.getJobRole());
            boolean isSeller = "Ticket Seller".equalsIgnoreCase(role);
            boolean isScanner = "Ticket Scanner".equalsIgnoreCase(role);
            if ((isSeller && needsSeller) || (isScanner && needsScanner)) {
                return p;
            }
        }

        return null;
    }

    private Person findBestWorkerForGate(List<Person> pool, GateQuota q, String phase, Map<String, UUID> orgGatePrefMap) {
        return findBestWorkerForGate(pool, q, null, phase, orgGatePrefMap);
    }

    private boolean assignPersonToGate(Person p, GateQuota q, String targetRole, String phase) {
        String workerRole = normalizeJobRole(p.getJobRole());
        if (!"Ticket Seller".equalsIgnoreCase(workerRole) && !"Ticket Scanner".equalsIgnoreCase(workerRole)) {
            return false;
        }

        String role = targetRole != null ? targetRole : workerRole;
        if (!role.equalsIgnoreCase(workerRole)) {
            return false; // Workers can never be flexed to a different role
        }

        if (!q.hasRoleCapacity(role, phase)) {
            return false;
        }

        p.setGate(q.gate);
        p.setJobRole(role);
        if ("Ticket Seller".equalsIgnoreCase(role)) {
            q.sellersAssigned++;
        } else {
            q.scannersAssigned++;
        }
        q.recordPerson(p);
        return true;
    }

    private boolean assignPersonToGate(Person p, GateQuota q, String phase) {
        return assignPersonToGate(p, q, null, phase);
    }

    private static class GateQuota {
        Gate gate;
        int priority = 1;
        int sellersPriority = 2;
        int scannersPriority = 2;

        int sellersMin = 0;
        int sellersHappy = 0;
        int sellersPlanned = 0;
        int sellersAssigned = 0;

        int scannersMin = 0;
        int scannersHappy = 0;
        int scannersPlanned = 0;
        int scannersAssigned = 0;

        Set<String> assignedOrganizations = new LinkedHashSet<>();

        void recordPerson(Person p) {
            if (p.getOrganization() != null && !p.getOrganization().isBlank() && !p.getOrganization().equalsIgnoreCase("Individual / None")) {
                assignedOrganizations.add(p.getOrganization().toLowerCase().trim());
            }
        }

        int getRolePriority(String role) {
            return "Ticket Seller".equalsIgnoreCase(role) ? sellersPriority : scannersPriority;
        }

        int getAssigned(String role) {
            return "Ticket Seller".equalsIgnoreCase(role) ? sellersAssigned : scannersAssigned;
        }

        int getTotalAssigned() {
            return sellersAssigned + scannersAssigned;
        }

        int getMinTotal() {
            return sellersMin + scannersMin;
        }

        int getHappyTotal() {
            return sellersHappy + scannersHappy;
        }

        int getPlannedTotal() {
            return sellersPlanned + scannersPlanned;
        }

        int getRemainingMinTotal() {
            return Math.max(0, sellersMin - sellersAssigned) + Math.max(0, scannersMin - scannersAssigned);
        }

        int getRemainingHappyTotal() {
            return Math.max(0, sellersHappy - sellersAssigned) + Math.max(0, scannersHappy - scannersAssigned);
        }

        int getRemainingPlannedTotal() {
            return Math.max(0, sellersPlanned - sellersAssigned) + Math.max(0, scannersPlanned - scannersAssigned);
        }

        int getRemainingPlannedSellers() {
            return Math.max(0, sellersPlanned - sellersAssigned);
        }

        int getRemainingPlannedScanners() {
            return Math.max(0, scannersPlanned - scannersAssigned);
        }

        int getRemainingHappySellers() {
            return Math.max(0, sellersHappy - sellersAssigned);
        }

        int getRemainingHappyScanners() {
            return Math.max(0, scannersHappy - scannersAssigned);
        }

        int getRemainingMinSellers() {
            return Math.max(0, sellersMin - sellersAssigned);
        }

        int getRemainingMinScanners() {
            return Math.max(0, scannersMin - scannersAssigned);
        }

        int getRoleTarget(String role, String level) {
            boolean isSeller = "Ticket Seller".equalsIgnoreCase(role);
            if ("MIN".equalsIgnoreCase(level)) {
                return isSeller ? sellersMin : scannersMin;
            } else if ("HAPPY".equalsIgnoreCase(level)) {
                return isSeller ? sellersHappy : scannersHappy;
            } else {
                return isSeller ? sellersPlanned : scannersPlanned;
            }
        }

        int getRemainingRoleTarget(String role, String level) {
            boolean isSeller = "Ticket Seller".equalsIgnoreCase(role);
            int assigned = isSeller ? sellersAssigned : scannersAssigned;
            return Math.max(0, getRoleTarget(role, level) - assigned);
        }

        int getRemainingCapacity(String phase) {
            if ("MIN".equalsIgnoreCase(phase)) return getRemainingMinTotal();
            if ("HAPPY".equalsIgnoreCase(phase)) return getRemainingHappyTotal();
            return getRemainingPlannedTotal();
        }

        boolean hasRoleCapacity(String role, String phase) {
            return getRemainingRoleTarget(role, phase) > 0;
        }
    }

    // ==========================================
    // 5. Seller Sticker Management (Unidirectional)
    // ==========================================

    @Transactional
    public PersonStickerDto createOrGetSellerSticker(UUID shiftId, UUID personId, UUID gateId) {
        Person person = personRepository.findById(personId)
                .orElseThrow(() -> new EntityNotFoundException("Person not found with id: " + personId));
        Shift shift = shiftRepository.findById(shiftId)
                .orElseThrow(() -> new EntityNotFoundException("Shift not found with id: " + shiftId));

        String normalizedRole = normalizeJobRole(person.getJobRole());
        if (!"Ticket Seller".equalsIgnoreCase(normalizedRole)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Only Ticket Sellers can have QR code stickers generated. Ticket Scanners do not receive QR codes.");
        }

        Gate gate = null;
        if (gateId != null) {
            gate = gateRepository.findById(gateId)
                    .orElseThrow(() -> new EntityNotFoundException("Gate not found with id: " + gateId));
        } else if (person.getGate() != null) {
            gate = person.getGate();
        }

        PersonSticker existing = personStickerRepository.findFirstByPerson_PersonIdAndShift_ShiftId(personId, shiftId);
        if (existing != null) {
            if (gate != null && (existing.getGate() == null || !gate.getGateId().equals(existing.getGate().getGateId()))) {
                existing.setGate(gate);
                existing.setUpdatedBy("System");
                existing = personStickerRepository.save(existing);
            }
            return toPersonStickerDto(existing);
        }

        PersonSticker sticker = new PersonSticker();
        sticker.setPerson(person);
        sticker.setShift(shift);
        sticker.setGate(gate);
        sticker.setCreatedBy("System");
        sticker = personStickerRepository.save(sticker);
        return toPersonStickerDto(sticker);
    }

    @Transactional(readOnly = true)
    public List<PersonStickerDto> getSessionStickers(UUID shiftId) {
        shiftRepository.findById(shiftId)
                .orElseThrow(() -> new EntityNotFoundException("Shift not found with id: " + shiftId));
        return personStickerRepository.findByShift_ShiftId(shiftId).stream()
                .map(this::toPersonStickerDto)
                .collect(Collectors.toList());
    }

    @Transactional
    public List<PersonStickerDto> generateMissingSellerStickers(UUID shiftId) {
        shiftRepository.findById(shiftId)
                .orElseThrow(() -> new EntityNotFoundException("Shift not found with id: " + shiftId));

        List<Person> sellers = personRepository.findByShift_ShiftIdAndIsActiveTrue(shiftId).stream()
                .filter(p -> "Ticket Seller".equalsIgnoreCase(normalizeJobRole(p.getJobRole())))
                .toList();

        List<PersonStickerDto> result = new ArrayList<>();
        for (Person seller : sellers) {
            result.add(createOrGetSellerSticker(shiftId, seller.getPersonId(), seller.getGate() != null ? seller.getGate().getGateId() : null));
        }
        return result;
    }

    private PersonStickerDto toPersonStickerDto(PersonSticker s) {
        return PersonStickerDto.builder()
                .personStickerId(s.getPersonStickerId())
                .personId(s.getPerson() != null ? s.getPerson().getPersonId() : null)
                .shiftId(s.getShift() != null ? s.getShift().getShiftId() : null)
                .gateId(s.getGate() != null ? s.getGate().getGateId() : null)
                .externalId(s.getPerson() != null ? s.getPerson().getExternalId() : null)
                .firstName(s.getPerson() != null ? s.getPerson().getFirstName() : null)
                .lastName(s.getPerson() != null ? s.getPerson().getLastName() : null)
                .build();
    }

    // ==========================================
    // 5. Gate Assignment Summary
    // ==========================================

    @Transactional(readOnly = true)
    public GateAssignmentSummaryDto getSummary(UUID shiftId) {
        List<Person> workers = personRepository.findByShift_ShiftIdAndIsActiveTrue(shiftId);
        List<ShiftGateRequirement> reqs = requirementRepository.findByShift_ShiftId(shiftId);

        List<Gate> activeGates = gateRepository.findAll().stream()
                .filter(g -> Boolean.TRUE.equals(g.getIsActive()))
                .sorted(Comparator.comparing(Gate::getDisplayOrder, Comparator.nullsLast(Comparator.naturalOrder())))
                .toList();

        // Map requirements per gate
        Map<UUID, Map<String, Integer>> reqMap = new HashMap<>();
        Map<UUID, Integer> gatePriorityMap = new HashMap<>();
        for (ShiftGateRequirement r : reqs) {
            if (r.getGate() == null || r.getGate().getGateId() == null) continue;
            String role = normalizeJobRole(r.getJobRole());
            if (!"Ticket Seller".equalsIgnoreCase(role) && !"Ticket Scanner".equalsIgnoreCase(role)) {
                continue;
            }
            int planned = r.getWorkersNeeded() != null ? r.getWorkersNeeded() : 0;
            int min = r.getMinWorkers() != null ? r.getMinWorkers() : 0;
            int happy = r.getHappyWorkers() != null ? r.getHappyWorkers() : 0;

            Map<String, Integer> gateData = reqMap.computeIfAbsent(r.getGate().getGateId(), k -> new HashMap<>());
            String plannedKey = role + "_planned";
            if (!gateData.containsKey(plannedKey) || planned > gateData.getOrDefault(plannedKey, 0)) {
                gateData.put(plannedKey, planned);
                gateData.put(role + "_min", min);
                gateData.put(role + "_happy", happy);
            }
            if (r.getPriority() != null) {
                gatePriorityMap.put(r.getGate().getGateId(), r.getPriority());
            }
        }

        // Map actual assignments per gate and overall pools
        Map<UUID, Map<String, Integer>> assignedMap = new HashMap<>();
        int totalAssigned = 0;
        int totalSellers = 0;
        int sellersAssigned = 0;
        int totalScanners = 0;
        int scannersAssigned = 0;

        for (Person p : workers) {
            boolean isSeller = "Ticket Seller".equalsIgnoreCase(normalizeJobRole(p.getJobRole()));
            if (isSeller) {
                totalSellers++;
            } else {
                totalScanners++;
            }

            if (p.getGate() != null) {
                totalAssigned++;
                if (isSeller) sellersAssigned++;
                else scannersAssigned++;

                assignedMap.computeIfAbsent(p.getGate().getGateId(), k -> new HashMap<>())
                        .merge(normalizeJobRole(p.getJobRole()), 1, Integer::sum);
            }
        }

        int totalNeeded = 0;
        int totalSellersNeeded = 0;
        int totalScannersNeeded = 0;
        List<GateAssignmentSummaryDto.GateBreakdownDto> breakdowns = new ArrayList<>();

        for (Gate gate : activeGates) {
            Map<String, Integer> gateReq = reqMap.getOrDefault(gate.getGateId(), Collections.emptyMap());
            Map<String, Integer> gateAssigned = assignedMap.getOrDefault(gate.getGateId(), Collections.emptyMap());

            int sellersPlanned = gateReq.getOrDefault("Ticket Seller_planned", 0);
            int sellersMin = gateReq.getOrDefault("Ticket Seller_min", 0);
            int sellersHappy = gateReq.getOrDefault("Ticket Seller_happy", 0);
            int sellersAtGate = gateAssigned.getOrDefault("Ticket Seller", 0);

            int scannersPlanned = gateReq.getOrDefault("Ticket Scanner_planned", 0);
            int scannersMin = gateReq.getOrDefault("Ticket Scanner_min", 0);
            int scannersHappy = gateReq.getOrDefault("Ticket Scanner_happy", 0);
            int scannersAtGate = gateAssigned.getOrDefault("Ticket Scanner", 0);

            int gateTotalNeeded = sellersPlanned + scannersPlanned;
            int gateTotalAssigned = sellersAtGate + scannersAtGate;
            totalNeeded += gateTotalNeeded;
            totalSellersNeeded += sellersPlanned;
            totalScannersNeeded += scannersPlanned;

            String status = "STAFFED";
            if (gateTotalAssigned < gateTotalNeeded) {
                status = "SHORT";
            } else if (gateTotalAssigned > gateTotalNeeded) {
                status = "SURPLUS";
            }

            // Milestone status
            String milestoneStatus = "BELOW_MIN";
            boolean sellersMetPlanned = sellersPlanned == 0 || sellersAtGate >= sellersPlanned;
            boolean scannersMetPlanned = scannersPlanned == 0 || scannersAtGate >= scannersPlanned;
            boolean sellersMetHappy = sellersHappy == 0 || sellersAtGate >= sellersHappy;
            boolean scannersMetHappy = scannersHappy == 0 || scannersAtGate >= scannersHappy;
            boolean sellersMetMin = sellersMin == 0 || sellersAtGate >= sellersMin;
            boolean scannersMetMin = scannersMin == 0 || scannersAtGate >= scannersMin;

            if (sellersMetPlanned && scannersMetPlanned && (sellersPlanned > 0 || scannersPlanned > 0)) {
                milestoneStatus = "PLANNED_MET";
            } else if (sellersMetHappy && scannersMetHappy && (sellersHappy > 0 || scannersHappy > 0)) {
                milestoneStatus = "HAPPY_MET";
            } else if (sellersMetMin && scannersMetMin) {
                milestoneStatus = "MIN_MET";
            }

            int priority = gatePriorityMap.getOrDefault(gate.getGateId(), PRIMARY_GATE_NAMES.contains(gate.getName()) ? 1 : 2);

            breakdowns.add(GateAssignmentSummaryDto.GateBreakdownDto.builder()
                    .gateId(gate.getGateId())
                    .gateName(gate.getName())
                    .displayOrder(gate.getDisplayOrder() != null ? gate.getDisplayOrder() : 99)
                    .priority(priority)
                    .sellersMin(sellersMin)
                    .sellersHappy(sellersHappy)
                    .sellersNeeded(sellersPlanned)
                    .sellersAssigned(sellersAtGate)
                    .scannersMin(scannersMin)
                    .scannersHappy(scannersHappy)
                    .scannersNeeded(scannersPlanned)
                    .scannersAssigned(scannersAtGate)
                    .flexNeeded(0)
                    .flexAssigned(0)
                    .totalNeeded(gateTotalNeeded)
                    .totalAssigned(gateTotalAssigned)
                    .status(status)
                    .milestoneStatus(milestoneStatus)
                    .build());
        }

        int unassignedCount = Math.max(0, workers.size() - totalAssigned);

        return GateAssignmentSummaryDto.builder()
                .totalWorkers(workers.size())
                .totalNeeded(totalNeeded)
                .totalAssigned(totalAssigned)
                .unassignedCount(unassignedCount)
                .totalSellers(totalSellers)
                .sellersNeeded(totalSellersNeeded)
                .sellersAssigned(sellersAssigned)
                .totalScanners(totalScanners)
                .scannersNeeded(totalScannersNeeded)
                .scannersAssigned(scannersAssigned)
                .solitaryCount(0)
                .gateBreakdowns(breakdowns)
                .build();
    }

    // ==========================================
    // 6. Streaming SXSSFWorkbook Dual-Sheet Export
    // ==========================================

    @Transactional(readOnly = true)
    public byte[] exportAssignmentsExcel(UUID shiftId) {
        Shift shift = shiftRepository.findById(shiftId)
                .orElseThrow(() -> new EntityNotFoundException("Shift not found with id: " + shiftId));

        List<Person> workers = personRepository.findByShift_ShiftId(shiftId).stream()
                .sorted(Comparator.comparing((Person p) -> p.getGate() != null ? (p.getGate().getDisplayOrder() != null ? p.getGate().getDisplayOrder() : 999) : 9999)
                        .thenComparing((Person p) -> "Ticket Seller".equalsIgnoreCase(normalizeJobRole(p.getJobRole())) ? 0 : 1)
                        .thenComparing(Person::getLastName, Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER))
                        .thenComparing(Person::getFirstName, Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER)))
                .toList();

        GateAssignmentSummaryDto summary = getSummary(shiftId);

        try (SXSSFWorkbook workbook = new SXSSFWorkbook(100);
             ByteArrayOutputStream bos = new ByteArrayOutputStream()) {

            workbook.setCompressTempFiles(true);

            // Create styles
            Font headerFont = workbook.createFont();
            headerFont.setBold(true);
            headerFont.setColor(IndexedColors.WHITE.getIndex());

            CellStyle headerStyle = workbook.createCellStyle();
            headerStyle.setFont(headerFont);
            headerStyle.setFillForegroundColor(IndexedColors.DARK_BLUE.getIndex());
            headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            headerStyle.setAlignment(HorizontalAlignment.LEFT);

            Font boldFont = workbook.createFont();
            boldFont.setBold(true);

            CellStyle totalRowStyle = workbook.createCellStyle();
            totalRowStyle.setFont(boldFont);
            totalRowStyle.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
            totalRowStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);

            // Sheet 1: Roster
            String sheet1Name = sanitizeSheetName(shift.getName() + "AlbuquerqueInternationalBal");
            SXSSFSheet sheet1 = workbook.createSheet(sheet1Name);

            String[] rosterHeaders = {"First Name", "Last Name", "Organization", "Job", "Gate"};
            SXSSFRow headerRow1 = sheet1.createRow(0);
            for (int i = 0; i < rosterHeaders.length; i++) {
                Cell c = headerRow1.createCell(i);
                c.setCellValue(rosterHeaders[i]);
                c.setCellStyle(headerStyle);
            }

            int rowIdx = 1;
            for (Person p : workers) {
                SXSSFRow row = sheet1.createRow(rowIdx++);
                row.createCell(0).setCellValue(sanitizeExcelCell(p.getFirstName()));
                row.createCell(1).setCellValue(sanitizeExcelCell(p.getLastName()));
                row.createCell(2).setCellValue(sanitizeExcelCell(p.getOrganization()));
                row.createCell(3).setCellValue(sanitizeExcelCell(p.getJobRole()));
                row.createCell(4).setCellValue(sanitizeExcelCell(p.getGate() != null ? p.getGate().getName() : "Unassigned"));
            }

            // Sheet 2: Summary Pivot
            SXSSFSheet sheet2 = workbook.createSheet("Summary");
            String[] summaryHeaders = {"Gate", "Ticket Sellers", "Scanners", "Total Assigned", "Target Needed", "Status"};
            SXSSFRow headerRow2 = sheet2.createRow(0);
            for (int i = 0; i < summaryHeaders.length; i++) {
                Cell c = headerRow2.createCell(i);
                c.setCellValue(summaryHeaders[i]);
                c.setCellStyle(headerStyle);
            }

            int summaryRowIdx = 1;
            int sumSellers = 0, sumScanners = 0, sumAssigned = 0, sumNeeded = 0;

            for (GateAssignmentSummaryDto.GateBreakdownDto bd : summary.getGateBreakdowns()) {
                SXSSFRow row = sheet2.createRow(summaryRowIdx++);
                row.createCell(0).setCellValue("Gate " + bd.getGateName());
                row.createCell(1).setCellValue(bd.getSellersAssigned() + " / " + bd.getSellersNeeded());
                row.createCell(2).setCellValue(bd.getScannersAssigned() + " / " + bd.getScannersNeeded());
                row.createCell(3).setCellValue(bd.getTotalAssigned());
                row.createCell(4).setCellValue(bd.getTotalNeeded());
                row.createCell(5).setCellValue(bd.getStatus());

                sumSellers += bd.getSellersAssigned();
                sumScanners += bd.getScannersAssigned();
                sumAssigned += bd.getTotalAssigned();
                sumNeeded += bd.getTotalNeeded();
            }

            // Total Summary Row
            SXSSFRow totalRow = sheet2.createRow(summaryRowIdx);
            Cell totalLabel = totalRow.createCell(0);
            totalLabel.setCellValue("TOTAL");
            totalLabel.setCellStyle(totalRowStyle);

            Cell c1 = totalRow.createCell(1); c1.setCellValue(sumSellers); c1.setCellStyle(totalRowStyle);
            Cell c2 = totalRow.createCell(2); c2.setCellValue(sumScanners); c2.setCellStyle(totalRowStyle);
            Cell c3 = totalRow.createCell(3); c3.setCellValue(sumAssigned); c3.setCellStyle(totalRowStyle);
            Cell c4 = totalRow.createCell(4); c4.setCellValue(sumNeeded); c4.setCellStyle(totalRowStyle);

            Cell statusCell = totalRow.createCell(5);
            statusCell.setCellValue(sumAssigned >= sumNeeded ? "STAFFED" : "SHORT (" + (sumNeeded - sumAssigned) + ")");
            statusCell.setCellStyle(totalRowStyle);

            workbook.write(bos);
            workbook.dispose(); // clean up temp disk files

            return bos.toByteArray();
        } catch (Exception e) {
            log.error("Failed to generate Excel export", e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to generate Excel export: " + e.getMessage());
        }
    }

    // ==========================================
    // Sanitization and Safety Helpers
    // ==========================================

    private String sanitizeName(String input) {
        if (input == null) return "";
        String trimmed = input.trim();
        // Strip formula injection characters
        while (trimmed.startsWith("=") || trimmed.startsWith("+") || trimmed.startsWith("-") || trimmed.startsWith("@")) {
            trimmed = trimmed.substring(1).trim();
        }
        // Retain only characters matching whitelist
        StringBuilder sb = new StringBuilder();
        for (char c : trimmed.toCharArray()) {
            if (Character.isLetter(c) || Character.isWhitespace(c) || c == '-' || c == '\'' || c == '.') {
                sb.append(c);
            }
        }
        return sb.toString().trim();
    }

    private String sanitizeField(String input) {
        if (input == null) return "";
        String trimmed = input.trim();
        while (trimmed.startsWith("=") || trimmed.startsWith("+") || trimmed.startsWith("-") || trimmed.startsWith("@")) {
            trimmed = trimmed.substring(1).trim();
        }
        return trimmed;
    }

    private String sanitizeExcelCell(String val) {
        if (val == null) return "";
        String trimmed = val.trim();
        if (trimmed.startsWith("=") || trimmed.startsWith("+") || trimmed.startsWith("-") || trimmed.startsWith("@")) {
            return "'" + trimmed;
        }
        return trimmed;
    }

    private String sanitizeSheetName(String raw) {
        if (raw == null || raw.isBlank()) return "Assignments";
        String clean = raw.replaceAll("[\\\\/*?\\[\\]:]", "");
        return clean.length() > 31 ? clean.substring(0, 31) : clean;
    }

    public String normalizeJobRole(String role) {
        if (role == null || role.isBlank()) {
            return null;
        }
        String sanitized = sanitizeField(role);
        String cleaned = sanitized.toLowerCase().replaceAll("[_-]", " ").trim();
        if (cleaned.equals("ticket seller") || cleaned.equals("seller")) {
            return "Ticket Seller";
        }
        if (cleaned.equals("ticket scanner") || cleaned.equals("scanner")) {
            return "Ticket Scanner";
        }
        return sanitized.trim();
    }

    private String getPartSafe(String[] parts, int idx) {
        if (idx >= 0 && idx < parts.length) {
            return parts[idx].trim();
        }
        return "";
    }

    private String getCellValueSafe(Cell cell) {
        if (cell == null) return "";
        return switch (cell.getCellType()) {
            case STRING -> cell.getStringCellValue().trim();
            case NUMERIC -> String.valueOf((long) cell.getNumericCellValue());
            case BOOLEAN -> String.valueOf(cell.getBooleanCellValue());
            default -> "";
        };
    }

    private String[] parseCsvLine(String line) {
        List<String> tokens = new ArrayList<>();
        StringBuilder sb = new StringBuilder();
        boolean inQuotes = false;
        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            if (c == '\"') {
                inQuotes = !inQuotes;
            } else if (c == ',' && !inQuotes) {
                tokens.add(sb.toString().trim());
                sb.setLength(0);
            } else {
                sb.append(c);
            }
        }
        tokens.add(sb.toString().trim());
        return tokens.toArray(new String[0]);
    }
}
