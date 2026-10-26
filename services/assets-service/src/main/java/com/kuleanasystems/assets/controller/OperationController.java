package com.kuleanasystems.assets.controller;

import com.kuleanasystems.assets.dto.OperationDto;
import com.kuleanasystems.assets.dto.OperationPatchDto;
import com.kuleanasystems.assets.entity.Operation;
import com.kuleanasystems.assets.service.OperationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.net.URI;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/operations")
@RequiredArgsConstructor
public class OperationController {

    private final OperationService operationService;

    @PostMapping
    public ResponseEntity<Operation> createOperation(@Valid @RequestBody OperationDto operationDto) {
        Operation createdOperation = operationService.createOperation(operationDto);
        return ResponseEntity.created(URI.create("/api/operations/" + createdOperation.getOperationId())).body(createdOperation);
    }

    @PatchMapping("/{operationId}")
    public ResponseEntity<Operation> updateOperation(@PathVariable UUID operationId, @RequestBody OperationPatchDto operationPatchDto) {
        Operation updatedOperation = operationService.updateOperation(operationId, operationPatchDto);
        return ResponseEntity.ok(updatedOperation);
    }

    @GetMapping
    public ResponseEntity<List<Operation>> getOperations(
            @RequestParam(required = false) UUID operationId,
            @RequestParam(required = false) String name,
            @RequestParam(required = false) LocalDate startDate,
            @RequestParam(required = false) LocalDate endDate,
            @RequestParam(required = false) String locationName,
            @RequestParam(required = false) Boolean isActive,
            @RequestParam(required = false) BigDecimal ticketPrice,
            @RequestParam(required = false) BigDecimal ccFee,
            @RequestParam(required = false) String ccFeeType,
            @RequestParam(required = false) String createdBy,
            @RequestParam(required = false) Instant createdAt,
            @RequestParam(required = false) String updatedBy,
            @RequestParam(required = false) Instant updatedAt) {

        List<Operation> operations = operationService.searchOperations(
                operationId, name, startDate, endDate, locationName,
                isActive, ticketPrice, ccFee, ccFeeType, createdBy, createdAt, updatedBy, updatedAt);

        if (operations.isEmpty()) {
            return ResponseEntity.noContent().build();
        }

        return ResponseEntity.ok(operations);
    }

    @GetMapping("/{id}")
    public ResponseEntity<Operation> getOperationById(@PathVariable UUID id) {
        return operationService.getOperationById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
}
