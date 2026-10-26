package com.kuleanasystems.assets.controller;

import com.kuleanasystems.assets.dto.CreditReportDto;
import com.kuleanasystems.assets.dto.CreditReportPatchDto;
import com.kuleanasystems.assets.entity.CreditReport;
import com.kuleanasystems.assets.service.CreditReportService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.net.URI;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/credit-reports")
@RequiredArgsConstructor
public class CreditReportController {

    private final CreditReportService creditReportService;

    @PostMapping
    public ResponseEntity<CreditReport> createCreditReport(@Valid @RequestBody CreditReportDto creditReportDto) {
        CreditReport createdCreditReport = creditReportService.createCreditReport(creditReportDto);
        return ResponseEntity.created(URI.create("/api/credit-reports/" + createdCreditReport.getCreditReportId())).body(createdCreditReport);
    }

    @PatchMapping("/{creditReportId}")
    public ResponseEntity<CreditReport> updateCreditReport(@PathVariable UUID creditReportId, @RequestBody CreditReportPatchDto creditReportPatchDto) {
        CreditReport updatedCreditReport = creditReportService.updateCreditReport(creditReportId, creditReportPatchDto);
        return ResponseEntity.ok(updatedCreditReport);
    }

    @GetMapping
    public ResponseEntity<List<CreditReport>> getCreditReports(
            @RequestParam(required = false) UUID creditReportId,
            @RequestParam(required = false) BigDecimal totalAmount,
            @RequestParam(required = false) UUID operationId,
            @RequestParam(required = false) UUID shiftId,
            @RequestParam(required = false) UUID gateId,
            @RequestParam(required = false) String deviceId,
            @RequestParam(required = false) UUID processId,
            @RequestParam(required = false) UUID personStickerId,
            @RequestParam(required = false) String createdBy,
            @RequestParam(required = false) Instant createdAt,
            @RequestParam(required = false) String updatedBy,
            @RequestParam(required = false) Instant updatedAt) {

        List<CreditReport> creditReports = creditReportService.searchCreditReports(
                creditReportId, totalAmount, operationId, shiftId, gateId,
                deviceId, processId, personStickerId, createdBy, createdAt, updatedBy, updatedAt);

        if (creditReports.isEmpty()) {
            return ResponseEntity.noContent().build();
        }

        return ResponseEntity.ok(creditReports);
    }
}