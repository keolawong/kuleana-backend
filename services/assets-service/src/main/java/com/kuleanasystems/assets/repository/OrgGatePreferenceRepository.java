package com.kuleanasystems.assets.repository;

import com.kuleanasystems.assets.entity.OrgGatePreference;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface OrgGatePreferenceRepository extends JpaRepository<OrgGatePreference, UUID> {
    List<OrgGatePreference> findByShift_ShiftId(UUID shiftId);
    Optional<OrgGatePreference> findByShift_ShiftIdAndOrganizationAndGate_GateId(UUID shiftId, String organization, UUID gateId);
    List<OrgGatePreference> findByShift_ShiftIdAndOrganization(UUID shiftId, String organization);
    void deleteByShift_ShiftId(UUID shiftId);
}
