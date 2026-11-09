package com.kahua.assets.controller;

import com.kahua.assets.dto.ShiftDto;
import com.kahua.assets.dto.ShiftPatchDto;
import com.kahua.assets.entity.Shift;
import com.kahua.assets.service.ShiftService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/shifts")
@RequiredArgsConstructor
public class ShiftController {

    private final ShiftService shiftService;

    @PostMapping
    public ResponseEntity<Shift> createShift(@Valid @RequestBody ShiftDto shiftDto) {
        Shift createdShift = shiftService.createShift(shiftDto);
        return ResponseEntity.created(URI.create("/api/shifts/" + createdShift.getShiftId())).body(createdShift);
    }

    @PatchMapping("/{shiftId}")
    public ResponseEntity<Shift> updateShift(@PathVariable UUID shiftId, @RequestBody ShiftPatchDto shiftPatchDto) {
        Shift updatedShift = shiftService.updateShift(shiftId, shiftPatchDto);
        return ResponseEntity.ok(updatedShift);
    }

    @GetMapping
    public ResponseEntity<List<Shift>> getShifts(
            @RequestParam(required = false) UUID shiftId,
            @RequestParam(required = false) UUID operationId,
            @RequestParam(required = false) String name,
            @RequestParam(required = false) Instant startTime,
            @RequestParam(required = false) Instant endTime,
            @RequestParam(required = false) String traffic,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String createdBy,
            @RequestParam(required = false) Instant createdAt,
            @RequestParam(required = false) String updatedBy,
            @RequestParam(required = false) Instant updatedAt,
            @RequestHeader(value = "X-Org-Id", required = false) String orgIdHeader) {

        UUID effectiveOrgId = resolveOrgId(orgIdHeader);

        List<Shift> shifts = shiftService.searchShifts(
                shiftId, operationId, effectiveOrgId, name, startTime, endTime, traffic,
                status, createdBy, createdAt, updatedBy, updatedAt, null);

        if (shifts.isEmpty()) {
            return ResponseEntity.noContent().build();
        }

        return ResponseEntity.ok(shifts);
    }

    @GetMapping("/active")
    public ResponseEntity<List<Shift>> getActiveShifts(
            @RequestParam(required = false) UUID shiftId,
            @RequestParam(required = false) UUID operationId,
            @RequestParam(required = false) String name,
            @RequestParam(required = false) Instant startTime,
            @RequestParam(required = false) Instant endTime,
            @RequestParam(required = false) String traffic,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String createdBy,
            @RequestParam(required = false) Instant createdAt,
            @RequestParam(required = false) String updatedBy,
            @RequestParam(required = false) Instant updatedAt,
            @RequestHeader(value = "X-Org-Id", required = false) String orgIdHeader) {

        UUID effectiveOrgId = resolveOrgId(orgIdHeader);

        List<Shift> shifts = shiftService.searchShifts(
                shiftId, operationId, effectiveOrgId, name, startTime, endTime, traffic,
                status, createdBy, createdAt, updatedBy, updatedAt, true);

        if (shifts.isEmpty()) {
            return ResponseEntity.noContent().build();
        }

        return ResponseEntity.ok(shifts);
    }

    private UUID resolveOrgId(String orgIdHeader) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return com.kuleanasystems.security.TenantSecurityUtil.resolveEffectiveOrgId(orgIdHeader, auth);
    }
}
