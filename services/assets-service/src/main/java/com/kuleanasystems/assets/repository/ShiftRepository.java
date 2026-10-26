package com.kuleanasystems.assets.repository;

import com.kuleanasystems.assets.entity.Shift;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ShiftRepository extends JpaRepository<Shift, UUID> {
    List<Shift> findByOperationOperationId(UUID operationId);
    List<Shift> findByOperationOperationIdAndStatus(UUID operationId, String status);
    List<Shift> findByStatus(String status);
}
