package com.kuleanasystems.assets.controller;

import com.kuleanasystems.assets.dto.ActiveCheckinSessionDto;
import com.kuleanasystems.assets.service.ActiveCheckinSessionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping({"/api/check-in/sessions", "/check-in/sessions"})
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('VIEWER', 'viewer', 'EDITOR', 'editor', 'ADMIN', 'admin', 'ASSET-READ', 'asset-read', 'ASSET-WRITE', 'asset-write', 'PAGE_/RECONCILE', 'PAGE_/VIEWER', 'PAGE_/reconcile', 'PAGE_/viewer')")
public class ActiveCheckinSessionController {

    private final ActiveCheckinSessionService activeCheckinSessionService;

    @PostMapping("/heartbeat")
    @PreAuthorize("hasAnyRole('VIEWER', 'viewer', 'EDITOR', 'editor', 'ADMIN', 'admin', 'ASSET-READ', 'asset-read', 'ASSET-WRITE', 'asset-write', 'PAGE_/RECONCILE', 'PAGE_/VIEWER', 'PAGE_/reconcile', 'PAGE_/viewer')")
    public ResponseEntity<ActiveCheckinSessionDto> heartbeat(@RequestBody ActiveCheckinSessionDto dto) {
        return ResponseEntity.ok(activeCheckinSessionService.recordHeartbeat(dto));
    }

    @GetMapping("/active")
    @PreAuthorize("hasAnyRole('VIEWER', 'viewer', 'EDITOR', 'editor', 'ADMIN', 'admin', 'ASSET-READ', 'asset-read', 'ASSET-WRITE', 'asset-write', 'PAGE_/RECONCILE', 'PAGE_/VIEWER', 'PAGE_/reconcile', 'PAGE_/viewer')")
    public ResponseEntity<List<ActiveCheckinSessionDto>> getActiveSessions() {
        return ResponseEntity.ok(activeCheckinSessionService.getActiveSessions());
    }

    @PostMapping("/close")
    @PreAuthorize("hasAnyRole('VIEWER', 'viewer', 'EDITOR', 'editor', 'ADMIN', 'admin', 'ASSET-READ', 'asset-read', 'ASSET-WRITE', 'asset-write', 'PAGE_/RECONCILE', 'PAGE_/VIEWER', 'PAGE_/reconcile', 'PAGE_/viewer')")
    public ResponseEntity<Void> closeSession(
            @RequestParam(required = false) String username,
            @RequestBody(required = false) Map<String, Object> body) {
        String targetUsername = username;
        if ((targetUsername == null || targetUsername.isBlank()) && body != null && body.get("username") != null) {
            targetUsername = body.get("username").toString();
        }
        if (targetUsername != null && !targetUsername.isBlank()) {
            activeCheckinSessionService.closeSession(targetUsername);
        }
        return ResponseEntity.noContent().build();
    }
}
