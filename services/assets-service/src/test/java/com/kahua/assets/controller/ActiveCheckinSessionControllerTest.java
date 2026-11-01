package com.kahua.assets.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.kahua.assets.dto.ActiveCheckinSessionDto;
import com.kahua.assets.service.ActiveCheckinSessionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class ActiveCheckinSessionControllerTest {

    private MockMvc mockMvc;

    @Mock
    private ActiveCheckinSessionService activeCheckinSessionService;

    @InjectMocks
    private ActiveCheckinSessionController activeCheckinSessionController;

    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());
        mockMvc = MockMvcBuilders.standaloneSetup(activeCheckinSessionController).build();
    }

    @Test
    void testHeartbeat_ApiPrefix() throws Exception {
        UUID sessionId = UUID.randomUUID();
        UUID gateId = UUID.randomUUID();
        UUID shiftId = UUID.randomUUID();

        ActiveCheckinSessionDto requestDto = ActiveCheckinSessionDto.builder()
                .gateId(gateId)
                .shiftId(shiftId)
                .username("jsmith")
                .userDisplayName("John Smith")
                .build();

        ActiveCheckinSessionDto responseDto = ActiveCheckinSessionDto.builder()
                .sessionId(sessionId)
                .gateId(gateId)
                .gateName("Gate North")
                .shiftId(shiftId)
                .shiftName("Shift 1")
                .username("jsmith")
                .userDisplayName("John Smith")
                .status("ACTIVE")
                .lastHeartbeatAt(Instant.now())
                .build();

        when(activeCheckinSessionService.recordHeartbeat(any(ActiveCheckinSessionDto.class))).thenReturn(responseDto);

        mockMvc.perform(post("/api/check-in/sessions/heartbeat")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sessionId").value(sessionId.toString()))
                .andExpect(jsonPath("$.gateName").value("Gate North"))
                .andExpect(jsonPath("$.shiftName").value("Shift 1"))
                .andExpect(jsonPath("$.username").value("jsmith"))
                .andExpect(jsonPath("$.status").value("ACTIVE"));

        verify(activeCheckinSessionService).recordHeartbeat(any(ActiveCheckinSessionDto.class));
    }

    @Test
    void testHeartbeat_BarePrefix() throws Exception {
        UUID sessionId = UUID.randomUUID();
        UUID gateId = UUID.randomUUID();
        UUID shiftId = UUID.randomUUID();

        ActiveCheckinSessionDto responseDto = ActiveCheckinSessionDto.builder()
                .sessionId(sessionId)
                .gateId(gateId)
                .shiftId(shiftId)
                .username("jsmith")
                .status("ACTIVE")
                .build();

        when(activeCheckinSessionService.recordHeartbeat(any(ActiveCheckinSessionDto.class))).thenReturn(responseDto);

        mockMvc.perform(post("/check-in/sessions/heartbeat")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(responseDto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("jsmith"));
    }

    @Test
    void testGetActiveSessions_ApiPrefix() throws Exception {
        ActiveCheckinSessionDto s1 = ActiveCheckinSessionDto.builder()
                .sessionId(UUID.randomUUID())
                .username("worker1")
                .gateName("Gate 1")
                .shiftName("Shift 1")
                .status("ACTIVE")
                .build();

        when(activeCheckinSessionService.getActiveSessions()).thenReturn(List.of(s1));

        mockMvc.perform(get("/api/check-in/sessions/active"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].username").value("worker1"))
                .andExpect(jsonPath("$[0].gateName").value("Gate 1"))
                .andExpect(jsonPath("$[0].status").value("ACTIVE"));

        verify(activeCheckinSessionService).getActiveSessions();
    }

    @Test
    void testGetActiveSessions_BarePrefix() throws Exception {
        when(activeCheckinSessionService.getActiveSessions()).thenReturn(List.of());

        mockMvc.perform(get("/check-in/sessions/active"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

    @Test
    void testCloseSession_RequestParam() throws Exception {
        mockMvc.perform(post("/api/check-in/sessions/close")
                        .param("username", "jsmith"))
                .andExpect(status().isNoContent());

        verify(activeCheckinSessionService).closeSession("jsmith");
    }

    @Test
    void testCloseSession_RequestBody() throws Exception {
        mockMvc.perform(post("/check-in/sessions/close")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("username", "jdoe"))))
                .andExpect(status().isNoContent());

        verify(activeCheckinSessionService).closeSession("jdoe");
    }
}
