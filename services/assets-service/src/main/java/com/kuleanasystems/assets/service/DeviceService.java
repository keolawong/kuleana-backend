package com.kuleanasystems.assets.service;

import com.kuleanasystems.assets.entity.Device;
import com.kuleanasystems.assets.repository.DeviceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DeviceService {

    private final DeviceRepository deviceRepository;

    public List<Device> getActiveDevices() {
        return deviceRepository.findByIsActiveTrue();
    }

    public Optional<Device> getDeviceByDeviceId(String deviceId) {
        return deviceRepository.findByDeviceId(deviceId);
    }
}
