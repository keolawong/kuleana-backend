package com.kuleanasystems.assets.repository;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.domain.Specification;
import java.util.List;
import org.springframework.lang.Nullable;

import com.kuleanasystems.assets.entity.CashBank;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface CashBankRepository extends JpaRepository<CashBank, UUID>, JpaSpecificationExecutor<CashBank> {

    @Override
    @EntityGraph(attributePaths = {"operation", "shift", "gate", "person"})
    List<CashBank> findAll(@Nullable Specification<CashBank> spec);

    @Override
    @EntityGraph(attributePaths = {"operation", "shift", "gate", "person"})
    List<CashBank> findAll();

    List<CashBank> findByParentCashBankId(UUID parentCashBankId);

    @EntityGraph(attributePaths = {"operation", "shift", "gate"})
    List<CashBank> findByShift_ShiftId(UUID shiftId);

    @EntityGraph(attributePaths = {"operation", "shift", "gate"})
    List<CashBank> findByOperation_OperationId(UUID operationId);

    @org.springframework.data.jpa.repository.Modifying
    @org.springframework.data.jpa.repository.Query("UPDATE CashBank c SET c.deviceId = :newDeviceId WHERE c.deviceId = :oldDeviceId")
    int updateDeviceId(@org.springframework.data.repository.query.Param("oldDeviceId") String oldDeviceId, @org.springframework.data.repository.query.Param("newDeviceId") String newDeviceId);


    List<CashBank> findByShiftShiftId(UUID shiftId);
    List<CashBank> findByOperationOperationId(UUID operationId);
    List<CashBank> findByOperationOperationIdAndShiftShiftId(UUID operationId, UUID shiftId);
}
