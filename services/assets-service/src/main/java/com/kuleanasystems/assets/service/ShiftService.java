package com.kuleanasystems.assets.service;

import com.kuleanasystems.assets.entity.Shift;
import com.kuleanasystems.assets.repository.ShiftRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ShiftService {

    private final ShiftRepository shiftRepository;

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

    public Optional<Shift> getShiftById(UUID id) {
        return shiftRepository.findById(id);
    }

    @Transactional
    public Shift saveShift(Shift shift) {
        return shiftRepository.save(shift);
    }
}
