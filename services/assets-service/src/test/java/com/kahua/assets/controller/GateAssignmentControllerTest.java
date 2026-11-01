package com.kahua.assets.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.kahua.assets.dto.GateAssignmentSummaryDto;
import com.kahua.assets.dto.ShiftGateRequirementDto;
import com.kahua.assets.entity.Person;
import com.kahua.assets.service.GateAssignmentService;
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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class GateAssignmentControllerTest {

    private MockMvc mockMvc;

    @Mock
    private GateAssignmentService gateAssignmentService;

    @InjectMocks
    private GateAssignmentController gateAssignmentController;

    private ObjectMapper objectMapper;
    private UUID shiftId;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());
        mockMvc = MockMvcBuilders.standaloneSetup(gateAssignmentController).build();
        shiftId = UUID.randomUUID();
    }

    @Test
    void testGetRequirements() throws Exception {
        ShiftGateRequirementDto dto = ShiftGateRequirementDto.builder()
                .shiftId(shiftId).gateName("32").jobRole("Ticket Seller").workersNeeded(10).build();

        when(gateAssignmentService.getRequirements(shiftId)).thenReturn(List.of(dto));

        mockMvc.perform(get("/api/gate-assignments/requirements")
                        .param("shiftId", shiftId.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].gateName").value("32"))
                .andExpect(jsonPath("$[0].workersNeeded").value(10));
    }

    @Test
    void testApplyTemplate() throws Exception {
        ShiftGateRequirementDto dto = ShiftGateRequirementDto.builder()
                .shiftId(shiftId).gateName("20").jobRole("Scanner").workersNeeded(10).build();

        when(gateAssignmentService.applyTemplate(eq(shiftId), eq("BUSY"))).thenReturn(List.of(dto));

        mockMvc.perform(post("/api/gate-assignments/requirements/template")
                        .param("shiftId", shiftId.toString())
                        .param("template", "BUSY"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].gateName").value("20"));
    }

    @Test
    void testAssignGates() throws Exception {
        Person p = new Person();
        p.setPersonId(UUID.randomUUID());
        p.setFirstName("Alice");

        when(gateAssignmentService.assignGates(shiftId)).thenReturn(List.of(p));

        mockMvc.perform(post("/api/gate-assignments/assign")
                        .param("shiftId", shiftId.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].firstName").value("Alice"));
    }

    @Test
    void testGetSummary() throws Exception {
        GateAssignmentSummaryDto summary = GateAssignmentSummaryDto.builder()
                .totalWorkers(40).totalNeeded(40).totalAssigned(40).solitaryCount(2)
                .gateBreakdowns(List.of())
                .build();

        when(gateAssignmentService.getSummary(shiftId)).thenReturn(summary);

        mockMvc.perform(get("/api/gate-assignments/summary")
                        .param("shiftId", shiftId.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalWorkers").value(40))
                .andExpect(jsonPath("$.solitaryCount").value(2));
    }

    @Test
    void testExportAssignments() throws Exception {
        byte[] excelBytes = new byte[]{1, 2, 3, 4};
        when(gateAssignmentService.exportAssignmentsExcel(shiftId)).thenReturn(excelBytes);

        mockMvc.perform(get("/api/gate-assignments/export")
                        .param("shiftId", shiftId.toString()))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Disposition", "attachment; filename=\"gate_assignments_" + shiftId + ".xlsx\""))
                .andExpect(content().bytes(excelBytes));
    }

    @Test
    void testParseWorkersFile() throws Exception {
        org.springframework.mock.web.MockMultipartFile file = new org.springframework.mock.web.MockMultipartFile(
                "file", "volunteers.csv", "text/csv", "First,Last\nBob,Smith".getBytes()
        );
        com.kahua.assets.dto.PersonBulkWorkerDto dto = com.kahua.assets.dto.PersonBulkWorkerDto.builder()
                .firstName("Bob").lastName("Smith").build();

        when(gateAssignmentService.parseWorkersFile(any())).thenReturn(List.of(dto));

        mockMvc.perform(multipart("/api/gate-assignments/workers/parse").file(file))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].firstName").value("Bob"))
                .andExpect(jsonPath("$[0].lastName").value("Smith"));
    }
}
