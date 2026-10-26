package com.kuleanasystems.assets.controller;

import com.kuleanasystems.assets.dto.ReconciliationSummaryDto;
import com.kuleanasystems.assets.service.ReconciliationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/reconciliation")
@RequiredArgsConstructor
public class ReconciliationController {

    private final ReconciliationService reconciliationService;

    @GetMapping("/summary")
    public ResponseEntity<ReconciliationSummaryDto> getSummary(
            @RequestParam(required = false) UUID operationId,
            @RequestParam(required = false) UUID shiftId) {
        ReconciliationSummaryDto summary = reconciliationService.calculateShiftSummary(operationId, shiftId);
        return ResponseEntity.ok(summary);
    }
}
