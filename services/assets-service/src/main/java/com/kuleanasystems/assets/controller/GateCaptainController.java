package com.kuleanasystems.assets.controller;

import com.kuleanasystems.assets.dto.GateCaptainDto;
import com.kuleanasystems.assets.dto.GateCaptainPatchDto;
import com.kuleanasystems.assets.entity.GateCaptain;
import com.kuleanasystems.assets.service.GateCaptainService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/gate-captains")
@RequiredArgsConstructor
public class GateCaptainController {

    private final GateCaptainService gateCaptainService;

    @PostMapping
    public ResponseEntity<GateCaptain> createGateCaptain(@Valid @RequestBody GateCaptainDto gateCaptainDto) {
        GateCaptain createdGateCaptain = gateCaptainService.createGateCaptain(gateCaptainDto);
        return ResponseEntity.created(URI.create("/api/gate-captains/" + createdGateCaptain.getGateCaptainId())).body(createdGateCaptain);
    }

    @PatchMapping("/{gateCaptainId}")
    public ResponseEntity<GateCaptain> updateGateCaptain(@PathVariable UUID gateCaptainId, @RequestBody GateCaptainPatchDto gateCaptainPatchDto) {
        GateCaptain updatedGateCaptain = gateCaptainService.updateGateCaptain(gateCaptainId, gateCaptainPatchDto);
        return ResponseEntity.ok(updatedGateCaptain);
    }

    @GetMapping
    public ResponseEntity<List<GateCaptain>> getGateCaptains(
            @RequestParam(required = false) UUID gateCaptainId,
            @RequestParam(required = false) String captainName,
            @RequestParam(required = false) UUID gateId,
            @RequestParam(required = false) UUID operationId,
            @RequestParam(required = false) UUID shiftId,
            @RequestParam(required = false) String createdBy,
            @RequestParam(required = false) Instant createdAt,
            @RequestParam(required = false) String updatedBy,
            @RequestParam(required = false) Instant updatedAt) {

        List<GateCaptain> gateCaptains = gateCaptainService.searchGateCaptains(
                gateCaptainId, captainName, gateId, operationId, shiftId,
                createdBy, createdAt, updatedBy, updatedAt);

        if (gateCaptains.isEmpty()) {
            return ResponseEntity.noContent().build();
        }

        return ResponseEntity.ok(gateCaptains);
    }
}