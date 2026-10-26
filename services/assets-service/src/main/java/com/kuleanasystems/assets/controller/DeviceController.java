package com.kuleanasystems.assets.controller;

import com.kuleanasystems.assets.dto.DeviceDto;
import com.kuleanasystems.assets.dto.DevicePatchDto;
import com.kuleanasystems.assets.entity.Device;
import com.kuleanasystems.assets.service.DeviceService;
import jakarta.persistence.EntityNotFoundException;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

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
    public ResponseEntity<Device> createDevice(@Valid @RequestBody DeviceDto deviceDto) {
        Device createdDevice = deviceService.createDevice(deviceDto);
        return ResponseEntity.created(URI.create("/api/devices/" + createdDevice.getId())).body(createdDevice);
    }

    @PostMapping("/bulk")
    public ResponseEntity<List<Device>> bulkCreateDevices(@Valid @RequestBody List<DeviceDto> dtos) {
        List<Device> createdDevices = deviceService.bulkCreateDevices(dtos);
        return ResponseEntity.ok(createdDevices);
    }

    @GetMapping
    public ResponseEntity<List<Device>> getDevices(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) Boolean isActive,
            @RequestParam(required = false) String sortBy,
            @RequestParam(required = false) String sortDir) {
        List<Device> devices = deviceService.searchDevices(search, isActive, sortBy, sortDir);
        if (devices.isEmpty()) {
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.ok(devices);
    }

    @GetMapping("/{id}")
    public ResponseEntity<Device> getDeviceById(@PathVariable UUID id) {
        Device device = deviceService.getDeviceById(id);
        return ResponseEntity.ok(device);
    }

    @PatchMapping("/{id}")
    public ResponseEntity<Device> updateDevice(@PathVariable UUID id, @RequestBody DevicePatchDto patchDto) {
        Device updatedDevice = deviceService.updateDevice(id, patchDto);
        return ResponseEntity.ok(updatedDevice);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteDevice(@PathVariable UUID id) {
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
