package com.kuleanasystems.assets.service;

import com.kuleanasystems.assets.entity.CashBank;
import com.kuleanasystems.assets.repository.CashBankRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CashBankService {

    private final CashBankRepository cashBankRepository;

    public List<CashBank> getCashBanks(UUID operationId, UUID shiftId) {
        if (operationId != null && shiftId != null) {
            return cashBankRepository.findByOperationOperationIdAndShiftShiftId(operationId, shiftId);
        } else if (shiftId != null) {
            return cashBankRepository.findByShiftShiftId(shiftId);
        } else if (operationId != null) {
            return cashBankRepository.findByOperationOperationId(operationId);
        }
        return cashBankRepository.findAll();
    }

    public Optional<CashBank> getCashBankById(UUID id) {
        return cashBankRepository.findById(id);
    }

    @Transactional
    public CashBank saveCashBank(CashBank cashBank) {
        return cashBankRepository.save(cashBank);
    }

    @Transactional
    public CashBank updateStatus(UUID id, String status) {
        CashBank bank = cashBankRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("CashBank not found: " + id));
        bank.setStatus(status);
        return cashBankRepository.save(bank);
    }

    @Transactional
    public CashBank reconcileBank(UUID id, BigDecimal officialCount) {
        CashBank bank = cashBankRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("CashBank not found: " + id));
        if (officialCount != null) {
            bank.setOfficialCount(officialCount);
        }
        bank.setStatus("RECONCILED");
        return cashBankRepository.save(bank);
    }

    @Transactional
    public List<CashBank> reconcileBanks(List<UUID> ids, BigDecimal officialCount) {
        List<CashBank> banks = cashBankRepository.findAllById(ids);
        for (CashBank bank : banks) {
            if (officialCount != null) {
                bank.setOfficialCount(officialCount);
            }
            bank.setStatus("RECONCILED");
        }
        return cashBankRepository.saveAll(banks);
    }
}
