package com.kahua.assets.controller;

import com.kahua.assets.dto.AssetSearchResponseDto;
import com.kahua.assets.entity.Operation;
import com.kahua.assets.service.AssetService;
import com.kahua.assets.service.OperationService;
import com.kuleanasystems.security.TenantSecurityUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.UUID;

@RestController
@RequestMapping("/api/assets")
@RequiredArgsConstructor
public class AssetController {

    private final AssetService assetService;
    private final OperationService operationService;

    @GetMapping("/{operationId}")
    public ResponseEntity<AssetSearchResponseDto> searchAssets(
            @PathVariable UUID operationId,
            @RequestParam(required = false) String search) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        Operation op = operationService.getOperationById(operationId);
        if (op != null && op.getOrgId() != null && !TenantSecurityUtil.isMemberOf(auth, op.getOrgId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied to operation's assets");
        }

        AssetSearchResponseDto assets = assetService.searchAssets(operationId, search);

        if ((assets.getCashBank() == null || assets.getCashBank().isEmpty()) &&
            (assets.getTicketBook() == null || assets.getTicketBook().isEmpty()) &&
            (assets.getDevice() == null || assets.getDevice().isEmpty())) {
            return ResponseEntity.noContent().build();
        }

        return ResponseEntity.ok(assets);
    }
}