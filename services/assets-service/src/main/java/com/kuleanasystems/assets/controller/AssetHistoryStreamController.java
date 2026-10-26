package com.kuleanasystems.assets.controller;

import com.kuleanasystems.assets.service.AssetHistoryStreamingService;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

@RestController
@RequestMapping("/api/asset-history")
public class AssetHistoryStreamController {

    private final AssetHistoryStreamingService assetHistoryStreamingService;

    public AssetHistoryStreamController(AssetHistoryStreamingService assetHistoryStreamingService) {
        this.assetHistoryStreamingService = assetHistoryStreamingService;
    }

    @GetMapping(value = "/search")
    public java.util.List<com.kuleanasystems.assets.dto.AssetSearchDto> searchAssetsForHistory(@RequestParam String search) {
        return assetHistoryStreamingService.searchAssetsForHistory(search);
    }

    @GetMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter streamAssetHistory(@RequestParam UUID assetId,
                                         @RequestParam String assetType,
                                         @RequestParam(required = false) String token,
                                         HttpServletRequest request) {
        SseEmitter emitter = new SseEmitter(180_000L);

        // Resolve token from param, Authorization header, or cookie
        String effectiveToken = token;
        if (effectiveToken == null || effectiveToken.isBlank()) {
            String authHeader = request.getHeader("Authorization");
            if (authHeader != null && authHeader.startsWith("Bearer ")) {
                effectiveToken = authHeader.substring(7);
            } else if (request.getCookies() != null) {
                for (Cookie cookie : request.getCookies()) {
                    if ("jwt".equals(cookie.getName())) {
                        effectiveToken = cookie.getValue();
                        break;
                    }
                }
            }
        }

        final String finalToken = effectiveToken;

        // Send initial connect event immediately to flush headers and confirm connection
        try {
            emitter.send(SseEmitter.event()
                    .name("OPEN")
                    .data(Map.of("status", "connected", "assetId", assetId)));
        } catch (IOException e) {
            emitter.completeWithError(e);
            return emitter;
        }

        CompletableFuture.runAsync(() -> {
            try {
                assetHistoryStreamingService.streamHistory(assetId, assetType, finalToken, emitter);
            } catch (Exception e) {
                try {
                    emitter.completeWithError(e);
                } catch (Exception ignored) {
                }
            }
        });

        return emitter;
    }
}
