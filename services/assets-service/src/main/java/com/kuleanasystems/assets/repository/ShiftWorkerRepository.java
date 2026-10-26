package com.kuleanasystems.assets.repository;

import com.kuleanasystems.assets.entity.ShiftWorker;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ShiftWorkerRepository extends JpaRepository<ShiftWorker, UUID> {
    List<ShiftWorker> findByShift_ShiftIdOrderByRoleNameAsc(UUID shiftId);

    void deleteByShift_ShiftId(UUID shiftId);
}
