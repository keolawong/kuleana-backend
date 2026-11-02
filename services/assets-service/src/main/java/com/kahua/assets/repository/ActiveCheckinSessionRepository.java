package com.kahua.assets.repository;

import com.kahua.assets.entity.ActiveCheckinSession;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ActiveCheckinSessionRepository extends JpaRepository<ActiveCheckinSession, UUID> {

    @EntityGraph(attributePaths = {"gate", "shift"})
    List<ActiveCheckinSession> findByUsername(String username);

    @EntityGraph(attributePaths = {"gate", "shift"})
    Optional<ActiveCheckinSession> findTopByUsernameOrderByCreatedAtDesc(String username);

    @EntityGraph(attributePaths = {"gate", "shift"})
    List<ActiveCheckinSession> findByStatusAndLastHeartbeatAtAfterOrderByLastHeartbeatAtDesc(String status, Instant threshold);
}
