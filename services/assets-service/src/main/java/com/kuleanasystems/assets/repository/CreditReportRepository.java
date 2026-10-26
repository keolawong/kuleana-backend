package com.kuleanasystems.assets.repository;

import com.kuleanasystems.assets.entity.CreditReport;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface CreditReportRepository extends JpaRepository<CreditReport, UUID> {
    List<CreditReport> findByShiftShiftId(UUID shiftId);
    List<CreditReport> findByOperationOperationId(UUID operationId);
    List<CreditReport> findByOperationOperationIdAndShiftShiftId(UUID operationId, UUID shiftId);
}
