package com.kuleanasystems.assets.repository;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.domain.Specification;
import java.util.List;
import org.springframework.lang.Nullable;

import com.kuleanasystems.assets.entity.CreditReport;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface CreditReportRepository extends JpaRepository<CreditReport, UUID>, JpaSpecificationExecutor<CreditReport> {

    @Override
    @EntityGraph(attributePaths = {"operation", "shift", "gate", "person"})
    List<CreditReport> findAll(@Nullable Specification<CreditReport> spec);

    @Override
    @EntityGraph(attributePaths = {"operation", "shift", "gate", "person"})
    List<CreditReport> findAll();

    @EntityGraph(attributePaths = {"operation", "shift", "gate"})
    List<CreditReport> findByShift_ShiftId(UUID shiftId);

    @EntityGraph(attributePaths = {"operation", "shift", "gate"})
    List<CreditReport> findByOperation_OperationId(UUID operationId);

    @org.springframework.data.jpa.repository.Modifying
    @org.springframework.data.jpa.repository.Query("UPDATE CreditReport cr SET cr.deviceId = :newDeviceId WHERE cr.deviceId = :oldDeviceId")
    int updateDeviceId(@org.springframework.data.repository.query.Param("oldDeviceId") String oldDeviceId, @org.springframework.data.repository.query.Param("newDeviceId") String newDeviceId);


    List<CreditReport> findByShiftShiftId(UUID shiftId);
    List<CreditReport> findByOperationOperationId(UUID operationId);
    List<CreditReport> findByOperationOperationIdAndShiftShiftId(UUID operationId, UUID shiftId);
}
