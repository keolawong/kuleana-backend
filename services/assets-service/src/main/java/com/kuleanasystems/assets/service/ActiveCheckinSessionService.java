package com.kuleanasystems.assets.service;

import com.kuleanasystems.assets.dto.ActiveCheckinSessionDto;
import com.kuleanasystems.assets.entity.ActiveCheckinSession;
import com.kuleanasystems.assets.entity.Gate;
import com.kuleanasystems.assets.entity.Shift;
import com.kuleanasystems.assets.repository.ActiveCheckinSessionRepository;
import com.kuleanasystems.assets.repository.GateRepository;
import com.kuleanasystems.assets.repository.ShiftRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ActiveCheckinSessionService {

    private final ActiveCheckinSessionRepository activeCheckinSessionRepository;
    private final GateRepository gateRepository;
    private final ShiftRepository shiftRepository;

    @Transactional
    public ActiveCheckinSessionDto recordHeartbeat(ActiveCheckinSessionDto dto) {
        if (dto.getUsername() == null || dto.getUsername().isBlank()) {
            throw new IllegalArgumentException("username must not be blank");
        }
        if (dto.getGateId() == null) {
            throw new IllegalArgumentException("gateId must not be null");
        }
        if (dto.getShiftId() == null) {
            throw new IllegalArgumentException("shiftId must not be null");
        }

        Gate gate = gateRepository.findById(dto.getGateId())
                .orElseThrow(() -> new EntityNotFoundException("Gate not found with id: " + dto.getGateId()));
        Shift shift = shiftRepository.findById(dto.getShiftId())
                .orElseThrow(() -> new EntityNotFoundException("Shift not found with id: " + dto.getShiftId()));

        ActiveCheckinSession session = activeCheckinSessionRepository.findByUsername(dto.getUsername())
                .orElseGet(() -> {
                    ActiveCheckinSession s = new ActiveCheckinSession();
                    s.setUsername(dto.getUsername());
                    return s;
                });

        session.setGate(gate);
        session.setShift(shift);
        session.setUserDisplayName(dto.getUserDisplayName());
        session.setStatus("ACTIVE");
        session.setLastHeartbeatAt(Instant.now());

        ActiveCheckinSession saved = activeCheckinSessionRepository.save(session);
        return toDto(saved);
    }

    @Transactional(readOnly = true)
    public List<ActiveCheckinSessionDto> getActiveSessions() {
        Instant threshold = Instant.now().minus(Duration.ofMinutes(10));
        List<ActiveCheckinSession> sessions = activeCheckinSessionRepository
                .findByStatusAndLastHeartbeatAtAfterOrderByLastHeartbeatAtDesc("ACTIVE", threshold);
        return sessions.stream().map(this::toDto).collect(Collectors.toList());
    }

    @Transactional
    public void closeSession(String username) {
        if (username == null || username.isBlank()) {
            return;
        }
        activeCheckinSessionRepository.findByUsername(username).ifPresent(session -> {
            session.setStatus("INACTIVE");
            session.setLastHeartbeatAt(Instant.now());
            activeCheckinSessionRepository.save(session);
        });
    }

    public ActiveCheckinSessionDto toDto(ActiveCheckinSession session) {
        if (session == null) {
            return null;
        }
        return ActiveCheckinSessionDto.builder()
                .sessionId(session.getSessionId())
                .gateId(session.getGate() != null ? session.getGate().getGateId() : null)
                .gateName(session.getGate() != null ? session.getGate().getName() : null)
                .shiftId(session.getShift() != null ? session.getShift().getShiftId() : null)
                .shiftName(session.getShift() != null ? session.getShift().getName() : null)
                .username(session.getUsername())
                .userDisplayName(session.getUserDisplayName())
                .status(session.getStatus())
                .lastHeartbeatAt(session.getLastHeartbeatAt())
                .build();
    }
}
