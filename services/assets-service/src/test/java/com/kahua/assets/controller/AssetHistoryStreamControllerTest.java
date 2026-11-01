package com.kahua.assets.controller;

import com.kahua.assets.service.AssetHistoryStreamingService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class AssetHistoryStreamControllerTest {

    private MockMvc mockMvc;

    @Mock
    private AssetHistoryStreamingService assetHistoryStreamingService;

    private AssetHistoryStreamController controller;

    @BeforeEach
    void setUp() {
        controller = new AssetHistoryStreamController(assetHistoryStreamingService);
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    @Test
    void streamAssetHistory_ReturnsSseEmitter() throws Exception {
        java.util.UUID assetId = java.util.UUID.randomUUID();
        String assetType = "Ticket Book";

        MvcResult result = mockMvc.perform(get("/api/asset-history/stream")
                        .param("assetId", assetId.toString())
                        .param("assetType", assetType))
                .andExpect(status().isOk())
                .andReturn();
        verify(assetHistoryStreamingService).streamHistory(eq(assetId), eq(assetType), any(), any(SseEmitter.class));
    }
    
    @Test
    void streamAssetHistory_MissingScanCode_ReturnsBadRequest() throws Exception {
        mockMvc.perform(get("/api/asset-history/stream"))
                .andExpect(status().isBadRequest());
    }
}
