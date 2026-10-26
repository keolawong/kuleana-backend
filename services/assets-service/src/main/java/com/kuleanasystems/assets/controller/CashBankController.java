package com.kuleanasystems.assets.controller;

import com.kuleanasystems.assets.entity.CashBank;
import com.kuleanasystems.assets.service.CashBankService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/cash-banks")
@RequiredArgsConstructor
public class CashBankController {

    private final CashBankService cashBankService;

    @GetMapping
    public ResponseEntity<List<CashBank>> getCashBanks(
            @RequestParam(required = false) UUID operationId,
            @RequestParam(required = false) UUID shiftId) {
        List<CashBank> banks = cashBankService.getCashBanks(operationId, shiftId);
        return ResponseEntity.ok(banks);
    }

    @GetMapping("/{id}")
    public ResponseEntity<CashBank> getCashBankById(@PathVariable UUID id) {
        return cashBankService.getCashBankById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<CashBank> createCashBank(@RequestBody CashBank bank) {
        CashBank saved = cashBankService.saveCashBank(bank);
        return ResponseEntity.ok(saved);
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<CashBank> updateStatus(@PathVariable UUID id, @RequestParam String status) {
        CashBank updated = cashBankService.updateStatus(id, status);
        return ResponseEntity.ok(updated);
    }

    @PatchMapping("/{id}/reconcile")
    public ResponseEntity<CashBank> reconcileBank(@PathVariable UUID id, @RequestParam(required = false) java.math.BigDecimal officialCount) {
        CashBank updated = cashBankService.reconcileBank(id, officialCount);
        return ResponseEntity.ok(updated);
    }

    @PostMapping("/reconcile-bulk")
    public ResponseEntity<List<CashBank>> reconcileBanks(@RequestBody List<UUID> bankIds, @RequestParam(required = false) java.math.BigDecimal officialCount) {
        List<CashBank> updated = cashBankService.reconcileBanks(bankIds, officialCount);
        return ResponseEntity.ok(updated);
    }
}
