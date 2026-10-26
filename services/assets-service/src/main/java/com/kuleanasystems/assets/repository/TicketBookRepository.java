package com.kuleanasystems.assets.repository;

import com.kuleanasystems.assets.entity.TicketBook;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface TicketBookRepository extends JpaRepository<TicketBook, UUID> {
    List<TicketBook> findByShiftShiftId(UUID shiftId);
    List<TicketBook> findByOperationOperationId(UUID operationId);
    List<TicketBook> findByOperationOperationIdAndShiftShiftId(UUID operationId, UUID shiftId);
    Optional<TicketBook> findByScanCode(String scanCode);
}
