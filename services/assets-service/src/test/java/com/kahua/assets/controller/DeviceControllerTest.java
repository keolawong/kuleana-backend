package com.kahua.assets.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.kahua.assets.dto.DeviceDto;
import com.kahua.assets.dto.DevicePatchDto;
import com.kahua.assets.entity.Device;
import com.kahua.assets.service.DeviceService;
import jakarta.persistence.EntityNotFoundException;
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
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class DeviceControllerTest {

    private MockMvc mockMvc;

    @Mock
    private DeviceService deviceService;

    @InjectMocks
    private DeviceController deviceController;

    private ObjectMapper objectMapper;

    private Device mockDevice;
    private UUID mockId;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(deviceController).build();
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());

        mockId = UUID.randomUUID();
        mockDevice = new Device();
        mockDevice.setId(mockId);
        mockDevice.setDeviceId("POS-001");
        mockDevice.setDeviceNumber("001");
        mockDevice.setIsActive(true);
    }

    @Test
    void createDevice_shouldReturn201WithLocation() throws Exception {
        DeviceDto dto = new DeviceDto();
        dto.setDeviceId("POS-001");
        dto.setDeviceNumber("001");

        when(deviceService.createDevice(any(DeviceDto.class))).thenReturn(mockDevice);

        mockMvc.perform(post("/api/devices")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "/api/devices/" + mockId))
                .andExpect(jsonPath("$.deviceId").value("POS-001"))
                .andExpect(jsonPath("$.deviceNumber").value("001"));
    }

    @Test
    void bulkCreateDevices_shouldReturn200() throws Exception {
        DeviceDto dto1 = new DeviceDto();
        dto1.setDeviceId("POS-001");
        dto1.setDeviceNumber("001");

        when(deviceService.bulkCreateDevices(anyList())).thenReturn(List.of(mockDevice));

        mockMvc.perform(post("/api/devices/bulk")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(List.of(dto1))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].deviceId").value("POS-001"))
                .andExpect(jsonPath("$[0].deviceNumber").value("001"));
    }

    @Test
    void getDevices_shouldReturn200() throws Exception {
        when(deviceService.searchDevices(eq("POS"), eq(true), eq("deviceNumber"), eq("asc")))
                .thenReturn(List.of(mockDevice));

        mockMvc.perform(get("/api/devices")
                .param("search", "POS")
                .param("isActive", "true")
                .param("sortBy", "deviceNumber")
                .param("sortDir", "asc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].deviceId").value("POS-001"))
                .andExpect(jsonPath("$[0].deviceNumber").value("001"));
    }

    @Test
    void getDevices_shouldReturn204IfEmpty() throws Exception {
        when(deviceService.searchDevices(any(), any(), any(), any()))
                .thenReturn(List.of());

        mockMvc.perform(get("/api/devices"))
                .andExpect(status().isNoContent());
    }

    @Test
    void getDeviceById_shouldReturn200() throws Exception {
        when(deviceService.getDeviceById(mockId)).thenReturn(mockDevice);

        mockMvc.perform(get("/api/devices/{id}", mockId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(mockId.toString()))
                .andExpect(jsonPath("$.deviceId").value("POS-001"));
    }

    @Test
    void getDeviceById_notFound_shouldReturn404() throws Exception {
        when(deviceService.getDeviceById(mockId))
                .thenThrow(new EntityNotFoundException("Device not found with id: " + mockId));

        mockMvc.perform(get("/api/devices/{id}", mockId))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("Device not found with id: " + mockId));
    }

    @Test
    void updateDevice_shouldReturn200() throws Exception {
        DevicePatchDto patch = new DevicePatchDto();
        patch.setDeviceId("POS-002");

        when(deviceService.updateDevice(eq(mockId), any(DevicePatchDto.class))).thenReturn(mockDevice);

        mockMvc.perform(patch("/api/devices/{id}", mockId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(patch)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.deviceId").value("POS-001"));
    }

    @Test
    void updateDevice_notFound_shouldReturn404() throws Exception {
        DevicePatchDto patch = new DevicePatchDto();
        when(deviceService.updateDevice(eq(mockId), any(DevicePatchDto.class)))
                .thenThrow(new EntityNotFoundException("Device not found with id: " + mockId));

        mockMvc.perform(patch("/api/devices/{id}", mockId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(patch)))
                .andExpect(status().isNotFound());
    }

    @Test
    void deleteDevice_shouldReturn204() throws Exception {
        doNothing().when(deviceService).deleteDevice(mockId);

        mockMvc.perform(delete("/api/devices/{id}", mockId))
                .andExpect(status().isNoContent());

        verify(deviceService).deleteDevice(mockId);
    }
}
