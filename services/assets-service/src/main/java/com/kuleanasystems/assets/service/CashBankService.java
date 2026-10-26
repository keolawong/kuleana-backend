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
    public CashBank updateOfficialCount(UUID id, BigDecimal officialCount, Integer count100s, Integer count50s, Integer count20s, Integer count10s, Integer count5s, Integer count1s) {
        CashBank bank = cashBankRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("CashBank not found: " + id));
        bank.setOfficialCount(officialCount);
        if (count100s != null) bank.setCount100s(count100s);
        if (count50s != null) bank.setCount50s(count50s);
        if (count20s != null) bank.setCount20s(count20s);
        if (count10s != null) bank.setCount10s(count10s);
        if (count5s != null) bank.setCount5s(count5s);
        if (count1s != null) bank.setCount1s(count1s);
        return cashBankRepository.save(bank);
    }
}
