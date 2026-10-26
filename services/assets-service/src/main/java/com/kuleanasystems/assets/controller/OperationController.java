package com.kuleanasystems.assets.controller;

import com.kuleanasystems.assets.entity.Operation;
import com.kuleanasystems.assets.service.OperationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/operations")
@RequiredArgsConstructor
public class OperationController {

    private final OperationService operationService;

    @GetMapping
    public ResponseEntity<List<Operation>> getOperations(@RequestParam(required = false) Boolean isActive) {
        List<Operation> operations = operationService.getAllOperations(isActive);
        return ResponseEntity.ok(operations);
    }

    @GetMapping("/{id}")
    public ResponseEntity<Operation> getOperationById(@PathVariable UUID id) {
        return operationService.getOperationById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<Operation> createOperation(@RequestBody Operation operation) {
        Operation saved = operationService.saveOperation(operation);
        return ResponseEntity.ok(saved);
    }
}
