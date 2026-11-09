package com.kahua.assets.repository;

import com.kahua.assets.entity.Device;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface DeviceRepository extends JpaRepository<Device, UUID>, JpaSpecificationExecutor<Device> {

    Optional<Device> findByDeviceId(String deviceId);

    Optional<Device> findByDeviceNumber(String deviceNumber);

    Optional<Device> findByDeviceIdAndOrgId(String deviceId, UUID orgId);

    Optional<Device> findByDeviceNumberAndOrgId(String deviceNumber, UUID orgId);

    boolean existsByDeviceId(String deviceId);

    boolean existsByDeviceNumber(String deviceNumber);

    boolean existsByDeviceIdAndOrgId(String deviceId, UUID orgId);

    boolean existsByDeviceNumberAndOrgId(String deviceNumber, UUID orgId);

    boolean existsByDeviceIdAndIdNot(String deviceId, UUID id);

    boolean existsByDeviceNumberAndIdNot(String deviceNumber, UUID id);

    boolean existsByDeviceIdAndOrgIdAndIdNot(String deviceId, UUID orgId, UUID id);

    boolean existsByDeviceNumberAndOrgIdAndIdNot(String deviceNumber, UUID orgId, UUID id);
}
