package com.kuleanasystems.assets.repository;

import com.kuleanasystems.assets.entity.TicketBookHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface TicketBookHistoryRepository extends JpaRepository<TicketBookHistory, UUID> {
    List<TicketBookHistory> findByShift_ShiftId(UUID shiftId);
    List<TicketBookHistory> findByTicketBookIdOrderByCreatedAtAsc(UUID ticketBookId);
}
