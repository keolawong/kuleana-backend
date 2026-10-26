package com.kuleanasystems.assets.controller;

import com.kuleanasystems.assets.entity.Device;
import com.kuleanasystems.assets.service.DeviceService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/devices")
@RequiredArgsConstructor
public class DeviceController {

    private final DeviceService deviceService;

    @GetMapping
    public ResponseEntity<List<Device>> getDevices() {
        List<Device> devices = deviceService.getActiveDevices();
        return ResponseEntity.ok(devices);
    }
}
