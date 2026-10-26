package com.kuleanasystems.assets.service;

import com.kuleanasystems.assets.entity.Operation;
import com.kuleanasystems.assets.repository.OperationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class OperationService {

    private final OperationRepository operationRepository;

    public List<Operation> getAllOperations(Boolean isActive) {
        if (Boolean.TRUE.equals(isActive)) {
            return operationRepository.findByIsActiveTrue();
        }
        return operationRepository.findAllByOrderByStartDateDesc();
    }

    public Optional<Operation> getOperationById(UUID id) {
        return operationRepository.findById(id);
    }

    @Transactional
    public Operation saveOperation(Operation operation) {
        return operationRepository.save(operation);
    }
}
