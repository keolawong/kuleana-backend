package com.kuleanasystems.assets.repository;

import com.kuleanasystems.assets.entity.Device;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface DeviceRepository extends JpaRepository<Device, UUID>, JpaSpecificationExecutor<Device> {

    Optional<Device> findByDeviceId(String deviceId);

    Optional<Device> findByDeviceNumber(String deviceNumber);

    boolean existsByDeviceId(String deviceId);

    boolean existsByDeviceNumber(String deviceNumber);

    boolean existsByDeviceIdAndIdNot(String deviceId, UUID id);

    boolean existsByDeviceNumberAndIdNot(String deviceNumber, UUID id);

    List<Device> findByIsActiveTrue();
}
