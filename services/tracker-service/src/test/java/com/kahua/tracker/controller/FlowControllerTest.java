package com.kahua.tracker.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kahua.tracker.dto.flow.FlowCreateRequest;
import com.kahua.tracker.dto.flow.FlowUpdateRequest;
import com.kahua.tracker.entity.Flow;
import com.kahua.tracker.service.FlowService;
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
import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class FlowControllerTest {

    private MockMvc mockMvc;

    @Mock
    private FlowService flowService;

    @InjectMocks
    private FlowController flowController;

    private ObjectMapper objectMapper;

    private Flow mockFlow;
    private UUID mockId;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(flowController).build();
        objectMapper = new ObjectMapper();

        mockId = UUID.randomUUID();
        mockFlow = new Flow();
        mockFlow.setId(mockId);
        mockFlow.setFlowName("Test Flow");
    }

    @Test
    void createFlow_shouldReturn201() throws Exception {
        FlowCreateRequest request = new FlowCreateRequest();
        request.setFlowName("Test Flow");

        when(flowService.createFlow(any(FlowCreateRequest.class))).thenReturn(mockFlow);

        mockMvc.perform(post("/api/flows")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "http://localhost/api/flows/" + mockId))
                .andExpect(jsonPath("$.flowName").value("Test Flow"));
    }

    @Test
    void updateFlow_shouldReturn200() throws Exception {
        when(flowService.updateFlow(eq(mockId), any(FlowUpdateRequest.class))).thenReturn(Optional.of(mockFlow));

        mockMvc.perform(patch("/api/flows/{id}", mockId)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"flowName\":\"Test Flow\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.flowName").value("Test Flow"));
    }
    
    @Test
    void updateFlow_shouldReturn404IfNotFound() throws Exception {
        when(flowService.updateFlow(eq(mockId), any(FlowUpdateRequest.class))).thenReturn(Optional.empty());

        mockMvc.perform(patch("/api/flows/{id}", mockId)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"flowName\":\"Test Flow\"}"))
                .andExpect(status().isNotFound());
    }

    @Test
    void searchFlows_shouldReturn200() throws Exception {
        when(flowService.searchFlows(any(), any(), any()))
                .thenReturn(List.of(mockFlow));

        mockMvc.perform(get("/api/flows")
                .param("flowName", "Test Flow"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].flowName").value("Test Flow"));
    }

    @Test
    void searchFlows_shouldReturn204IfEmpty() throws Exception {
        when(flowService.searchFlows(any(), any(), any()))
                .thenReturn(List.of());

        mockMvc.perform(get("/api/flows"))
                .andExpect(status().isNoContent());
    }
}
