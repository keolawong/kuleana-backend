package com.kahua.assets.controller;

import com.kahua.assets.dto.OperationDto;
import com.kahua.assets.dto.OperationPatchDto;
import com.kahua.assets.entity.Operation;
import com.kahua.assets.repository.OperationRepository;
import com.kahua.assets.service.OperationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

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
    private final OperationRepository operationRepository;

    @PostMapping
    public ResponseEntity<Operation> createOperation(
            @Valid @RequestBody OperationDto operationDto,
            @RequestHeader(value = "X-Org-Id", required = false) String orgIdHeader) {
        UUID headerOrgId = null;
        if (orgIdHeader != null && !orgIdHeader.trim().isEmpty() && !"all".equalsIgnoreCase(orgIdHeader.trim())) {
            try {
                headerOrgId = UUID.fromString(orgIdHeader.trim());
            } catch (IllegalArgumentException ignored) {
            }
        }

        if (headerOrgId != null) {
            if (operationDto.getOrgId() != null && !operationDto.getOrgId().equals(headerOrgId)) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Users can only manage items for their single selected organization");
            }
            operationDto.setOrgId(headerOrgId);
        } else if (operationDto.getOrgId() == null) {
            operationDto.setOrgId(UUID.fromString("a0000000-0000-0000-0000-000000000001"));
        }

        Operation createdOperation = operationService.createOperation(operationDto);
        return ResponseEntity.created(URI.create("/api/operations/" + createdOperation.getOperationId())).body(createdOperation);
    }

    @PatchMapping("/{operationId}")
    public ResponseEntity<Operation> updateOperation(
            @PathVariable UUID operationId,
            @RequestBody OperationPatchDto operationPatchDto,
            @RequestHeader(value = "X-Org-Id", required = false) String orgIdHeader) {

        UUID headerOrgId = null;
        if (orgIdHeader != null && !orgIdHeader.trim().isEmpty() && !"all".equalsIgnoreCase(orgIdHeader.trim())) {
            try {
                headerOrgId = UUID.fromString(orgIdHeader.trim());
            } catch (IllegalArgumentException ignored) {
            }
        }

        if (headerOrgId != null) {
            Operation existing = operationRepository.findById(operationId)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Operation not found"));
            if (existing.getOrgId() != null && !existing.getOrgId().equals(headerOrgId)) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Users can only manage items for their single selected organization");
            }
            if (operationPatchDto.getOrgId() != null && !operationPatchDto.getOrgId().equals(headerOrgId)) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Cannot reassign operation outside the single selected organization");
            }
            operationPatchDto.setOrgId(headerOrgId);
        }

        Operation updatedOperation = operationService.updateOperation(operationId, operationPatchDto);
        return ResponseEntity.ok(updatedOperation);
    }

    @GetMapping
    public ResponseEntity<List<Operation>> getOperations(
            @RequestHeader(value = "X-Org-Id", required = false) String orgIdHeader,
            @RequestParam(required = false) UUID orgId,
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

        boolean isSuperAdmin = false;
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getAuthorities() != null) {
            isSuperAdmin = authentication.getAuthorities().stream()
                    .map(GrantedAuthority::getAuthority)
                    .anyMatch(a -> "ROLE_SUPERADMIN".equalsIgnoreCase(a) || "SUPERADMIN".equalsIgnoreCase(a));
        }

        UUID effectiveOrgId = orgId;
        if (effectiveOrgId == null && orgIdHeader != null && !orgIdHeader.trim().isEmpty() && !"all".equalsIgnoreCase(orgIdHeader.trim())) {
            try {
                effectiveOrgId = UUID.fromString(orgIdHeader.trim());
            } catch (IllegalArgumentException ignored) {
            }
        }

        // Only allow unscoped global view if explicitly 'all' header AND superadmin
        if ("all".equalsIgnoreCase(orgIdHeader) && isSuperAdmin) {
            effectiveOrgId = null;
        } else if (effectiveOrgId == null && !isSuperAdmin) {
            effectiveOrgId = UUID.fromString("a0000000-0000-0000-0000-000000000001");
        }

        List<Operation> operations = operationService.searchOperations(
                operationId, effectiveOrgId, name, startDate, endDate, locationName,
                isActive, ticketPrice, ccFee, ccFeeType, createdBy, createdAt, updatedBy, updatedAt);

        if (operations.isEmpty()) {
            return ResponseEntity.noContent().build();
        }

        return ResponseEntity.ok(operations);
    }
}
