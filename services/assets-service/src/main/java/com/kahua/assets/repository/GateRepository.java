package com.kahua.assets.repository;

import com.kahua.assets.entity.Gate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface GateRepository extends JpaRepository<Gate, UUID>, JpaSpecificationExecutor<Gate> {
    List<Gate> findByIsActiveTrueOrderByDisplayOrderAscNameAsc();
    List<Gate> findByOperation_OperationId(UUID operationId);
    List<Gate> findByOperation_OperationIdOrderByDisplayOrderAscNameAsc(UUID operationId);
}
