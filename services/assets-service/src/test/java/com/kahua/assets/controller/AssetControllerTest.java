package com.kahua.assets.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kahua.assets.dto.AssetSearchResponseDto;
import com.kahua.assets.entity.CashBank;
import com.kahua.assets.entity.TicketBook;
import com.kahua.assets.service.AssetService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class AssetControllerTest {

    private MockMvc mockMvc;

    @Mock
    private AssetService assetService;

    @InjectMocks
    private AssetController assetController;

    private UUID mockOperationId;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(assetController).build();
        mockOperationId = UUID.randomUUID();
    }

    @Test
    void searchAssets_shouldReturn200() throws Exception {
        AssetSearchResponseDto mockResponse = new AssetSearchResponseDto();
        com.kahua.assets.dto.TicketBookDto tb = new com.kahua.assets.dto.TicketBookDto();
        tb.setScanCode("TB-123");
        mockResponse.setTicketBook(List.of(tb));
        
        com.kahua.assets.dto.CashBankDto cb = new com.kahua.assets.dto.CashBankDto();
        cb.setBankName("Vault A");
        mockResponse.setCashBank(List.of(cb));

        com.kahua.assets.dto.DeviceDto dev = new com.kahua.assets.dto.DeviceDto();
        dev.setDeviceId("POS-001");
        dev.setDeviceNumber("DEV-01");
        mockResponse.setDevice(List.of(dev));

        when(assetService.searchAssets(eq(mockOperationId), eq("search_term"))).thenReturn(mockResponse);

        mockMvc.perform(get("/api/assets/{operationId}", mockOperationId)
                .param("search", "search_term"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$['ticket-book'][0].scanCode").value("TB-123"))
                .andExpect(jsonPath("$['cash-bank'][0].bankName").value("Vault A"))
                .andExpect(jsonPath("$['device'][0].deviceId").value("POS-001"))
                .andExpect(jsonPath("$['device'][0].deviceNumber").value("DEV-01"));
    }

    @Test
    void searchAssets_shouldReturn204IfEmpty() throws Exception {
        AssetSearchResponseDto emptyResponse = new AssetSearchResponseDto();
        emptyResponse.setTicketBook(List.of());
        emptyResponse.setCashBank(List.of());
        emptyResponse.setDevice(List.of());

        when(assetService.searchAssets(eq(mockOperationId), eq(null))).thenReturn(emptyResponse);

        mockMvc.perform(get("/api/assets/{operationId}", mockOperationId))
                .andExpect(status().isNoContent());
    }

    @Test
    void searchAssets_shouldReturn200IfOnlyDevicePresent() throws Exception {
        AssetSearchResponseDto response = new AssetSearchResponseDto();
        response.setTicketBook(List.of());
        response.setCashBank(List.of());

        com.kahua.assets.dto.DeviceDto dev = new com.kahua.assets.dto.DeviceDto();
        dev.setDeviceId("POS-999");
        response.setDevice(List.of(dev));

        when(assetService.searchAssets(eq(mockOperationId), eq("POS-999"))).thenReturn(response);

        mockMvc.perform(get("/api/assets/{operationId}", mockOperationId)
                .param("search", "POS-999"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$['device'][0].deviceId").value("POS-999"));
    }
}
