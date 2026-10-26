package com.kuleanasystems.assets.controller;

import com.kuleanasystems.assets.dto.CashBankDto;
import com.kuleanasystems.assets.dto.CashBankPatchDto;
import com.kuleanasystems.assets.entity.CashBank;
import com.kuleanasystems.assets.service.CashBankService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.net.URI;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import com.kuleanasystems.assets.entity.CashBankHistory;

@RestController
@RequestMapping("/api/cash-banks")
@RequiredArgsConstructor
public class CashBankController {

    private final CashBankService cashBankService;

    @PostMapping
    public ResponseEntity<CashBank> createCashBank(@Valid @RequestBody CashBankDto cashBankDto) {
        CashBank createdCashBank = cashBankService.createCashBank(cashBankDto);
        return ResponseEntity.created(URI.create("/api/cash-banks/" + createdCashBank.getCashBankId())).body(createdCashBank);
    }

    @PostMapping("/bulk")
    public ResponseEntity<List<CashBank>> createCashBanks(@Valid @RequestBody List<CashBankDto> cashBankDtos) {
        List<CashBank> createdCashBanks = cashBankService.createCashBanks(cashBankDtos);
        return ResponseEntity.status(HttpStatus.CREATED).body(createdCashBanks);
    }

    @PatchMapping("/{cashBankId}")
    public ResponseEntity<CashBank> updateCashBank(@PathVariable UUID cashBankId, @RequestBody CashBankPatchDto cashBankPatchDto) {
        CashBank updatedCashBank = cashBankService.updateCashBank(cashBankId, cashBankPatchDto);
        return ResponseEntity.ok(updatedCashBank);
    }

    @GetMapping
    public ResponseEntity<List<CashBank>> getCashBanks(
            @RequestParam(required = false) UUID cashBankId,
            @RequestParam(required = false) String bankName,
            @RequestParam(required = false) BigDecimal startingAmount,
            @RequestParam(required = false) BigDecimal officialCount,
            @RequestParam(required = false) Integer count100s,
            @RequestParam(required = false) Integer count50s,
            @RequestParam(required = false) Integer count20s,
            @RequestParam(required = false) Integer count10s,
            @RequestParam(required = false) Integer count5s,
            @RequestParam(required = false) Integer count1s,
            @RequestParam(required = false) Integer count1cs,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) UUID operationId,
            @RequestParam(required = false) UUID shiftId,
            @RequestParam(required = false) UUID gateId,
            @RequestParam(required = false) String deviceId,
            @RequestParam(required = false) UUID parentCashBankId,
            @RequestParam(required = false) UUID processId,
            @RequestParam(required = false) UUID personStickerId,
            @RequestParam(required = false) String createdBy,
            @RequestParam(required = false) Instant createdAt,
            @RequestParam(required = false) String updatedBy,
            @RequestParam(required = false) Instant updatedAt) {

        List<CashBank> cashBanks = cashBankService.searchCashBanks(
                cashBankId, bankName, startingAmount, officialCount, count100s, count50s,
                count20s, count10s, count5s, count1s, count1cs, status, operationId, shiftId, gateId,
                deviceId, parentCashBankId, processId, personStickerId, createdBy, createdAt, updatedBy, updatedAt);

        if (shiftId != null) {
            List<CashBankHistory> historyList = cashBankService.getHistoryByShiftId(shiftId);
            
            if (gateId != null) {
                historyList = historyList.stream()
                        .filter(h -> h.getGate() != null && h.getGate().getGateId().equals(gateId))
                        .collect(Collectors.toList());
            }
            
            Map<UUID, CashBankHistory> latestHistoryMap = historyList.stream()
                .collect(Collectors.toMap(
                    CashBankHistory::getCashBankId,
                    h -> h,
                    (h1, h2) -> h1.getCreatedAt().isAfter(h2.getCreatedAt()) ? h1 : h2
                ));

            List<CashBank> mergedList = new ArrayList<>(cashBanks);
            final List<CashBank> activeCashBanks = cashBanks;
            
            List<UUID> inactiveIds = latestHistoryMap.values().stream()
                .map(CashBankHistory::getCashBankId)
                .filter(id -> activeCashBanks.stream().noneMatch(cb -> cb.getCashBankId().equals(id)))
                .collect(Collectors.toList());

            Map<UUID, CashBank> currentBankMap = new java.util.HashMap<>();
            if (!inactiveIds.isEmpty()) {
                currentBankMap = cashBankService.getCashBanksByIds(inactiveIds).stream()
                    .collect(Collectors.toMap(CashBank::getCashBankId, cb -> cb));
            }
            
            for (CashBankHistory history : latestHistoryMap.values()) {
                boolean isActive = activeCashBanks.stream().anyMatch(cb -> cb.getCashBankId().equals(history.getCashBankId()));
                
                if (!isActive) {
                    CashBank currentCb = currentBankMap.get(history.getCashBankId());
                    if (currentCb != null && "Available".equals(currentCb.getStatus())) {
                        continue;
                    }

                    boolean hasReturnedStatus = historyList.stream()
                        .filter(h -> h.getCashBankId().equals(history.getCashBankId()))
                        .anyMatch(h -> "Returned".equals(h.getStatus()) || "Returned Unused".equals(h.getStatus()));
                        
                    if (hasReturnedStatus) {
                        CashBank historicalCb = new CashBank();
                        historicalCb.setCashBankId(history.getCashBankId());
                        historicalCb.setBankName(history.getBankName());
                        historicalCb.setStartingAmount(history.getStartingAmount());
                        historicalCb.setOfficialCount(history.getOfficialCount());
                        historicalCb.setCount100s(history.getCount100s());
                        historicalCb.setCount50s(history.getCount50s());
                        historicalCb.setCount20s(history.getCount20s());
                        historicalCb.setCount10s(history.getCount10s());
                        historicalCb.setCount5s(history.getCount5s());
                        historicalCb.setCount1s(history.getCount1s());
                        historicalCb.setCount1cs(history.getCount1cs());
                        historicalCb.setStatus(history.getStatus());
                        historicalCb.setOperation(history.getOperation());
                        historicalCb.setShift(history.getShift());
                        historicalCb.setGate(history.getGate());
                        historicalCb.setDeviceId(history.getDeviceId());
                        historicalCb.setParentCashBankId(history.getParentCashBankId());
                        historicalCb.setProcessId(history.getProcessId());
                        mergedList.add(historicalCb);
                    }
                }
            }
            cashBanks = mergedList;
        }

        if (cashBanks.isEmpty()) {
            return ResponseEntity.noContent().build();
        }

        return ResponseEntity.ok(cashBanks);
    }

    @GetMapping("/{id}")
    public ResponseEntity<CashBank> getCashBankById(@PathVariable UUID id) {
        return cashBankService.getCashBankById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
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
