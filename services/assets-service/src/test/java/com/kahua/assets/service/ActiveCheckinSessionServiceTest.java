package com.kahua.assets.service;

import com.kahua.assets.dto.ActiveCheckinSessionDto;
import com.kahua.assets.entity.ActiveCheckinSession;
import com.kahua.assets.entity.Gate;
import com.kahua.assets.entity.Shift;
import com.kahua.assets.repository.ActiveCheckinSessionRepository;
import com.kahua.assets.repository.GateRepository;
import com.kahua.assets.repository.ShiftRepository;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ActiveCheckinSessionServiceTest {

    @Mock
    private ActiveCheckinSessionRepository activeCheckinSessionRepository;

    @Mock
    private GateRepository gateRepository;

    @Mock
    private ShiftRepository shiftRepository;

    @InjectMocks
    private ActiveCheckinSessionService activeCheckinSessionService;

    private UUID gateId;
    private UUID shiftId;
    private Gate gate;
    private Shift shift;

    @BeforeEach
    void setUp() {
        gateId = UUID.randomUUID();
        shiftId = UUID.randomUUID();

        gate = new Gate();
        gate.setGateId(gateId);
        gate.setName("Main Gate");

        shift = new Shift();
        shift.setShiftId(shiftId);
        shift.setName("Morning Shift");
    }

    @Test
    void recordHeartbeat_WhenNewSession_CreatesAndReturnsDto() {
        ActiveCheckinSessionDto requestDto = ActiveCheckinSessionDto.builder()
                .gateId(gateId)
                .shiftId(shiftId)
                .username("jsmith")
                .userDisplayName("John Smith")
                .build();

        when(gateRepository.findById(gateId)).thenReturn(Optional.of(gate));
        when(shiftRepository.findById(shiftId)).thenReturn(Optional.of(shift));
        when(activeCheckinSessionRepository.findByUsername("jsmith")).thenReturn(java.util.Collections.emptyList());

        UUID generatedId = UUID.randomUUID();
        when(activeCheckinSessionRepository.save(any(ActiveCheckinSession.class))).thenAnswer(invocation -> {
            ActiveCheckinSession entity = invocation.getArgument(0);
            entity.setSessionId(generatedId);
            return entity;
        });

        ActiveCheckinSessionDto result = activeCheckinSessionService.recordHeartbeat(requestDto);

        assertThat(result).isNotNull();
        assertThat(result.getSessionId()).isEqualTo(generatedId);
        assertThat(result.getGateId()).isEqualTo(gateId);
        assertThat(result.getGateName()).isEqualTo("Main Gate");
        assertThat(result.getShiftId()).isEqualTo(shiftId);
        assertThat(result.getShiftName()).isEqualTo("Morning Shift");
        assertThat(result.getUsername()).isEqualTo("jsmith");
        assertThat(result.getUserDisplayName()).isEqualTo("John Smith");
        assertThat(result.getStatus()).isEqualTo("ACTIVE");
        assertThat(result.getLastHeartbeatAt()).isNotNull();

        ArgumentCaptor<ActiveCheckinSession> captor = ArgumentCaptor.forClass(ActiveCheckinSession.class);
        verify(activeCheckinSessionRepository).save(captor.capture());
        ActiveCheckinSession saved = captor.getValue();
        assertThat(saved.getUsername()).isEqualTo("jsmith");
        assertThat(saved.getStatus()).isEqualTo("ACTIVE");
        assertThat(saved.getGate()).isEqualTo(gate);
        assertThat(saved.getShift()).isEqualTo(shift);
    }

    @Test
    void recordHeartbeat_WhenExistingSession_UpdatesAndReturnsDto() {
        ActiveCheckinSession existing = ActiveCheckinSession.builder()
                .sessionId(UUID.randomUUID())
                .username("jsmith")
                .userDisplayName("Old Name")
                .status("INACTIVE")
                .lastHeartbeatAt(Instant.now().minusSeconds(3600))
                .build();

        ActiveCheckinSessionDto requestDto = ActiveCheckinSessionDto.builder()
                .gateId(gateId)
                .shiftId(shiftId)
                .username("jsmith")
                .userDisplayName("John Smith")
                .build();

        when(gateRepository.findById(gateId)).thenReturn(Optional.of(gate));
        when(shiftRepository.findById(shiftId)).thenReturn(Optional.of(shift));
        when(activeCheckinSessionRepository.findByUsername("jsmith")).thenReturn(List.of(existing));
        when(activeCheckinSessionRepository.save(existing)).thenReturn(existing);

        ActiveCheckinSessionDto result = activeCheckinSessionService.recordHeartbeat(requestDto);

        assertThat(result).isNotNull();
        assertThat(existing.getStatus()).isEqualTo("ACTIVE");
        assertThat(existing.getUserDisplayName()).isEqualTo("John Smith");
        assertThat(existing.getGate()).isEqualTo(gate);
        assertThat(existing.getShift()).isEqualTo(shift);
        verify(activeCheckinSessionRepository).save(existing);
    }

    @Test
    void recordHeartbeat_WhenGateNotFound_ThrowsEntityNotFoundException() {
        ActiveCheckinSessionDto requestDto = ActiveCheckinSessionDto.builder()
                .gateId(gateId)
                .shiftId(shiftId)
                .username("jsmith")
                .build();

        when(gateRepository.findById(gateId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> activeCheckinSessionService.recordHeartbeat(requestDto))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessageContaining("Gate not found");

        verify(activeCheckinSessionRepository, never()).save(any());
    }

    @Test
    void recordHeartbeat_WhenShiftNotFound_ThrowsEntityNotFoundException() {
        ActiveCheckinSessionDto requestDto = ActiveCheckinSessionDto.builder()
                .gateId(gateId)
                .shiftId(shiftId)
                .username("jsmith")
                .build();

        when(gateRepository.findById(gateId)).thenReturn(Optional.of(gate));
        when(shiftRepository.findById(shiftId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> activeCheckinSessionService.recordHeartbeat(requestDto))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessageContaining("Shift not found");

        verify(activeCheckinSessionRepository, never()).save(any());
    }

    @Test
    void recordHeartbeat_WhenUsernameBlank_ThrowsIllegalArgumentException() {
        ActiveCheckinSessionDto requestDto = ActiveCheckinSessionDto.builder()
                .gateId(gateId)
                .shiftId(shiftId)
                .username("   ")
                .build();

        assertThatThrownBy(() -> activeCheckinSessionService.recordHeartbeat(requestDto))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void recordHeartbeat_WhenGateIdNull_ThrowsIllegalArgumentException() {
        ActiveCheckinSessionDto requestDto = ActiveCheckinSessionDto.builder()
                .gateId(null)
                .shiftId(shiftId)
                .username("jsmith")
                .build();

        assertThatThrownBy(() -> activeCheckinSessionService.recordHeartbeat(requestDto))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void recordHeartbeat_WhenShiftIdNull_ThrowsIllegalArgumentException() {
        ActiveCheckinSessionDto requestDto = ActiveCheckinSessionDto.builder()
                .gateId(gateId)
                .shiftId(null)
                .username("jsmith")
                .build();

        assertThatThrownBy(() -> activeCheckinSessionService.recordHeartbeat(requestDto))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void getActiveSessions_ReturnsActiveSessionsWithinThreshold() {
        ActiveCheckinSession s1 = ActiveCheckinSession.builder()
                .sessionId(UUID.randomUUID())
                .gate(gate)
                .shift(shift)
                .username("user1")
                .userDisplayName("User One")
                .status("ACTIVE")
                .lastHeartbeatAt(Instant.now())
                .build();

        when(activeCheckinSessionRepository.findByStatusAndLastHeartbeatAtAfterOrderByLastHeartbeatAtDesc(
                eq("ACTIVE"), any(Instant.class))).thenReturn(List.of(s1));

        List<ActiveCheckinSessionDto> activeSessions = activeCheckinSessionService.getActiveSessions();

        assertThat(activeSessions).hasSize(1);
        assertThat(activeSessions.get(0).getUsername()).isEqualTo("user1");
        assertThat(activeSessions.get(0).getGateName()).isEqualTo("Main Gate");
        assertThat(activeSessions.get(0).getShiftName()).isEqualTo("Morning Shift");
    }

    @Test
    void closeSession_WhenSessionExists_SetsStatusInactive() {
        ActiveCheckinSession session = ActiveCheckinSession.builder()
                .sessionId(UUID.randomUUID())
                .username("jsmith")
                .status("ACTIVE")
                .build();

        when(activeCheckinSessionRepository.findByUsername("jsmith")).thenReturn(List.of(session));

        activeCheckinSessionService.closeSession("jsmith");

        assertThat(session.getStatus()).isEqualTo("INACTIVE");
        assertThat(session.getLastHeartbeatAt()).isNotNull();
        verify(activeCheckinSessionRepository).save(session);
    }

    @Test
    void closeSession_WhenSessionNotFound_DoesNothing() {
        when(activeCheckinSessionRepository.findByUsername("unknown")).thenReturn(java.util.Collections.emptyList());

        activeCheckinSessionService.closeSession("unknown");

        verify(activeCheckinSessionRepository, never()).save(any());
    }

    @Test
    void closeSession_WhenUsernameNullOrBlank_DoesNothing() {
        activeCheckinSessionService.closeSession(null);
        activeCheckinSessionService.closeSession("  ");

        verify(activeCheckinSessionRepository, never()).findByUsername(any());
    }
}
