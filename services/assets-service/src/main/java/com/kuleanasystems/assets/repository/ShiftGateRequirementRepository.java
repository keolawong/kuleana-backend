package com.kuleanasystems.assets.repository;

import com.kuleanasystems.assets.entity.ShiftGateRequirement;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ShiftGateRequirementRepository extends JpaRepository<ShiftGateRequirement, UUID> {
    List<ShiftGateRequirement> findByShift_ShiftId(UUID shiftId);
    Optional<ShiftGateRequirement> findByShift_ShiftIdAndGate_GateIdAndJobRole(UUID shiftId, UUID gateId, String jobRole);
    void deleteByShift_ShiftId(UUID shiftId);
}
