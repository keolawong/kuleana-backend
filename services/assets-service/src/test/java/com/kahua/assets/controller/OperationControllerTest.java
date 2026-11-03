package com.kahua.assets.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.kahua.assets.dto.OperationDto;
import com.kahua.assets.dto.OperationPatchDto;
import com.kahua.assets.entity.Operation;
import com.kahua.assets.service.OperationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class OperationControllerTest {

    private MockMvc mockMvc;

    @Mock
    private OperationService operationService;

    @InjectMocks
    private OperationController operationController;

    private ObjectMapper objectMapper;

    private Operation mockOperation;
    private UUID mockId;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(operationController).build();
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        
        mockId = UUID.randomUUID();
        mockOperation = new Operation();
        mockOperation.setOperationId(mockId);
        mockOperation.setName("Test Op");
        mockOperation.setStartDate(LocalDate.now());
    }

    @Test
    void createOperation_shouldReturn201() throws Exception {
        OperationDto dto = new OperationDto();
        dto.setName("Test Op");
        dto.setStartDate(LocalDate.now());
        dto.setEndDate(LocalDate.now().plusDays(1));
        
        when(operationService.createOperation(any(OperationDto.class))).thenReturn(mockOperation);

        mockMvc.perform(post("/api/operations")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "/api/operations/" + mockId))
                .andExpect(jsonPath("$.name").value("Test Op"));
    }

    @Test
    void updateOperation_shouldReturn200() throws Exception {
        OperationPatchDto patch = new OperationPatchDto();
        
        when(operationService.updateOperation(eq(mockId), any(OperationPatchDto.class))).thenReturn(mockOperation);

        mockMvc.perform(patch("/api/operations/{id}", mockId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(patch)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Test Op"));
    }

    @Test
    void getOperations_shouldReturn200() throws Exception {
        when(operationService.searchOperations(any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(List.of(mockOperation));

        mockMvc.perform(get("/api/operations")
                .param("name", "Test Op"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("Test Op"));
    }

    @Test
    void getOperations_shouldReturn204IfEmpty() throws Exception {
        when(operationService.searchOperations(any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(List.of());

        mockMvc.perform(get("/api/operations"))
                .andExpect(status().isNoContent());
    }
}
