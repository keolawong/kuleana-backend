package com.kahua.assets.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.kahua.assets.dto.GateDto;
import com.kahua.assets.dto.GatePatchDto;
import com.kahua.assets.entity.Gate;
import com.kahua.assets.service.GateService;
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
class GateControllerTest {

    private MockMvc mockMvc;

    @Mock
    private GateService gateService;

    @InjectMocks
    private GateController gateController;

    private ObjectMapper objectMapper;

    private Gate mockGate;
    private UUID mockId;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(gateController).build();
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());

        mockId = UUID.randomUUID();
        mockGate = new Gate();
        mockGate.setGateId(mockId);
        mockGate.setName("Gate A");
    }

    @Test
    void createGate_shouldReturn201() throws Exception {
        GateDto dto = new GateDto();
        dto.setName("Gate A");

        when(gateService.createGate(any(GateDto.class))).thenReturn(mockGate);

        mockMvc.perform(post("/api/gates")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "/api/gates/" + mockId))
                .andExpect(jsonPath("$.name").value("Gate A"));
    }

    @Test
    void updateGate_shouldReturn200() throws Exception {
        GatePatchDto patch = new GatePatchDto();
        
        when(gateService.updateGate(eq(mockId), any(GatePatchDto.class))).thenReturn(mockGate);

        mockMvc.perform(patch("/api/gates/{id}", mockId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(patch)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Gate A"));
    }

    @Test
    void getGates_shouldReturn200() throws Exception {
        when(gateService.searchGates(any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(List.of(mockGate));

        mockMvc.perform(get("/api/gates")
                .param("name", "Gate A"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("Gate A"));
    }

    @Test
    void getGates_shouldReturn204IfEmpty() throws Exception {
        when(gateService.searchGates(any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(List.of());

        mockMvc.perform(get("/api/gates"))
                .andExpect(status().isNoContent());
    }
}
