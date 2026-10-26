package com.kuleanasystems.assets.service;

import com.kuleanasystems.assets.dto.DeviceDto;
import com.kuleanasystems.assets.dto.DevicePatchDto;
import com.kuleanasystems.assets.entity.Device;
import com.kuleanasystems.assets.repository.CashBankRepository;
import com.kuleanasystems.assets.repository.CreditReportRepository;
import com.kuleanasystems.assets.repository.DeviceRepository;
import com.kuleanasystems.assets.repository.DeviceSpecification;
import com.kuleanasystems.assets.repository.TicketBookRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class DeviceService {

    private final DeviceRepository deviceRepository;
    private final CashBankRepository cashBankRepository;
    private final TicketBookRepository ticketBookRepository;
    private final CreditReportRepository creditReportRepository;

    @Transactional
    public Device createDevice(DeviceDto dto) {
        if (dto.getDeviceId() == null || dto.getDeviceId().trim().isEmpty()) {
            throw new IllegalArgumentException("Device ID cannot be empty");
        }
        if (dto.getDeviceNumber() == null || dto.getDeviceNumber().trim().isEmpty()) {
            throw new IllegalArgumentException("Device number cannot be empty");
        }

        String trimmedDeviceId = dto.getDeviceId().trim();
        String trimmedDeviceNumber = dto.getDeviceNumber().trim();

        if (deviceRepository.existsByDeviceId(trimmedDeviceId)) {
            throw new IllegalArgumentException("Device ID already exists: " + trimmedDeviceId);
        }
        if (deviceRepository.existsByDeviceNumber(trimmedDeviceNumber)) {
            throw new IllegalArgumentException("Device number already exists: " + trimmedDeviceNumber);
        }

        Device device = new Device();
        device.setDeviceId(trimmedDeviceId);
        device.setDeviceNumber(trimmedDeviceNumber);
        device.setIsActive(dto.getIsActive() != null ? dto.getIsActive() : true);
        device.setCreatedBy(dto.getCreatedBy() != null ? dto.getCreatedBy() : "System");

        return deviceRepository.save(device);
    }

    @Transactional
    public List<Device> bulkCreateDevices(List<DeviceDto> dtos) {
        if (dtos == null || dtos.isEmpty()) {
            return java.util.Collections.emptyList();
        }

        java.util.Set<String> seenDeviceIds = new java.util.HashSet<>();
        java.util.Set<String> seenDeviceNumbers = new java.util.HashSet<>();
        List<Device> devicesToSave = new java.util.ArrayList<>();

        for (int i = 0; i < dtos.size(); i++) {
            DeviceDto dto = dtos.get(i);
            if (dto.getDeviceId() == null || dto.getDeviceId().trim().isEmpty()) {
                throw new IllegalArgumentException("Device ID cannot be empty at index " + i);
            }
            if (dto.getDeviceNumber() == null || dto.getDeviceNumber().trim().isEmpty()) {
                throw new IllegalArgumentException("Device number cannot be empty at index " + i);
            }

            String trimmedDeviceId = dto.getDeviceId().trim();
            String trimmedDeviceNumber = dto.getDeviceNumber().trim();

            if (!seenDeviceIds.add(trimmedDeviceId.toLowerCase())) {
                throw new IllegalArgumentException("Duplicate Device ID in bulk request: " + trimmedDeviceId);
            }
            if (!seenDeviceNumbers.add(trimmedDeviceNumber.toLowerCase())) {
                throw new IllegalArgumentException("Duplicate Device number in bulk request: " + trimmedDeviceNumber);
            }

            if (deviceRepository.existsByDeviceId(trimmedDeviceId)) {
                throw new IllegalArgumentException("Device ID already exists: " + trimmedDeviceId);
            }
            if (deviceRepository.existsByDeviceNumber(trimmedDeviceNumber)) {
                throw new IllegalArgumentException("Device number already exists: " + trimmedDeviceNumber);
            }

            Device device = new Device();
            device.setDeviceId(trimmedDeviceId);
            device.setDeviceNumber(trimmedDeviceNumber);
            device.setIsActive(dto.getIsActive() != null ? dto.getIsActive() : true);
            device.setCreatedBy(dto.getCreatedBy() != null ? dto.getCreatedBy() : "System");
            devicesToSave.add(device);
        }

        return deviceRepository.saveAll(devicesToSave);
    }

    @Transactional
    public Device updateDevice(UUID id, DevicePatchDto patch) {
        Device device = deviceRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Device not found with id: " + id));

        String oldDeviceId = device.getDeviceId();
        boolean deviceIdChanged = false;

        if (patch.isDirty("deviceId") && patch.getDeviceId() != null) {
            String newDeviceId = patch.getDeviceId().trim();
            if (!newDeviceId.equalsIgnoreCase(oldDeviceId)) {
                if (deviceRepository.existsByDeviceIdAndIdNot(newDeviceId, id)) {
                    throw new IllegalArgumentException("Device ID already exists: " + newDeviceId);
                }
                device.setDeviceId(newDeviceId);
                deviceIdChanged = true;
            }
        }

        if (patch.isDirty("deviceNumber") && patch.getDeviceNumber() != null) {
            String newDeviceNumber = patch.getDeviceNumber().trim();
            if (!newDeviceNumber.equalsIgnoreCase(device.getDeviceNumber())) {
                if (deviceRepository.existsByDeviceNumberAndIdNot(newDeviceNumber, id)) {
                    throw new IllegalArgumentException("Device number already exists: " + newDeviceNumber);
                }
                device.setDeviceNumber(newDeviceNumber);
            }
        }

        if (patch.isDirty("isActive") && patch.getIsActive() != null) {
            device.setIsActive(patch.getIsActive());
        }

        if (deviceIdChanged && oldDeviceId != null) {
            cashBankRepository.updateDeviceId(oldDeviceId, device.getDeviceId());
            ticketBookRepository.updateDeviceId(oldDeviceId, device.getDeviceId());
            creditReportRepository.updateDeviceId(oldDeviceId, device.getDeviceId());
        }

        device.setUpdatedBy("System");

        return deviceRepository.save(device);
    }

    @Transactional(readOnly = true)
    public List<Device> searchDevices(String search, Boolean isActive, String sortBy, String sortDir) {
        Specification<Device> spec = DeviceSpecification.search(search, isActive);

        Sort.Direction direction = "desc".equalsIgnoreCase(sortDir) ? Sort.Direction.DESC : Sort.Direction.ASC;
        String sortProperty = "createdAt".equalsIgnoreCase(sortBy) ? "createdAt" : "deviceNumber";
        Sort sort = Sort.by(direction, sortProperty);

        return deviceRepository.findAll(spec, sort);
    }

    @Transactional(readOnly = true)
    public List<Device> searchDevices(String search, Boolean isActive) {
        return searchDevices(search, isActive, null, null);
    }

    @Transactional(readOnly = true)
    public Device getDeviceById(UUID id) {
        return deviceRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Device not found with id: " + id));
    }

    @Transactional
    public void deleteDevice(UUID id) {
        Device device = deviceRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Device not found with id: " + id));
        deviceRepository.delete(device);
    }

    public List<Device> getActiveDevices() {
        return deviceRepository.findByIsActiveTrue();
    }

    public java.util.Optional<Device> getDeviceByDeviceId(String deviceId) {
        return deviceRepository.findByDeviceId(deviceId);
    }
}
