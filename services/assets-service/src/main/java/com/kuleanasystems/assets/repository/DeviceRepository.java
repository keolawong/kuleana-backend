package com.kuleanasystems.assets.repository;

import com.kuleanasystems.assets.entity.Device;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface DeviceRepository extends JpaRepository<Device, UUID> {
    List<Device> findByIsActiveTrue();
    Optional<Device> findByDeviceId(String deviceId);
}
