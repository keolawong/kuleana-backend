package com.kahua.assets.service;

import com.kahua.assets.dto.OperationDto;
import com.kahua.assets.dto.OperationPatchDto;
import com.kahua.assets.entity.Operation;
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

    private static final UUID DEFAULT_ORG_ID = UUID.fromString("a0000000-0000-0000-0000-000000000001");

    private final OperationRepository operationRepository;

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

        // Normally you would fetch this from the SecurityContext
        String currentUser = "System"; 
        operation.setCreatedBy(currentUser);

        return operationRepository.save(operation);
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

        // Normally you would fetch this from the SecurityContext
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
