package com.kahua.assets.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.kahua.assets.dto.CreditReportDto;
import com.kahua.assets.dto.CreditReportPatchDto;
import com.kahua.assets.entity.CreditReport;
import com.kahua.assets.service.CreditReportService;
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
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class CreditReportControllerTest {

    private MockMvc mockMvc;

    @Mock
    private CreditReportService creditReportService;

    @InjectMocks
    private CreditReportController creditReportController;

    private ObjectMapper objectMapper;

    private CreditReport mockReport;
    private UUID mockId;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(creditReportController).build();
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());

        mockId = UUID.randomUUID();
        mockReport = new CreditReport();
        mockReport.setCreditReportId(mockId);
        mockReport.setTotalAmount(new BigDecimal("150.00"));
    }

    @Test
    void createCreditReport_shouldReturn201() throws Exception {
        CreditReportDto dto = new CreditReportDto();
        dto.setTotalAmount(new BigDecimal("150.00"));

        when(creditReportService.createCreditReport(any(CreditReportDto.class))).thenReturn(mockReport);

        mockMvc.perform(post("/api/credit-reports")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "/api/credit-reports/" + mockId))
                .andExpect(jsonPath("$.totalAmount").value(150.0));
    }

    @Test
    void updateCreditReport_shouldReturn200() throws Exception {
        CreditReportPatchDto patch = new CreditReportPatchDto();
        when(creditReportService.updateCreditReport(eq(mockId), any(CreditReportPatchDto.class))).thenReturn(mockReport);

        mockMvc.perform(patch("/api/credit-reports/{id}", mockId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(patch)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalAmount").value(150.0));
    }

    @Test
    void getCreditReports_shouldReturn200() throws Exception {
        when(creditReportService.searchCreditReports(any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(List.of(mockReport));

        mockMvc.perform(get("/api/credit-reports")
                .param("totalAmount", "150.00"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].totalAmount").value(150.0));
    }

    @Test
    void getCreditReports_shouldReturn204IfEmpty() throws Exception {
        when(creditReportService.searchCreditReports(any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(List.of());

        mockMvc.perform(get("/api/credit-reports"))
                .andExpect(status().isNoContent());
    }
}
