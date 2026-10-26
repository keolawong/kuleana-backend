package com.kuleanasystems.assets.repository;

import com.kuleanasystems.assets.entity.ShiftSales;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ShiftSalesRepository extends JpaRepository<ShiftSales, UUID> {
    @EntityGraph(attributePaths = {"windowSales"})
    List<ShiftSales> findByShift_ShiftIdOrderByRecordedAtAsc(UUID shiftId);

    void deleteByShift_ShiftId(UUID shiftId);
}
