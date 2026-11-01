package com.kahua.assets.controller;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.kahua.assets.dto.report.OperationReportDto;
import com.kahua.assets.dto.report.ShiftReportDto;
import com.kahua.assets.service.ReportService;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

@ExtendWith(MockitoExtension.class)
class ReportControllerTest {

    private MockMvc mockMvc;

    @Mock
    private ReportService reportService;

    @InjectMocks
    private ReportController reportController;

    private UUID shiftId;
    private UUID operationId;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(reportController).build();
        shiftId = UUID.randomUUID();
        operationId = UUID.randomUUID();
    }

    @Test
    void getShiftReport_shouldReturnShiftReportDto() throws Exception {
        ShiftReportDto dto = ShiftReportDto.builder()
            .shiftId(shiftId)
            .shiftName("Shift 1")
            .ticketPrice(BigDecimal.valueOf(15.00))
            .ccFee(BigDecimal.valueOf(1.45))
            .reportData(List.of())
            .bankSummary(List.of())
            .salesSummary(List.of())
            .build();

        when(reportService.getShiftReport(shiftId)).thenReturn(dto);

        mockMvc.perform(get("/api/reports/shifts/{shiftId}", shiftId)
                .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.shiftId").value(shiftId.toString()))
            .andExpect(jsonPath("$.shiftName").value("Shift 1"))
            .andExpect(jsonPath("$.ticketPrice").value(15.00));
    }

    @Test
    void getOperationReport_shouldReturnOperationReportDto() throws Exception {
        OperationReportDto dto = OperationReportDto.builder()
            .operationId(operationId)
            .operationName("Balloon Fiesta 2026")
            .gates(List.of())
            .sections(Map.of())
            .sectionTotals(Map.of())
            .build();

        when(reportService.getOperationReport(operationId)).thenReturn(dto);

        mockMvc.perform(get("/api/reports/operations/{operationId}", operationId)
                .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.operationId").value(operationId.toString()))
            .andExpect(jsonPath("$.operationName").value("Balloon Fiesta 2026"));
    }
}
