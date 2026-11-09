package com.kahua.assets.controller;

import com.kahua.assets.dto.DeviceDto;
import com.kahua.assets.dto.DevicePatchDto;
import com.kahua.assets.entity.Device;
import com.kahua.assets.service.DeviceService;
import com.kuleanasystems.security.TenantSecurityUtil;
import jakarta.persistence.EntityNotFoundException;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/devices")
@RequiredArgsConstructor
public class DeviceController {

    private final DeviceService deviceService;

    @PostMapping
    public ResponseEntity<Device> createDevice(
            @Valid @RequestBody DeviceDto deviceDto,
            @RequestHeader(value = "X-Org-Id", required = false) String orgIdHeader) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String targetOrgStr = deviceDto.getOrgId() != null ? deviceDto.getOrgId().toString() : orgIdHeader;
        UUID effectiveOrgId = TenantSecurityUtil.resolveEffectiveOrgId(targetOrgStr, auth);
        if (effectiveOrgId == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Organization ID is required to create a device");
        }
        deviceDto.setOrgId(effectiveOrgId);

        Device createdDevice = deviceService.createDevice(deviceDto);
        return ResponseEntity.created(URI.create("/api/devices/" + createdDevice.getId())).body(createdDevice);
    }

    @PostMapping("/bulk")
    public ResponseEntity<List<Device>> bulkCreateDevices(
            @Valid @RequestBody List<DeviceDto> dtos,
            @RequestHeader(value = "X-Org-Id", required = false) String orgIdHeader) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        UUID effectiveOrgId = TenantSecurityUtil.resolveEffectiveOrgId(orgIdHeader, auth);
        if (effectiveOrgId == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Organization ID is required to bulk create devices");
        }

        for (DeviceDto dto : dtos) {
            if (dto.getOrgId() == null) {
                dto.setOrgId(effectiveOrgId);
            } else if (!TenantSecurityUtil.isMemberOf(auth, dto.getOrgId())) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied to org: " + dto.getOrgId());
            }
        }

        List<Device> createdDevices = deviceService.bulkCreateDevices(dtos);
        return ResponseEntity.ok(createdDevices);
    }

    @GetMapping
    public ResponseEntity<List<Device>> getDevices(
            @RequestHeader(value = "X-Org-Id", required = false) String orgIdHeader,
            @RequestParam(required = false) UUID orgId,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) Boolean isActive,
            @RequestParam(required = false) String sortBy,
            @RequestParam(required = false) String sortDir) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String targetOrgStr = (orgId != null) ? orgId.toString() : orgIdHeader;
        UUID effectiveOrgId = TenantSecurityUtil.resolveEffectiveOrgId(targetOrgStr, auth);

        List<Device> devices = deviceService.searchDevices(search, isActive, effectiveOrgId, sortBy, sortDir);
        if (devices.isEmpty()) {
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.ok(devices);
    }

    @GetMapping("/{id}")
    public ResponseEntity<Device> getDeviceById(@PathVariable UUID id) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        Device device = deviceService.getDeviceById(id);
        if (device.getOrgId() != null && !TenantSecurityUtil.isMemberOf(auth, device.getOrgId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied to device in another organization");
        }
        return ResponseEntity.ok(device);
    }

    @PatchMapping("/{id}")
    public ResponseEntity<Device> updateDevice(@PathVariable UUID id, @RequestBody DevicePatchDto patchDto) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        Device existing = deviceService.getDeviceById(id);
        if (existing.getOrgId() != null && !TenantSecurityUtil.isMemberOf(auth, existing.getOrgId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied to device in another organization");
        }
        Device updatedDevice = deviceService.updateDevice(id, patchDto);
        return ResponseEntity.ok(updatedDevice);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteDevice(@PathVariable UUID id) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        Device existing = deviceService.getDeviceById(id);
        if (existing.getOrgId() != null && !TenantSecurityUtil.isMemberOf(auth, existing.getOrgId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied to device in another organization");
        }
        deviceService.deleteDevice(id);
        return ResponseEntity.noContent().build();
    }

    @ExceptionHandler(EntityNotFoundException.class)
    public ResponseEntity<Map<String, String>> handleEntityNotFound(EntityNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", ex.getMessage()));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, String>> handleIllegalArgument(IllegalArgumentException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", ex.getMessage()));
    }
}
