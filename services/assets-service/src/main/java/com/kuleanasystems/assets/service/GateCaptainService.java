package com.kuleanasystems.assets.service;

import com.kuleanasystems.assets.dto.GateCaptainDto;
import com.kuleanasystems.assets.dto.GateCaptainPatchDto;
import com.kuleanasystems.assets.entity.Gate;
import com.kuleanasystems.assets.entity.GateCaptain;
import com.kuleanasystems.assets.entity.Operation;
import com.kuleanasystems.assets.entity.Shift;
import com.kuleanasystems.assets.repository.GateCaptainRepository;
import com.kuleanasystems.assets.repository.GateCaptainSpecification;
import com.kuleanasystems.assets.repository.GateRepository;
import com.kuleanasystems.assets.repository.OperationRepository;
import com.kuleanasystems.assets.repository.ShiftRepository;
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
public class GateCaptainService {

    private final GateCaptainRepository gateCaptainRepository;
    private final GateRepository gateRepository;
    private final OperationRepository operationRepository;
    private final ShiftRepository shiftRepository;

    @Transactional
    public GateCaptain createGateCaptain(GateCaptainDto dto) {
        Gate gate = gateRepository.findById(dto.getGateId())
                .orElseThrow(() -> new EntityNotFoundException("Gate not found with id: " + dto.getGateId()));
        Operation operation = operationRepository.findById(dto.getOperationId())
                .orElseThrow(() -> new EntityNotFoundException("Operation not found with id: " + dto.getOperationId()));
        Shift shift = shiftRepository.findById(dto.getShiftId())
                .orElseThrow(() -> new EntityNotFoundException("Shift not found with id: " + dto.getShiftId()));

        GateCaptain gateCaptain = new GateCaptain();
        gateCaptain.setCaptainName(dto.getCaptainName());
        gateCaptain.setGate(gate);
        gateCaptain.setOperation(operation);
        gateCaptain.setShift(shift);

        String currentUser = "System";
        gateCaptain.setCreatedBy(currentUser);

        return gateCaptainRepository.save(gateCaptain);
    }

    @Transactional
    public GateCaptain updateGateCaptain(UUID gateCaptainId, GateCaptainPatchDto dto) {
        GateCaptain gateCaptain = gateCaptainRepository.findById(gateCaptainId)
                .orElseThrow(() -> new EntityNotFoundException("GateCaptain not found with id: " + gateCaptainId));

        if (dto.isDirty("captainName")) {
            gateCaptain.setCaptainName(dto.getCaptainName());
        }
        if (dto.isDirty("gateId")) {
            if (dto.getGateId() == null) {
                gateCaptain.setGate(null);
            } else {
                Gate gate = gateRepository.findById(dto.getGateId())
                        .orElseThrow(() -> new EntityNotFoundException("Gate not found with id: " + dto.getGateId()));
                gateCaptain.setGate(gate);
            }
        }
        if (dto.isDirty("operationId")) {
            if (dto.getOperationId() == null) {
                gateCaptain.setOperation(null);
            } else {
                Operation operation = operationRepository.findById(dto.getOperationId())
                        .orElseThrow(() -> new EntityNotFoundException("Operation not found with id: " + dto.getOperationId()));
                gateCaptain.setOperation(operation);
            }
        }
        if (dto.isDirty("shiftId")) {
            if (dto.getShiftId() == null) {
                gateCaptain.setShift(null);
            } else {
                Shift shift = shiftRepository.findById(dto.getShiftId())
                        .orElseThrow(() -> new EntityNotFoundException("Shift not found with id: " + dto.getShiftId()));
                gateCaptain.setShift(shift);
            }
        }

        String currentUser = "System";
        gateCaptain.setUpdatedBy(currentUser);

        return gateCaptainRepository.save(gateCaptain);
    }

    @Transactional(readOnly = true)
    public List<GateCaptain> searchGateCaptains(
            UUID gateCaptainId, String captainName, UUID gateId, UUID operationId, UUID shiftId,
            String createdBy, Instant createdAt, String updatedBy, Instant updatedAt) {
        Specification<GateCaptain> spec = GateCaptainSpecification.search(
                gateCaptainId, captainName, gateId, operationId, shiftId,
                createdBy, createdAt, updatedBy, updatedAt);
        return gateCaptainRepository.findAll(spec);
    }
}