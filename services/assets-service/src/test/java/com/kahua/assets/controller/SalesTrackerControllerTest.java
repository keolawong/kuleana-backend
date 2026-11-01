package com.kahua.assets.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.kahua.assets.dto.ShiftSalesDto;
import com.kahua.assets.dto.ShiftWorkerDto;
import com.kahua.assets.dto.WindowSalesDto;
import com.kahua.assets.service.SalesTrackerService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class SalesTrackerControllerTest {

    private MockMvc mockMvc;

    @Mock
    private SalesTrackerService salesTrackerService;

    @InjectMocks
    private SalesTrackerController salesTrackerController;

    private ObjectMapper objectMapper;
    private UUID shiftId;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());
        mockMvc = MockMvcBuilders.standaloneSetup(salesTrackerController).build();
        shiftId = UUID.randomUUID();
    }

    @Test
    void testGetSnapshots() throws Exception {
        ShiftSalesDto dto = ShiftSalesDto.builder()
                .shiftSalesId(UUID.randomUUID())
                .shiftId(shiftId)
                .recordedAt(Instant.now())
                .totalSales(new BigDecimal("1250.00"))
                .laborCost(new BigDecimal("200.00"))
                .windowSales(List.of(
                        WindowSalesDto.builder().windowName("Gate 1 Window 1").salesAmount(new BigDecimal("1250.00")).build()
                ))
                .build();

        when(salesTrackerService.getShiftSales(shiftId)).thenReturn(List.of(dto));

        mockMvc.perform(get("/api/sales-tracker/{shiftId}/snapshots", shiftId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].totalSales").value(1250.00))
                .andExpect(jsonPath("$[0].windowSales[0].windowName").value("Gate 1 Window 1"));
    }

    @Test
    void testRecordSnapshot() throws Exception {
        ShiftSalesDto requestDto = ShiftSalesDto.builder()
                .shiftId(shiftId)
                .recordedAt(Instant.now())
                .totalSales(new BigDecimal("3500.00"))
                .laborCost(new BigDecimal("450.00"))
                .windowSales(List.of(
                        WindowSalesDto.builder().windowName("Gate 2 Window 1").salesAmount(new BigDecimal("3500.00")).build()
                ))
                .build();

        ShiftSalesDto responseDto = ShiftSalesDto.builder()
                .shiftSalesId(UUID.randomUUID())
                .shiftId(shiftId)
                .recordedAt(requestDto.getRecordedAt())
                .totalSales(requestDto.getTotalSales())
                .laborCost(requestDto.getLaborCost())
                .windowSales(requestDto.getWindowSales())
                .build();

        when(salesTrackerService.saveShiftSales(eq(shiftId), any(ShiftSalesDto.class))).thenReturn(responseDto);

        mockMvc.perform(post("/api/sales-tracker/{shiftId}/snapshots", shiftId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDto)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$[0].shiftSalesId").doesNotExist())
                .andExpect(jsonPath("$.shiftSalesId").value(responseDto.getShiftSalesId().toString()))
                .andExpect(jsonPath("$.totalSales").value(3500.00));
    }

    @Test
    void testGetRoles() throws Exception {
        ShiftWorkerDto workerDto = ShiftWorkerDto.builder()
                .shiftWorkerId(UUID.randomUUID())
                .shiftId(shiftId)
                .roleName("Cashier")
                .hourlyRate(new BigDecimal("15.00"))
                .count(4)
                .build();

        when(salesTrackerService.getShiftWorkers(shiftId)).thenReturn(List.of(workerDto));

        mockMvc.perform(get("/api/sales-tracker/{shiftId}/roles", shiftId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].roleName").value("Cashier"))
                .andExpect(jsonPath("$[0].hourlyRate").value(15.00))
                .andExpect(jsonPath("$[0].count").value(4));
    }

    @Test
    void testUpdateRoles() throws Exception {
        List<ShiftWorkerDto> updateList = List.of(
                ShiftWorkerDto.builder().roleName("Cashier").hourlyRate(new BigDecimal("16.00")).count(5).build()
        );

        when(salesTrackerService.updateShiftWorkers(eq(shiftId), anyList())).thenReturn(updateList);

        mockMvc.perform(put("/api/sales-tracker/{shiftId}/roles", shiftId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateList)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].roleName").value("Cashier"))
                .andExpect(jsonPath("$[0].hourlyRate").value(16.00))
                .andExpect(jsonPath("$[0].count").value(5));
    }

    @Test
    void testResetShiftSales() throws Exception {
        mockMvc.perform(delete("/api/sales-tracker/{shiftId}/reset", shiftId))
                .andExpect(status().isNoContent());

        verify(salesTrackerService).resetShiftSales(shiftId);
    }

    @Test
    void testRemoveWindowSales() throws Exception {
        mockMvc.perform(delete("/api/sales-tracker/{shiftId}/windows", shiftId)
                        .param("windowName", "Gate 1 Window 2"))
                .andExpect(status().isNoContent());

        verify(salesTrackerService).removeWindowSales(shiftId, "Gate 1 Window 2");
    }

    @Test
    void testRemoveGateSales() throws Exception {
        mockMvc.perform(delete("/api/sales-tracker/{shiftId}/gates", shiftId)
                        .param("gateName", "Gate 1"))
                .andExpect(status().isNoContent());

        verify(salesTrackerService).removeGateSales(shiftId, "Gate 1");
    }
}
