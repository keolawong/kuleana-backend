package com.kuleanasystems.assets.repository;

import com.kuleanasystems.assets.entity.CashBankHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface CashBankHistoryRepository extends JpaRepository<CashBankHistory, UUID> {
    List<CashBankHistory> findByShift_ShiftId(UUID shiftId);
    List<CashBankHistory> findByCashBankIdOrderByCreatedAtAsc(UUID cashBankId);
}
