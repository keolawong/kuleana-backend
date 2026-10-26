package com.kuleanasystems.assets.repository;

import com.kuleanasystems.assets.entity.CashBank;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface CashBankRepository extends JpaRepository<CashBank, UUID> {
    List<CashBank> findByShiftShiftId(UUID shiftId);
    List<CashBank> findByOperationOperationId(UUID operationId);
    List<CashBank> findByOperationOperationIdAndShiftShiftId(UUID operationId, UUID shiftId);
}
