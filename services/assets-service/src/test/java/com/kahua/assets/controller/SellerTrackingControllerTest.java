package com.kahua.assets.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.kahua.assets.dto.SellerPerformanceDto;
import com.kahua.assets.dto.SellerSessionStubDto;
import com.kahua.assets.service.SellerTrackingService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
public class SellerTrackingControllerTest {

    private MockMvc mockMvc;

    @Mock
    private SellerTrackingService sellerTrackingService;

    @InjectMocks
    private SellerTrackingController sellerTrackingController;

    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());
        mockMvc = MockMvcBuilders.standaloneSetup(sellerTrackingController).build();
    }

    @Test
    public void getSellerPerformance_shouldReturn200() throws Exception {
        SellerSessionStubDto stub = SellerSessionStubDto.builder()
                .shiftId(UUID.randomUUID())
                .shiftName("Session 1 - Saturday AM")
                .operationName("AIBF 2026")
                .gateName("Main Gate")
                .sellerType("CASH")
                .ticketsSold(100)
                .startingAmount(new BigDecimal("100.00"))
                .expectedAmount(new BigDecimal("1600.00"))
                .collectedAmount(new BigDecimal("1580.00"))
                .variance(new BigDecimal("-20.00"))
                .build();

        SellerPerformanceDto dto = SellerPerformanceDto.builder()
                .externalId("VOL-101")
                .sellerName("Jane Doe")
                .personId(UUID.randomUUID())
                .organization("Kiwanis Club of Alameda")
                .totalSessions(1)
                .balancedSessions(0)
                .overSessions(0)
                .underSessions(1)
                .netVariance(new BigDecimal("-20.00"))
                .totalAbsoluteDiscrepancy(new BigDecimal("20.00"))
                .totalExpected(new BigDecimal("1600.00"))
                .totalCollected(new BigDecimal("1580.00"))
                .accuracyRate(new BigDecimal("98.75"))
                .performanceStatus("UNDER")
                .sessions(List.of(stub))
                .build();

        when(sellerTrackingService.getSellerPerformance(any(), any(), any(), any(), any()))
                .thenReturn(List.of(dto));

        mockMvc.perform(get("/api/seller-tracking/performance"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].externalId").value("VOL-101"))
                .andExpect(jsonPath("$[0].sellerName").value("Jane Doe"))
                .andExpect(jsonPath("$[0].totalCollected").value(1580.00))
                .andExpect(jsonPath("$[0].netVariance").value(-20.00))
                .andExpect(jsonPath("$[0].sessions[0].collectedAmount").value(1580.00));

        mockMvc.perform(get("/seller-tracking/performance"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].externalId").value("VOL-101"))
                .andExpect(jsonPath("$[0].sellerName").value("Jane Doe"));
    }
}

