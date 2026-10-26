package com.kuleanasystems.assets.service;

import com.kuleanasystems.assets.dto.GateDto;
import com.kuleanasystems.assets.dto.GatePatchDto;
import com.kuleanasystems.assets.entity.Gate;
import com.kuleanasystems.assets.repository.GateRepository;
import com.kuleanasystems.assets.repository.GateSpecification;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class GateService {

    private final GateRepository gateRepository;

    @Transactional
    public Gate createGate(GateDto dto) {
        Gate gate = new Gate();
        gate.setName(dto.getName());
        
        if (dto.getIsActive() != null) {
            gate.setIsActive(dto.getIsActive());
        }
        gate.setCashBanksRequiredForBusy(dto.getCashBanksRequiredForBusy());
        gate.setCashBanksRequiredForModerate(dto.getCashBanksRequiredForModerate());
        gate.setCashBanksRequiredForLite(dto.getCashBanksRequiredForLite());
        gate.setDisplayOrder(dto.getDisplayOrder());

        String currentUser = "System";
        gate.setCreatedBy(currentUser);

        return gateRepository.save(gate);
    }

    @Transactional
    public Gate updateGate(UUID gateId, GatePatchDto dto) {
        Gate gate = gateRepository.findById(gateId)
                .orElseThrow(() -> new EntityNotFoundException("Gate not found with id: " + gateId));

        if (dto.isDirty("name")) {
            gate.setName(dto.getName());
        }
        if (dto.isDirty("isActive")) {
            gate.setIsActive(dto.getIsActive());
        }
        if (dto.isDirty("cashBanksRequiredForBusy")) {
            gate.setCashBanksRequiredForBusy(dto.getCashBanksRequiredForBusy());
        }
        if (dto.isDirty("cashBanksRequiredForModerate")) {
            gate.setCashBanksRequiredForModerate(dto.getCashBanksRequiredForModerate());
        }
        if (dto.isDirty("cashBanksRequiredForLite")) {
            gate.setCashBanksRequiredForLite(dto.getCashBanksRequiredForLite());
        }
        if (dto.isDirty("displayOrder")) {
            gate.setDisplayOrder(dto.getDisplayOrder());
        }

        String currentUser = "System";
        gate.setUpdatedBy(currentUser);

        return gateRepository.save(gate);
    }

    @Transactional(readOnly = true)
    public List<Gate> searchGates(
            UUID gateId, String name, Boolean isActive, Integer cashBanksRequiredForBusy,
            Integer cashBanksRequiredForModerate, Integer cashBanksRequiredForLite,
            Integer displayOrder, String createdBy, Instant createdAt,
            String updatedBy, Instant updatedAt) {
        Specification<Gate> spec = GateSpecification.search(
                gateId, name, isActive, cashBanksRequiredForBusy, cashBanksRequiredForModerate,
                cashBanksRequiredForLite, displayOrder, createdBy, createdAt, updatedBy, updatedAt);
        return gateRepository.findAll(spec);
    }

    public List<Gate> getActiveGates() {
        return gateRepository.findByIsActiveTrueOrderByDisplayOrderAsc();
    }
}
