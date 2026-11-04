package com.kahua.assets.controller;

import com.kahua.assets.dto.GateDto;
import com.kahua.assets.dto.GatePatchDto;
import com.kahua.assets.entity.Gate;
import com.kahua.assets.service.GateService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/gates")
@RequiredArgsConstructor
public class GateController {

    private final GateService gateService;

    @PostMapping
    public ResponseEntity<Gate> createGate(@Valid @RequestBody GateDto gateDto) {
        Gate createdGate = gateService.createGate(gateDto);
        return ResponseEntity.created(URI.create("/api/gates/" + createdGate.getGateId())).body(createdGate);
    }

    @PatchMapping("/{gateId}")
    public ResponseEntity<Gate> updateGate(@PathVariable UUID gateId, @RequestBody GatePatchDto gatePatchDto) {
        Gate updatedGate = gateService.updateGate(gateId, gatePatchDto);
        return ResponseEntity.ok(updatedGate);
    }

    @GetMapping
    public ResponseEntity<List<Gate>> getGates(
            @RequestParam(required = false) UUID gateId,
            @RequestParam(required = false) UUID operationId,
            @RequestParam(required = false) String name,
            @RequestParam(required = false) Boolean isActive,
            @RequestParam(required = false) Integer cashBanksRequiredForBusy,
            @RequestParam(required = false) Integer cashBanksRequiredForModerate,
            @RequestParam(required = false) Integer cashBanksRequiredForLite,
            @RequestParam(required = false) Integer displayOrder,
            @RequestParam(required = false) String createdBy,
            @RequestParam(required = false) Instant createdAt,
            @RequestParam(required = false) String updatedBy,
            @RequestParam(required = false) Instant updatedAt,
            @RequestHeader(value = "X-Org-Id", required = false) String orgIdHeader) {

        UUID effectiveOrgId = null;
        if (orgIdHeader != null && !orgIdHeader.trim().isEmpty() && !"all".equalsIgnoreCase(orgIdHeader.trim())) {
            try {
                effectiveOrgId = UUID.fromString(orgIdHeader.trim());
            } catch (IllegalArgumentException ignored) {
            }
        }

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        boolean isSuperAdmin = auth != null && auth.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch(a -> a.equals("ROLE_SUPERADMIN") || a.equals("SUPERADMIN"));

        if ("all".equalsIgnoreCase(orgIdHeader) && isSuperAdmin) {
            effectiveOrgId = null;
        }

        List<Gate> gates = gateService.searchGates(
                gateId, operationId, effectiveOrgId, name, isActive, cashBanksRequiredForBusy, cashBanksRequiredForModerate,
                cashBanksRequiredForLite, displayOrder, createdBy, createdAt, updatedBy, updatedAt);

        if (gates.isEmpty()) {
            return ResponseEntity.noContent().build();
        }

        return ResponseEntity.ok(gates);
    }
}
