package com.kuleanasystems.assets.repository;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.domain.Specification;
import java.util.List;
import org.springframework.lang.Nullable;

import com.kuleanasystems.assets.entity.Shift;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface ShiftRepository extends JpaRepository<Shift, UUID>, JpaSpecificationExecutor<Shift> {

    @Override
    @EntityGraph(attributePaths = {"operation"})
    List<Shift> findAll(@Nullable Specification<Shift> spec);

    @EntityGraph(attributePaths = {"operation"})
    List<Shift> findByOperation_OperationId(UUID operationId);


    List<Shift> findByOperationOperationId(UUID operationId);
    List<Shift> findByOperationOperationIdAndStatus(UUID operationId, String status);
    List<Shift> findByStatus(String status);
}
