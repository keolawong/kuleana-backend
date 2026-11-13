package com.kahua.assets.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.kahua.assets.dto.GateCaptainDto;
import com.kahua.assets.dto.GateCaptainPatchDto;
import com.kahua.assets.entity.GateCaptain;
import com.kahua.assets.service.GateCaptainService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class GateCaptainControllerTest {

    private MockMvc mockMvc;

    @Mock
    private GateCaptainService gateCaptainService;

    @InjectMocks
    private GateCaptainController gateCaptainController;

    private ObjectMapper objectMapper;

    private GateCaptain mockCaptain;
    private UUID mockId;
    private UUID mockGateId;
    private UUID mockOpId;
    private UUID mockShiftId;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(gateCaptainController).build();
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());

        mockId = UUID.randomUUID();
        mockGateId = UUID.randomUUID();
        mockOpId = UUID.randomUUID();
        mockShiftId = UUID.randomUUID();

        mockCaptain = new GateCaptain();
        mockCaptain.setGateCaptainId(mockId);
        mockCaptain.setCaptainName("John Doe");
    }

    @Test
    void createGateCaptain_shouldReturn201() throws Exception {
        GateCaptainDto dto = new GateCaptainDto();
        dto.setCaptainName("John Doe");
        dto.setGateId(mockGateId);
        dto.setOperationId(mockOpId);
        dto.setShiftId(mockShiftId);

        when(gateCaptainService.createGateCaptain(any(GateCaptainDto.class))).thenReturn(mockCaptain);

        mockMvc.perform(post("/api/gate-captains")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "/api/gate-captains/" + mockId))
                .andExpect(jsonPath("$.captainName").value("John Doe"));
    }

    @Test
    void updateGateCaptain_shouldReturn200() throws Exception {
        GateCaptainPatchDto patch = new GateCaptainPatchDto();
        
        when(gateCaptainService.updateGateCaptain(eq(mockId), any(GateCaptainPatchDto.class))).thenReturn(mockCaptain);

        mockMvc.perform(patch("/api/gate-captains/{id}", mockId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(patch)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.captainName").value("John Doe"));
    }

    @Test
    void getGateCaptains_shouldReturn200() throws Exception {
        when(gateCaptainService.searchGateCaptains(any(), any(), any(), any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(List.of(mockCaptain));

        mockMvc.perform(get("/api/gate-captains")
                .param("captainName", "John Doe"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].captainName").value("John Doe"));
    }

    @Test
    void getGateCaptains_shouldReturn204IfEmpty() throws Exception {
        when(gateCaptainService.searchGateCaptains(any(), any(), any(), any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(List.of());

        mockMvc.perform(get("/api/gate-captains"))
                .andExpect(status().isNoContent());
    }
}
