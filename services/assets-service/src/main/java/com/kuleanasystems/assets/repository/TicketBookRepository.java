package com.kuleanasystems.assets.repository;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.domain.Specification;
import java.util.List;
import org.springframework.lang.Nullable;

import com.kuleanasystems.assets.dto.MaxTicketsSoldProjection;
import com.kuleanasystems.assets.entity.TicketBook;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface TicketBookRepository extends JpaRepository<TicketBook, UUID>, JpaSpecificationExecutor<TicketBook> {
    @Query(value = "SELECT MAX(TICKETS_SOLD) AS maxTicketsSold FROM (" +
                   "SELECT A.SHIFT_ID, SUM(A.NEXT_TICKET - A.START_NUM) AS TICKETS_SOLD " +
                   "FROM asset.ticket_book A, asset.shift B, asset.gate C " +
                   "WHERE A.SHIFT_ID = B.SHIFT_ID AND A.GATE_ID = C.GATE_ID " +
                   "AND A.OPERATION_ID <> :excludeOperationId " +
                   "AND B.NAME = :shiftName AND C.NAME = :gateName " +
                   "AND A.STATUS = 'RETURNED' GROUP BY A.SHIFT_ID)", 
           nativeQuery = true)
    MaxTicketsSoldProjection findMaxTicketsSoldByMetrics(
        @Param("excludeOperationId") UUID excludeOperationId,
        @Param("shiftName") String shiftName,
        @Param("gateName") String gateName
    );

    @Override
    @EntityGraph(attributePaths = {"operation", "shift", "gate", "person"})
    List<TicketBook> findAll(@Nullable Specification<TicketBook> spec);

    @Override
    @EntityGraph(attributePaths = {"operation", "shift", "gate", "person"})
    List<TicketBook> findAll();

    List<TicketBook> findByParentTicketBookId(UUID parentTicketBookId);

    @EntityGraph(attributePaths = {"operation", "shift", "gate"})
    List<TicketBook> findByShift_ShiftId(UUID shiftId);

    @EntityGraph(attributePaths = {"operation", "shift", "gate"})
    List<TicketBook> findByOperation_OperationId(UUID operationId);

    java.util.Optional<TicketBook> findFirstByScanCodeOrderByCreatedAtDesc(String scanCode);

    @org.springframework.data.jpa.repository.Modifying
    @Query("UPDATE TicketBook t SET t.deviceId = :newDeviceId WHERE t.deviceId = :oldDeviceId")
    int updateDeviceId(@Param("oldDeviceId") String oldDeviceId, @Param("newDeviceId") String newDeviceId);

    List<TicketBook> findByShiftShiftId(UUID shiftId);
    List<TicketBook> findByOperationOperationId(UUID operationId);
    List<TicketBook> findByOperationOperationIdAndShiftShiftId(UUID operationId, UUID shiftId);
    java.util.Optional<TicketBook> findByScanCode(String scanCode);
}
