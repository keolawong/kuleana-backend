package com.kahua.assets.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.kahua.assets.dto.CashBankDto;
import com.kahua.assets.dto.CashBankPatchDto;
import com.kahua.assets.entity.CashBank;
import com.kahua.assets.entity.CashBankHistory;
import com.kahua.assets.service.CashBankService;
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
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class CashBankControllerTest {

    private MockMvc mockMvc;

    @Mock
    private CashBankService cashBankService;

    @InjectMocks
    private CashBankController cashBankController;

    private ObjectMapper objectMapper;

    private CashBank mockBank;
    private UUID mockId;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(cashBankController).build();
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());

        mockId = UUID.randomUUID();
        mockBank = new CashBank();
        mockBank.setCashBankId(mockId);
        mockBank.setBankName("Main Vault");
    }

    @Test
    void createCashBank_shouldReturn201() throws Exception {
        CashBankDto dto = new CashBankDto();
        dto.setBankName("Main Vault");

        when(cashBankService.createCashBank(any(CashBankDto.class))).thenReturn(mockBank);

        mockMvc.perform(post("/api/cash-banks")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "/api/cash-banks/" + mockId))
                .andExpect(jsonPath("$.bankName").value("Main Vault"));
    }

    @Test
    void createCashBanksBulk_shouldReturn201() throws Exception {
        CashBankDto dto = new CashBankDto();
        dto.setBankName("Main Vault");

        when(cashBankService.createCashBanks(anyList())).thenReturn(List.of(mockBank));

        mockMvc.perform(post("/api/cash-banks/bulk")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(List.of(dto))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$[0].bankName").value("Main Vault"));
    }

    @Test
    void getCashBanks_shouldReturn200() throws Exception {
        when(cashBankService.searchCashBanks(any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(List.of(mockBank));

        mockMvc.perform(get("/api/cash-banks")
                .param("bankName", "Main Vault"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].bankName").value("Main Vault"));
    }

    @Test
    void getCashBanks_withShiftId_shouldReturnMergedHistory() throws Exception {
        UUID shiftId = UUID.randomUUID();

        CashBankHistory history = new CashBankHistory();
        history.setCashBankId(mockId);
        history.setBankName("History Vault");
        history.setStatus("Returned");
        history.setCreatedAt(java.time.Instant.now());

        when(cashBankService.searchCashBanks(any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), eq(shiftId), any(), any(), any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(List.of()); // No active banks

        when(cashBankService.getHistoryByShiftId(shiftId)).thenReturn(List.of(history));
        when(cashBankService.getCashBanksByIds(anyList())).thenReturn(List.of());

        mockMvc.perform(get("/api/cash-banks")
                .param("shiftId", shiftId.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].bankName").value("History Vault"));
    }
}
