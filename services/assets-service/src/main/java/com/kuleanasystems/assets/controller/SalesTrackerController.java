package com.kuleanasystems.assets.controller;

import com.kuleanasystems.assets.dto.ShiftSalesDto;
import com.kuleanasystems.assets.dto.ShiftWorkerDto;
import com.kuleanasystems.assets.service.SalesTrackerService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/sales-tracker")
@RequiredArgsConstructor
public class SalesTrackerController {

    private final SalesTrackerService salesTrackerService;

    @GetMapping("/{shiftId}/snapshots")
    @PreAuthorize("hasAnyRole('ASSET-READ', 'asset-read', 'ASSET-WRITE', 'asset-write', 'ASSET-IT', 'asset-it', 'ADMIN', 'admin', 'EDITOR', 'editor', 'VIEWER', 'viewer', 'PAGE_/SALES-TRACKER', 'PAGE_/sales-tracker')")
    public ResponseEntity<List<ShiftSalesDto>> getSnapshots(@PathVariable UUID shiftId) {
        return ResponseEntity.ok(salesTrackerService.getShiftSales(shiftId));
    }

    @PostMapping("/{shiftId}/snapshots")
    @PreAuthorize("hasAnyRole('ASSET-WRITE', 'asset-write', 'ASSET-IT', 'asset-it', 'ADMIN', 'admin', 'EDITOR', 'editor', 'PAGE_/SALES-TRACKER', 'PAGE_/sales-tracker')")
    public ResponseEntity<ShiftSalesDto> recordSnapshot(
            @PathVariable UUID shiftId,
            @RequestBody ShiftSalesDto dto) {
        ShiftSalesDto saved = salesTrackerService.saveShiftSales(shiftId, dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(saved);
    }

    @GetMapping("/{shiftId}/roles")
    @PreAuthorize("hasAnyRole('ASSET-READ', 'asset-read', 'ASSET-WRITE', 'asset-write', 'ASSET-IT', 'asset-it', 'ADMIN', 'admin', 'EDITOR', 'editor', 'VIEWER', 'viewer', 'PAGE_/SALES-TRACKER', 'PAGE_/sales-tracker')")
    public ResponseEntity<List<ShiftWorkerDto>> getRoles(@PathVariable UUID shiftId) {
        return ResponseEntity.ok(salesTrackerService.getShiftWorkers(shiftId));
    }

    @PutMapping("/{shiftId}/roles")
    @PreAuthorize("hasAnyRole('ASSET-WRITE', 'asset-write', 'ASSET-IT', 'asset-it', 'ADMIN', 'admin', 'EDITOR', 'editor', 'PAGE_/SALES-TRACKER', 'PAGE_/sales-tracker')")
    public ResponseEntity<List<ShiftWorkerDto>> updateRoles(
            @PathVariable UUID shiftId,
            @RequestBody List<ShiftWorkerDto> dtos) {
        return ResponseEntity.ok(salesTrackerService.updateShiftWorkers(shiftId, dtos));
    }

    @DeleteMapping("/{shiftId}/reset")
    @PreAuthorize("hasAnyRole('ASSET-WRITE', 'asset-write', 'ASSET-IT', 'asset-it', 'ADMIN', 'admin', 'EDITOR', 'editor', 'PAGE_/SALES-TRACKER', 'PAGE_/sales-tracker')")
    public ResponseEntity<Void> resetShiftSales(@PathVariable UUID shiftId) {
        salesTrackerService.resetShiftSales(shiftId);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{shiftId}/windows")
    @PreAuthorize("hasAnyRole('ASSET-WRITE', 'asset-write', 'ASSET-IT', 'asset-it', 'ADMIN', 'admin', 'EDITOR', 'editor', 'PAGE_/SALES-TRACKER', 'PAGE_/sales-tracker')")
    public ResponseEntity<Void> removeWindowSales(
            @PathVariable UUID shiftId,
            @RequestParam String windowName) {
        salesTrackerService.removeWindowSales(shiftId, windowName);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{shiftId}/gates")
    @PreAuthorize("hasAnyRole('ASSET-WRITE', 'asset-write', 'ASSET-IT', 'asset-it', 'ADMIN', 'admin', 'EDITOR', 'editor', 'PAGE_/SALES-TRACKER', 'PAGE_/sales-tracker')")
    public ResponseEntity<Void> removeGateSales(
            @PathVariable UUID shiftId,
            @RequestParam String gateName) {
        salesTrackerService.removeGateSales(shiftId, gateName);
        return ResponseEntity.noContent().build();
    }
}
