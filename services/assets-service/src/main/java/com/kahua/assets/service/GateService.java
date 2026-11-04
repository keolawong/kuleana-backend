package com.kahua.assets.service;

import com.kahua.assets.dto.GateDto;
import com.kahua.assets.dto.GatePatchDto;
import com.kahua.assets.entity.Gate;
import com.kahua.assets.entity.Operation;
import com.kahua.assets.repository.GateRepository;
import com.kahua.assets.repository.GateSpecification;
import com.kahua.assets.repository.OperationRepository;
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
    private final OperationRepository operationRepository;

    @Transactional
    public Gate createGate(GateDto dto) {
        Gate gate = new Gate();
        gate.setName(dto.getName());

        UUID opId = dto.getOperationId();
        Operation operation = null;
        if (opId != null) {
            operation = operationRepository.findById(opId)
                    .orElseThrow(() -> new EntityNotFoundException("Operation not found with id: " + opId));
        } else {
            // Default to first active operation
            List<Operation> ops = operationRepository.findAll();
            operation = ops.stream()
                    .filter(o -> Boolean.TRUE.equals(o.getIsActive()))
                    .findFirst()
                    .orElse(!ops.isEmpty() ? ops.get(0) : null);
            if (operation == null) {
                throw new IllegalStateException("No operation exists to associate with gate");
            }
        }
        gate.setOperation(operation);
        
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

        if (dto.isDirty("operationId") && dto.getOperationId() != null) {
            Operation op = operationRepository.findById(dto.getOperationId())
                    .orElseThrow(() -> new EntityNotFoundException("Operation not found with id: " + dto.getOperationId()));
            gate.setOperation(op);
        }
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
            UUID gateId, UUID operationId, UUID orgId, String name, Boolean isActive, Integer cashBanksRequiredForBusy,
            Integer cashBanksRequiredForModerate, Integer cashBanksRequiredForLite,
            Integer displayOrder, String createdBy, Instant createdAt,
            String updatedBy, Instant updatedAt) {
        Specification<Gate> spec = GateSpecification.search(
                gateId, operationId, orgId, name, isActive, cashBanksRequiredForBusy, cashBanksRequiredForModerate,
                cashBanksRequiredForLite, displayOrder, createdBy, createdAt, updatedBy, updatedAt);
        return gateRepository.findAll(spec);
    }
}
