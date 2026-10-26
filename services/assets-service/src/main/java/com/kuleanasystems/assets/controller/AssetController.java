package com.kuleanasystems.assets.controller;

import com.kuleanasystems.assets.dto.AssetSearchResponseDto;
import com.kuleanasystems.assets.service.AssetService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/assets")
@RequiredArgsConstructor
public class AssetController {

    private final AssetService assetService;

    @GetMapping("/{operationId}")
    public ResponseEntity<AssetSearchResponseDto> searchAssets(
            @PathVariable UUID operationId,
            @RequestParam(required = false) String search) {
        AssetSearchResponseDto assets = assetService.searchAssets(operationId, search);

        if ((assets.getCashBank() == null || assets.getCashBank().isEmpty()) &&
            (assets.getTicketBook() == null || assets.getTicketBook().isEmpty()) &&
            (assets.getDevice() == null || assets.getDevice().isEmpty())) {
            return ResponseEntity.noContent().build();
        }

        return ResponseEntity.ok(assets);
    }
}