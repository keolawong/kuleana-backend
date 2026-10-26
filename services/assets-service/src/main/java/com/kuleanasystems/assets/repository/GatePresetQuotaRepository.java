package com.kuleanasystems.assets.repository;

import com.kuleanasystems.assets.entity.GatePresetQuota;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface GatePresetQuotaRepository extends JpaRepository<GatePresetQuota, UUID> {
    List<GatePresetQuota> findByPresetTypeIgnoreCase(String presetType);
    Optional<GatePresetQuota> findByPresetTypeIgnoreCaseAndGate_GateIdAndJobRole(String presetType, UUID gateId, String jobRole);
}
