package com.kuleanasystems.assets.service;

import com.kuleanasystems.assets.dto.ReconciliationSummaryDto;
import com.kuleanasystems.assets.entity.CashBank;
import com.kuleanasystems.assets.entity.CreditReport;
import com.kuleanasystems.assets.entity.Operation;
import com.kuleanasystems.assets.entity.Shift;
import com.kuleanasystems.assets.repository.CashBankRepository;
import com.kuleanasystems.assets.repository.CreditReportRepository;
import com.kuleanasystems.assets.repository.OperationRepository;
import com.kuleanasystems.assets.repository.ShiftRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ReconciliationService {

    private final CashBankRepository cashBankRepository;
    private final CreditReportRepository creditReportRepository;
    private final OperationRepository operationRepository;
    private final ShiftRepository shiftRepository;

    public ReconciliationSummaryDto calculateShiftSummary(UUID operationId, UUID shiftId) {
        Operation operation = operationId != null ? operationRepository.findById(operationId).orElse(null) : null;
        Shift shift = shiftId != null ? shiftRepository.findById(shiftId).orElse(null) : null;

        List<CashBank> banks = (operationId != null && shiftId != null)
                ? cashBankRepository.findByOperationOperationIdAndShiftShiftId(operationId, shiftId)
                : (shiftId != null)
                ? cashBankRepository.findByShiftShiftId(shiftId)
                : (operationId != null)
                ? cashBankRepository.findByOperationOperationId(operationId)
                : cashBankRepository.findAll();

        List<CreditReport> creditReports = (operationId != null && shiftId != null)
                ? creditReportRepository.findByOperationOperationIdAndShiftShiftId(operationId, shiftId)
                : (shiftId != null)
                ? creditReportRepository.findByShiftShiftId(shiftId)
                : (operationId != null)
                ? creditReportRepository.findByOperationOperationId(operationId)
                : creditReportRepository.findAll();

        // Group banks by Person / Cashier or Device for envelope deduplication
        Map<String, List<CashBank>> banksByCashier = banks.stream()
                .collect(Collectors.groupingBy(b -> {
                    if (b.getPerson() != null && b.getPerson().getFirstName() != null) {
                        return b.getPerson().getFirstName() + " " + (b.getPerson().getLastName() != null ? b.getPerson().getLastName() : "");
                    }
                    return b.getDeviceId() != null ? "Device: " + b.getDeviceId() : "Unassigned Float";
                }));

        // Map credit totals by person or device
        Map<String, BigDecimal> creditByCashier = new HashMap<>();
        for (CreditReport cr : creditReports) {
            String key = (cr.getPerson() != null && cr.getPerson().getFirstName() != null)
                    ? cr.getPerson().getFirstName() + " " + (cr.getPerson().getLastName() != null ? cr.getPerson().getLastName() : "")
                    : (cr.getDeviceId() != null ? "Device: " + cr.getDeviceId() : "Unknown");
            BigDecimal current = creditByCashier.getOrDefault(key, BigDecimal.ZERO);
            creditByCashier.put(key, current.add(cr.getTotalAmount() != null ? cr.getTotalAmount() : BigDecimal.ZERO));
        }

        List<ReconciliationSummaryDto.CashierIntakeDto> cashierBreakdowns = new ArrayList<>();
        BigDecimal totalStartingFloat = BigDecimal.ZERO;
        BigDecimal totalVerifiedCash = BigDecimal.ZERO;
        BigDecimal totalCreditCard = BigDecimal.ZERO;
        int reconciledCount = 0;

        for (Map.Entry<String, List<CashBank>> entry : banksByCashier.entrySet()) {
            String cashierName = entry.getKey();
            List<CashBank> cashierBanks = entry.getValue();

            BigDecimal cashierFloat = cashierBanks.stream()
                    .map(b -> b.getStartingAmount() != null ? b.getStartingAmount() : BigDecimal.ZERO)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            // Deduplicate sibling officialCounts if identical envelope
            Set<BigDecimal> distinctCounts = cashierBanks.stream()
                    .map(b -> b.getOfficialCount() != null ? b.getOfficialCount() : BigDecimal.ZERO)
                    .collect(Collectors.toSet());
            BigDecimal cashierVerifiedCount = distinctCounts.stream().reduce(BigDecimal.ZERO, BigDecimal::add);

            BigDecimal cashierCredit = creditByCashier.getOrDefault(cashierName, BigDecimal.ZERO);
            BigDecimal netSales = cashierVerifiedCount.add(cashierCredit).subtract(cashierFloat);

            boolean isReconciled = cashierBanks.stream().allMatch(b -> "RECONCILED".equalsIgnoreCase(b.getStatus()));
            if (isReconciled) reconciledCount++;

            CashBank firstBank = cashierBanks.get(0);
            String gateName = firstBank.getGate() != null ? firstBank.getGate().getName() : "Gate Unassigned";
            String deviceId = firstBank.getDeviceId() != null ? firstBank.getDeviceId() : "N/A";

            totalStartingFloat = totalStartingFloat.add(cashierFloat);
            totalVerifiedCash = totalVerifiedCash.add(cashierVerifiedCount);
            totalCreditCard = totalCreditCard.add(cashierCredit);

            cashierBreakdowns.add(ReconciliationSummaryDto.CashierIntakeDto.builder()
                    .cashierName(cashierName)
                    .gateName(gateName)
                    .deviceId(deviceId)
                    .bankIds(cashierBanks.stream().map(b -> b.getCashBankId().toString()).collect(Collectors.toList()))
                    .startingFloat(cashierFloat)
                    .officialCount(cashierVerifiedCount)
                    .creditCardTotal(cashierCredit)
                    .netSales(netSales)
                    .variance(BigDecimal.ZERO)
                    .status(isReconciled ? "RECONCILED" : "IN_PROGRESS")
                    .deduplicatedEnvelope(cashierBanks.size() > 1 && distinctCounts.size() == 1)
                    .build());
        }

        BigDecimal grossMonetary = totalVerifiedCash.add(totalCreditCard);
        BigDecimal netSalesTotal = grossMonetary.subtract(totalStartingFloat);

        return ReconciliationSummaryDto.builder()
                .operationId(operationId)
                .shiftId(shiftId)
                .operationName(operation != null ? operation.getName() : "Active Operation")
                .shiftName(shift != null ? shift.getName() : "Active Shift")
                .startingFloatTotal(totalStartingFloat)
                .verifiedCashIntake(totalVerifiedCash)
                .creditCardBatchTotal(totalCreditCard)
                .grossMonetaryIntake(grossMonetary)
                .netSalesTotal(netSalesTotal)
                .totalBanksCount(banks.size())
                .reconciledBanksCount(reconciledCount)
                .cashierBreakdowns(cashierBreakdowns)
                .build();
    }
}
