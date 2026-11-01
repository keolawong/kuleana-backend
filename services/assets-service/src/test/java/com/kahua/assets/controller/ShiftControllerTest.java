package com.kahua.assets.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.kahua.assets.dto.ShiftDto;
import com.kahua.assets.dto.ShiftPatchDto;
import com.kahua.assets.entity.Shift;
import com.kahua.assets.service.ShiftService;
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
class ShiftControllerTest {

    private MockMvc mockMvc;

    @Mock
    private ShiftService shiftService;

    @InjectMocks
    private ShiftController shiftController;

    private ObjectMapper objectMapper;

    private Shift mockShift;
    private UUID mockId;
    private UUID mockOpId;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(shiftController).build();
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());

        mockId = UUID.randomUUID();
        mockOpId = UUID.randomUUID();
        mockShift = new Shift();
        mockShift.setShiftId(mockId);
        mockShift.setName("Morning Shift");
    }

    @Test
    void createShift_shouldReturn201() throws Exception {
        ShiftDto dto = new ShiftDto();
        dto.setOperationId(mockOpId);
        dto.setName("Morning Shift");

        when(shiftService.createShift(any(ShiftDto.class))).thenReturn(mockShift);

        mockMvc.perform(post("/api/shifts")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "/api/shifts/" + mockId))
                .andExpect(jsonPath("$.name").value("Morning Shift"));
    }

    @Test
    void updateShift_shouldReturn200() throws Exception {
        ShiftPatchDto patch = new ShiftPatchDto();
        
        when(shiftService.updateShift(eq(mockId), any(ShiftPatchDto.class))).thenReturn(mockShift);

        mockMvc.perform(patch("/api/shifts/{id}", mockId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(patch)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Morning Shift"));
    }

    @Test
    void getShifts_shouldReturn200() throws Exception {
        when(shiftService.searchShifts(any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), eq(null)))
                .thenReturn(List.of(mockShift));

        mockMvc.perform(get("/api/shifts")
                .param("name", "Morning Shift"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("Morning Shift"));
    }

    @Test
    void getShifts_shouldReturn204IfEmpty() throws Exception {
        when(shiftService.searchShifts(any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), eq(null)))
                .thenReturn(List.of());

        mockMvc.perform(get("/api/shifts"))
                .andExpect(status().isNoContent());
    }

    @Test
    void getActiveShift_shouldReturn200() throws Exception {
        when(shiftService.searchShifts(any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), eq(true)))
                .thenReturn(List.of(mockShift));

        mockMvc.perform(get("/api/shifts/active"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("Morning Shift"));
    }

    @Test
    void getActiveShift_shouldReturn204IfNoActiveShift() throws Exception {
        when(shiftService.searchShifts(any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), eq(true)))
                .thenReturn(List.of());

        mockMvc.perform(get("/api/shifts/active"))
                .andExpect(status().isNoContent());
    }
}
