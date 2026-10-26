package com.kuleanasystems.assets.service;

import com.kuleanasystems.assets.dto.ShiftDto;
import com.kuleanasystems.assets.dto.ShiftPatchDto;
import com.kuleanasystems.assets.entity.Operation;
import com.kuleanasystems.assets.entity.Shift;
import com.kuleanasystems.assets.repository.OperationRepository;
import com.kuleanasystems.assets.repository.ShiftRepository;
import com.kuleanasystems.assets.repository.ShiftSpecification;
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
public class ShiftService {

    private final ShiftRepository shiftRepository;
    private final OperationRepository operationRepository;

    @Transactional
    public Shift createShift(ShiftDto dto) {
        Operation operation = operationRepository.findById(dto.getOperationId())
                .orElseThrow(() -> new EntityNotFoundException("Operation not found with id: " + dto.getOperationId()));

        Shift shift = new Shift();
        shift.setOperation(operation);
        shift.setName(dto.getName());
        shift.setStartTime(dto.getStartTime());
        shift.setEndTime(dto.getEndTime());
        shift.setTraffic(dto.getTraffic());
        shift.setStatus(dto.getStatus());

        String currentUser = "System";
        shift.setCreatedBy(currentUser);

        return shiftRepository.save(shift);
    }

    @Transactional
    public Shift updateShift(UUID shiftId, ShiftPatchDto dto) {
        Shift shift = shiftRepository.findById(shiftId)
                .orElseThrow(() -> new EntityNotFoundException("Shift not found with id: " + shiftId));

        if (dto.isDirty("operationId")) {
            if (dto.getOperationId() == null) {
                shift.setOperation(null);
            } else {
                Operation operation = operationRepository.findById(dto.getOperationId())
                        .orElseThrow(() -> new EntityNotFoundException("Operation not found with id: " + dto.getOperationId()));
                shift.setOperation(operation);
            }
        }
        if (dto.isDirty("name")) {
            shift.setName(dto.getName());
        }
        if (dto.isDirty("startTime")) {
            shift.setStartTime(dto.getStartTime());
        }
        if (dto.isDirty("endTime")) {
            shift.setEndTime(dto.getEndTime());
        }
        if (dto.isDirty("traffic")) {
            shift.setTraffic(dto.getTraffic());
        }
        if (dto.isDirty("status")) {
            shift.setStatus(dto.getStatus());
        }

        String currentUser = "System";
        shift.setUpdatedBy(currentUser);

        return shiftRepository.save(shift);
    }

    @Transactional(readOnly = true)
    public List<Shift> searchShifts(
            UUID shiftId, UUID operationId, String name, Instant startTime, Instant endTime,
            String traffic, String status, String createdBy, Instant createdAt,
            String updatedBy, Instant updatedAt, Boolean activeOperation) {
        Specification<Shift> spec = ShiftSpecification.search(
                shiftId, operationId, name, startTime, endTime, traffic,
                status, createdBy, createdAt, updatedBy, updatedAt, activeOperation);
        return shiftRepository.findAll(spec);
    }

    public List<Shift> getShifts(UUID operationId, String status) {
        if (operationId != null && status != null) {
            return shiftRepository.findByOperationOperationIdAndStatus(operationId, status);
        } else if (operationId != null) {
            return shiftRepository.findByOperationOperationId(operationId);
        } else if (status != null) {
            return shiftRepository.findByStatus(status);
        }
        return shiftRepository.findAll();
    }

    public java.util.Optional<Shift> getShiftById(UUID id) {
        return shiftRepository.findById(id);
    }

    @Transactional
    public Shift saveShift(Shift shift) {
        return shiftRepository.save(shift);
    }
}
