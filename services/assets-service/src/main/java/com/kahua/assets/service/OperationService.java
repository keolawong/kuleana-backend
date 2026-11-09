package com.kahua.assets.service;

import com.kahua.assets.dto.OperationDto;
import com.kahua.assets.dto.OperationPatchDto;
import com.kahua.assets.entity.Gate;
import com.kahua.assets.entity.GatePresetQuota;
import com.kahua.assets.entity.Operation;
import com.kahua.assets.repository.GatePresetQuotaRepository;
import com.kahua.assets.repository.GateRepository;
import com.kahua.assets.repository.OperationRepository;
import com.kahua.assets.repository.OperationSpecification;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class OperationService {

    private static final UUID DEFAULT_ORG_ID = UUID.fromString("b0000000-0000-0000-0000-000000000002");

    private final OperationRepository operationRepository;
    private final GateRepository gateRepository;
    private final GatePresetQuotaRepository gatePresetQuotaRepository;

    @Transactional
    public Operation createOperation(OperationDto dto) {
        Operation operation = new Operation();
        operation.setName(dto.getName());
        operation.setStartDate(dto.getStartDate());
        operation.setEndDate(dto.getEndDate());
        operation.setLocationName(dto.getLocationName());

        if (dto.getOrgId() != null) {
            operation.setOrgId(dto.getOrgId());
        } else {
            operation.setOrgId(DEFAULT_ORG_ID);
        }

        if (dto.getIsActive() != null) {
            operation.setIsActive(dto.getIsActive());
        }
        if (dto.getTicketPrice() != null) {
            operation.setTicketPrice(dto.getTicketPrice());
        }
        if (dto.getCcFee() != null) {
            operation.setCcFee(dto.getCcFee());
        }
        if (dto.getCcFeeType() != null) {
            operation.setCcFeeType(dto.getCcFeeType());
        }

        String currentUser = "System"; 
        operation.setCreatedBy(currentUser);

        Operation savedOperation = operationRepository.save(operation);

        if (dto.getTemplateOperationId() != null) {
            cloneGatesAndQuotas(dto.getTemplateOperationId(), savedOperation);
        }

        return savedOperation;
    }

    private void cloneGatesAndQuotas(UUID templateOperationId, Operation targetOperation) {
        List<Gate> sourceGates = gateRepository.findByOperation_OperationIdOrderByDisplayOrderAscNameAsc(templateOperationId);
        for (Gate sourceGate : sourceGates) {
            Gate newGate = new Gate();
            newGate.setOperation(targetOperation);
            newGate.setName(sourceGate.getName());
            newGate.setIsActive(sourceGate.getIsActive());
            newGate.setCashBanksRequiredForBusy(sourceGate.getCashBanksRequiredForBusy());
            newGate.setCashBanksRequiredForModerate(sourceGate.getCashBanksRequiredForModerate());
            newGate.setCashBanksRequiredForLite(sourceGate.getCashBanksRequiredForLite());
            newGate.setDisplayOrder(sourceGate.getDisplayOrder());
            newGate.setCreatedBy("Template Clone");
            Gate savedGate = gateRepository.save(newGate);

            List<GatePresetQuota> sourceQuotas = gatePresetQuotaRepository.findByGate_GateId(sourceGate.getGateId());
            for (GatePresetQuota sourceQuota : sourceQuotas) {
                GatePresetQuota newQuota = new GatePresetQuota();
                newQuota.setPresetType(sourceQuota.getPresetType());
                newQuota.setGate(savedGate);
                newQuota.setJobRole(sourceQuota.getJobRole());
                newQuota.setPlannedWorkers(sourceQuota.getPlannedWorkers());
                newQuota.setMinWorkers(sourceQuota.getMinWorkers());
                newQuota.setHappyWorkers(sourceQuota.getHappyWorkers());
                newQuota.setPriority(sourceQuota.getPriority());
                gatePresetQuotaRepository.save(newQuota);
            }
        }
    }

    @Transactional
    public Operation updateOperation(UUID operationId, OperationPatchDto dto) {
        Operation operation = operationRepository.findById(operationId)
                .orElseThrow(() -> new EntityNotFoundException("Operation not found with id: " + operationId));

        if (dto.isDirty("orgId") && dto.getOrgId() != null) {
            operation.setOrgId(dto.getOrgId());
        }
        if (dto.isDirty("name")) {
            operation.setName(dto.getName());
        }
        if (dto.isDirty("startDate")) {
            operation.setStartDate(dto.getStartDate());
        }
        if (dto.isDirty("endDate")) {
            operation.setEndDate(dto.getEndDate());
        }
        if (dto.isDirty("locationName")) {
            operation.setLocationName(dto.getLocationName());
        }
        if (dto.isDirty("isActive")) {
            operation.setIsActive(dto.getIsActive());
        }
        if (dto.isDirty("ticketPrice")) {
            operation.setTicketPrice(dto.getTicketPrice());
        }
        if (dto.isDirty("ccFee")) {
            operation.setCcFee(dto.getCcFee());
        }
        if (dto.isDirty("ccFeeType")) {
            operation.setCcFeeType(dto.getCcFeeType());
        }

        String currentUser = "System";
        operation.setUpdatedBy(currentUser);

        return operationRepository.save(operation);
    }

    @Transactional(readOnly = true)
    public Operation getOperationById(UUID operationId) {
        return operationRepository.findById(operationId).orElse(null);
    }

    @Transactional(readOnly = true)
    public List<Operation> searchOperations(
            UUID operationId, UUID orgId, String name, LocalDate startDate, LocalDate endDate,
            String locationName, Boolean isActive, BigDecimal ticketPrice, BigDecimal ccFee,
            String ccFeeType, String createdBy, Instant createdAt, String updatedBy, Instant updatedAt) {
        Specification<Operation> spec = OperationSpecification.search(
                operationId, orgId, name, startDate, endDate, locationName,
                isActive, ticketPrice, ccFee, ccFeeType, createdBy, createdAt, updatedBy, updatedAt);
        return operationRepository.findAll(spec);
    }
}
