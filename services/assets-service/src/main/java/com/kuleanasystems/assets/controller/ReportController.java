package com.kuleanasystems.assets.controller;

import com.kuleanasystems.assets.dto.report.OperationReportDto;
import com.kuleanasystems.assets.dto.report.ShiftReportDto;
import com.kuleanasystems.assets.service.ReportService;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping({"/api/reports", "/reports"})
@RequiredArgsConstructor
public class ReportController {

    private final ReportService reportService;

    @GetMapping("/shifts/{shiftId}")
    @PreAuthorize("hasAnyRole('ASSET-READ', 'asset-read', 'ASSET-WRITE', 'asset-write', 'ASSET-IT', 'asset-it', 'ADMIN', 'admin', 'EDITOR', 'editor', 'VIEWER', 'viewer', 'PAGE_/REPORTS', 'PAGE_/reports', 'PAGE_/RECONCILE', 'PAGE_/reconcile')")
    public ResponseEntity<ShiftReportDto> getShiftReport(@PathVariable UUID shiftId) {
        return ResponseEntity.ok(reportService.getShiftReport(shiftId));
    }

    @GetMapping("/operations/{operationId}")
    @PreAuthorize("hasAnyRole('ASSET-READ', 'asset-read', 'ASSET-WRITE', 'asset-write', 'ASSET-IT', 'asset-it', 'ADMIN', 'admin', 'EDITOR', 'editor', 'VIEWER', 'viewer', 'PAGE_/REPORTS', 'PAGE_/reports', 'PAGE_/RECONCILE', 'PAGE_/reconcile')")
    public ResponseEntity<OperationReportDto> getOperationReport(@PathVariable UUID operationId) {
        return ResponseEntity.ok(reportService.getOperationReport(operationId));
    }
}
