package com.kahua.assets.service;

import com.kahua.assets.dto.ActiveCheckinSessionDto;
import com.kahua.assets.entity.ActiveCheckinSession;
import com.kahua.assets.entity.Gate;
import com.kahua.assets.entity.Shift;
import com.kahua.assets.repository.ActiveCheckinSessionRepository;
import com.kahua.assets.repository.GateRepository;
import com.kahua.assets.repository.ShiftRepository;
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

        List<ActiveCheckinSession> existingSessions = activeCheckinSessionRepository.findByUsername(dto.getUsername());
        ActiveCheckinSession session;
        if (!existingSessions.isEmpty()) {
            session = existingSessions.get(0);
            if (existingSessions.size() > 1) {
                for (int i = 1; i < existingSessions.size(); i++) {
                    activeCheckinSessionRepository.delete(existingSessions.get(i));
                }
            }
        } else {
            session = new ActiveCheckinSession();
            session.setUsername(dto.getUsername());
        }

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
        List<ActiveCheckinSession> sessions = activeCheckinSessionRepository.findByUsername(username);
        for (ActiveCheckinSession session : sessions) {
            session.setStatus("INACTIVE");
            session.setLastHeartbeatAt(Instant.now());
            activeCheckinSessionRepository.save(session);
        }
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
