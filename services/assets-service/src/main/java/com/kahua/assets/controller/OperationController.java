package com.kahua.assets.controller;

import com.kahua.assets.dto.OperationDto;
import com.kahua.assets.dto.OperationPatchDto;
import com.kahua.assets.entity.Operation;
import com.kahua.assets.repository.OperationRepository;
import com.kahua.assets.service.OperationService;
import com.kuleanasystems.security.TenantSecurityUtil;
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
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String targetOrgStr = operationDto.getOrgId() != null ? operationDto.getOrgId().toString() : orgIdHeader;
        UUID effectiveOrgId = TenantSecurityUtil.resolveEffectiveOrgId(targetOrgStr, auth);

        if (effectiveOrgId == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Organization ID is required to create an operation");
        }

        operationDto.setOrgId(effectiveOrgId);
        Operation createdOperation = operationService.createOperation(operationDto);
        return ResponseEntity.created(URI.create("/api/operations/" + createdOperation.getOperationId())).body(createdOperation);
    }

    @PatchMapping("/{operationId}")
    public ResponseEntity<Operation> updateOperation(
            @PathVariable UUID operationId,
            @RequestBody OperationPatchDto operationPatchDto,
            @RequestHeader(value = "X-Org-Id", required = false) String orgIdHeader) {

        Operation existing = operationRepository.findById(operationId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Operation not found"));

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (existing.getOrgId() != null && !TenantSecurityUtil.isMemberOf(auth, existing.getOrgId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied to operation's organization");
        }

        if (operationPatchDto.getOrgId() != null && !TenantSecurityUtil.isMemberOf(auth, operationPatchDto.getOrgId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied to target organization");
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

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String targetOrgStr = (orgId != null) ? orgId.toString() : orgIdHeader;
        UUID effectiveOrgId = TenantSecurityUtil.resolveEffectiveOrgId(targetOrgStr, auth);

        List<Operation> operations = operationService.searchOperations(
                operationId, effectiveOrgId, name, startDate, endDate, locationName,
                isActive, ticketPrice, ccFee, ccFeeType, createdBy, createdAt, updatedBy, updatedAt);

        if (operations.isEmpty()) {
            return ResponseEntity.noContent().build();
        }

        return ResponseEntity.ok(operations);
    }
}
