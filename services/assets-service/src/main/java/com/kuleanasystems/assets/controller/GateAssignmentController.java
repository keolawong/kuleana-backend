package com.kuleanasystems.assets.controller;

import com.kuleanasystems.assets.dto.*;
import com.kuleanasystems.assets.entity.Person;
import com.kuleanasystems.assets.service.GateAssignmentService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/gate-assignments")
@RequiredArgsConstructor
public class GateAssignmentController {

    private final GateAssignmentService gateAssignmentService;

    // 1. Requirements / Quotas
    @GetMapping("/requirements")
    @PreAuthorize("hasAnyRole('ASSET-READ', 'asset-read', 'ASSET-WRITE', 'asset-write', 'ASSET-IT', 'asset-it', 'ADMIN', 'admin', 'EDITOR', 'editor', 'VIEWER', 'viewer', 'PAGE_/GATE-ASSIGNMENTS', 'PAGE_/gate-assignments', 'PAGE_/PERSON-STICKERS', 'PAGE_/person-stickers')")
    public ResponseEntity<List<ShiftGateRequirementDto>> getRequirements(@RequestParam UUID shiftId) {
        return ResponseEntity.ok(gateAssignmentService.getRequirements(shiftId));
    }

    @PutMapping("/requirements")
    @PreAuthorize("hasAnyRole('ASSET-WRITE', 'asset-write', 'ASSET-IT', 'asset-it', 'ADMIN', 'admin', 'EDITOR', 'editor', 'PAGE_/GATE-ASSIGNMENTS', 'PAGE_/gate-assignments', 'PAGE_/PERSON-STICKERS', 'PAGE_/person-stickers')")
    public ResponseEntity<List<ShiftGateRequirementDto>> updateRequirements(
            @RequestParam UUID shiftId,
            @RequestBody List<ShiftGateRequirementDto> dtos) {
        return ResponseEntity.ok(gateAssignmentService.updateRequirements(shiftId, dtos));
    }

    @PostMapping("/requirements/template")
    @PreAuthorize("hasAnyRole('ASSET-WRITE', 'asset-write', 'ASSET-IT', 'asset-it', 'ADMIN', 'admin', 'EDITOR', 'editor', 'PAGE_/GATE-ASSIGNMENTS', 'PAGE_/gate-assignments', 'PAGE_/PERSON-STICKERS', 'PAGE_/person-stickers')")
    public ResponseEntity<List<ShiftGateRequirementDto>> applyTemplate(
            @RequestParam UUID shiftId,
            @RequestParam(defaultValue = "BUSY") String template) {
        return ResponseEntity.ok(gateAssignmentService.applyTemplate(shiftId, template));
    }

    @GetMapping("/presets")
    @PreAuthorize("hasAnyRole('ASSET-READ', 'asset-read', 'ASSET-WRITE', 'asset-write', 'ASSET-IT', 'asset-it', 'ADMIN', 'admin', 'EDITOR', 'editor', 'VIEWER', 'viewer', 'PAGE_/GATE-ASSIGNMENTS', 'PAGE_/gate-assignments', 'PAGE_/ADMIN', 'PAGE_/admin')")
    public ResponseEntity<List<GatePresetQuotaDto>> getPresetQuotas(
            @RequestParam(defaultValue = "BUSY") String presetType) {
        return ResponseEntity.ok(gateAssignmentService.getPresetQuotas(presetType));
    }

    @PutMapping("/presets")
    @PreAuthorize("hasAnyRole('ASSET-WRITE', 'asset-write', 'ASSET-IT', 'asset-it', 'ADMIN', 'admin', 'PAGE_/GATE-ASSIGNMENTS', 'PAGE_/gate-assignments', 'PAGE_/ADMIN', 'PAGE_/admin')")
    public ResponseEntity<List<GatePresetQuotaDto>> updatePresetQuotas(
            @RequestParam(defaultValue = "BUSY") String presetType,
            @RequestBody List<GatePresetQuotaDto> dtos) {
        return ResponseEntity.ok(gateAssignmentService.updatePresetQuotas(presetType, dtos));
    }

    // 2. Workers Management
    @GetMapping("/workers")
    @PreAuthorize("hasAnyRole('ASSET-READ', 'asset-read', 'ASSET-WRITE', 'asset-write', 'ASSET-IT', 'asset-it', 'ADMIN', 'admin', 'EDITOR', 'editor', 'VIEWER', 'viewer', 'PAGE_/GATE-ASSIGNMENTS', 'PAGE_/gate-assignments', 'PAGE_/PERSON-STICKERS', 'PAGE_/person-stickers')")
    public ResponseEntity<List<Person>> getWorkers(@RequestParam UUID shiftId) {
        return ResponseEntity.ok(gateAssignmentService.getWorkers(shiftId));
    }

    @PostMapping(value = "/workers/parse", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAnyRole('ASSET-WRITE', 'asset-write', 'ASSET-IT', 'asset-it', 'ASSET-READ', 'asset-read', 'ADMIN', 'admin', 'EDITOR', 'editor', 'PAGE_/GATE-ASSIGNMENTS', 'PAGE_/gate-assignments', 'PAGE_/PERSON-STICKERS', 'PAGE_/person-stickers')")
    public ResponseEntity<List<PersonBulkWorkerDto>> parseWorkersFile(@RequestParam("file") MultipartFile file) {
        return ResponseEntity.ok(gateAssignmentService.parseWorkersFile(file));
    }

    @PostMapping("/workers/bulk")
    @PreAuthorize("hasAnyRole('ASSET-WRITE', 'asset-write', 'ASSET-IT', 'asset-it', 'ADMIN', 'admin', 'EDITOR', 'editor', 'PAGE_/GATE-ASSIGNMENTS', 'PAGE_/gate-assignments', 'PAGE_/PERSON-STICKERS', 'PAGE_/person-stickers')")
    public ResponseEntity<List<Person>> bulkUploadWorkers(
            @RequestParam UUID shiftId,
            @RequestBody List<PersonBulkWorkerDto> dtos) {
        return ResponseEntity.ok(gateAssignmentService.bulkUploadWorkers(shiftId, dtos));
    }

    @PatchMapping("/workers/{personId}")
    @PreAuthorize("hasAnyRole('ASSET-WRITE', 'asset-write', 'ASSET-IT', 'asset-it', 'ADMIN', 'admin', 'EDITOR', 'editor', 'PAGE_/GATE-ASSIGNMENTS', 'PAGE_/gate-assignments', 'PAGE_/PERSON-STICKERS', 'PAGE_/person-stickers')")
    public ResponseEntity<Person> patchWorker(
            @PathVariable UUID personId,
            @RequestBody PersonPatchDto patchDto) {
        return ResponseEntity.ok(gateAssignmentService.patchWorker(personId, patchDto));
    }

    // 3. Smart Gate Assignment
    @PostMapping("/assign")
    @PreAuthorize("hasAnyRole('ASSET-WRITE', 'asset-write', 'ASSET-IT', 'asset-it', 'ADMIN', 'admin', 'EDITOR', 'editor', 'PAGE_/GATE-ASSIGNMENTS', 'PAGE_/gate-assignments', 'PAGE_/PERSON-STICKERS', 'PAGE_/person-stickers')")
    public ResponseEntity<List<Person>> assignGates(@RequestParam UUID shiftId) {
        return ResponseEntity.ok(gateAssignmentService.assignGates(shiftId));
    }

    // 4. Summary & Status
    @GetMapping("/summary")
    @PreAuthorize("hasAnyRole('ASSET-READ', 'asset-read', 'ASSET-WRITE', 'asset-write', 'ASSET-IT', 'asset-it', 'ADMIN', 'admin', 'EDITOR', 'editor', 'VIEWER', 'viewer', 'PAGE_/GATE-ASSIGNMENTS', 'PAGE_/gate-assignments', 'PAGE_/PERSON-STICKERS', 'PAGE_/person-stickers')")
    public ResponseEntity<GateAssignmentSummaryDto> getSummary(@RequestParam UUID shiftId) {
        return ResponseEntity.ok(gateAssignmentService.getSummary(shiftId));
    }

    // 5. Org Gate Preferences
    @GetMapping("/preferences")
    @PreAuthorize("hasAnyRole('ASSET-READ', 'asset-read', 'ASSET-WRITE', 'asset-write', 'ASSET-IT', 'asset-it', 'ADMIN', 'admin', 'EDITOR', 'editor', 'VIEWER', 'viewer', 'PAGE_/GATE-ASSIGNMENTS', 'PAGE_/gate-assignments', 'PAGE_/PERSON-STICKERS', 'PAGE_/person-stickers')")
    public ResponseEntity<List<OrgGatePreferenceDto>> getPreferences(@RequestParam UUID shiftId) {
        return ResponseEntity.ok(gateAssignmentService.getPreferences(shiftId));
    }

    @PutMapping("/preferences")
    @PreAuthorize("hasAnyRole('ASSET-WRITE', 'asset-write', 'ASSET-IT', 'asset-it', 'ADMIN', 'admin', 'EDITOR', 'editor', 'PAGE_/GATE-ASSIGNMENTS', 'PAGE_/gate-assignments', 'PAGE_/PERSON-STICKERS', 'PAGE_/person-stickers')")
    public ResponseEntity<List<OrgGatePreferenceDto>> updatePreferences(
            @RequestParam UUID shiftId,
            @RequestBody List<OrgGatePreferenceDto> dtos) {
        return ResponseEntity.ok(gateAssignmentService.updatePreferences(shiftId, dtos));
    }

    // 6. Excel Streaming Export
    @GetMapping("/export")
    @PreAuthorize("hasAnyRole('ASSET-READ', 'asset-read', 'ASSET-WRITE', 'asset-write', 'ASSET-IT', 'asset-it', 'ADMIN', 'admin', 'EDITOR', 'editor', 'VIEWER', 'viewer', 'PAGE_/GATE-ASSIGNMENTS', 'PAGE_/gate-assignments', 'PAGE_/PERSON-STICKERS', 'PAGE_/person-stickers')")
    public ResponseEntity<byte[]> exportAssignments(@RequestParam UUID shiftId) {
        byte[] excelBytes = gateAssignmentService.exportAssignmentsExcel(shiftId);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"gate_assignments_" + shiftId + ".xlsx\"")
                .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(excelBytes);
    }

    // 7. Seller Sticker Management (Unidirectional)
    @PostMapping("/stickers")
    @PreAuthorize("hasAnyRole('ASSET-WRITE', 'asset-write', 'ASSET-IT', 'asset-it', 'ADMIN', 'admin', 'EDITOR', 'editor', 'PAGE_/GATE-ASSIGNMENTS', 'PAGE_/gate-assignments', 'PAGE_/PERSON-STICKERS', 'PAGE_/person-stickers')")
    public ResponseEntity<PersonStickerDto> createOrGetSticker(@RequestBody PersonStickerDto dto) {
        return ResponseEntity.ok(gateAssignmentService.createOrGetSellerSticker(
                dto.getShiftId(),
                dto.getPersonId(),
                dto.getGateId()
        ));
    }

    @GetMapping("/stickers")
    @PreAuthorize("hasAnyRole('ASSET-READ', 'asset-read', 'ASSET-WRITE', 'asset-write', 'ASSET-IT', 'asset-it', 'ADMIN', 'admin', 'EDITOR', 'editor', 'VIEWER', 'viewer', 'PAGE_/GATE-ASSIGNMENTS', 'PAGE_/gate-assignments', 'PAGE_/PERSON-STICKERS', 'PAGE_/person-stickers')")
    public ResponseEntity<List<PersonStickerDto>> getSessionStickers(@RequestParam UUID shiftId) {
        return ResponseEntity.ok(gateAssignmentService.getSessionStickers(shiftId));
    }

    @PostMapping("/stickers/bulk")
    @PreAuthorize("hasAnyRole('ASSET-WRITE', 'asset-write', 'ASSET-IT', 'asset-it', 'ADMIN', 'admin', 'EDITOR', 'editor', 'PAGE_/GATE-ASSIGNMENTS', 'PAGE_/gate-assignments', 'PAGE_/PERSON-STICKERS', 'PAGE_/person-stickers')")
    public ResponseEntity<List<PersonStickerDto>> generateMissingStickers(@RequestParam UUID shiftId) {
        return ResponseEntity.ok(gateAssignmentService.generateMissingSellerStickers(shiftId));
    }
}
