package com.kahua.tracker.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kahua.tracker.dto.ApplicationCreateRequest;
import com.kahua.tracker.dto.application.ApplicationUpdateRequest;
import com.kahua.tracker.entity.Application;
import com.kahua.tracker.service.ApplicationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;


import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class ApplicationControllerTest {

    private MockMvc mockMvc;

    @Mock
    private ApplicationService applicationService;

    @InjectMocks
    private ApplicationController applicationController;

    private ObjectMapper objectMapper;

    private Application mockApplication;
    private UUID mockId;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(applicationController).build();
        objectMapper = new ObjectMapper();

        SecurityContext securityContext = mock(SecurityContext.class);
        Authentication authentication = mock(Authentication.class);
        lenient().when(securityContext.getAuthentication()).thenReturn(authentication);
        lenient().when(authentication.getName()).thenReturn("test-user");
        SecurityContextHolder.setContext(securityContext);

        mockId = UUID.randomUUID();
        mockApplication = new Application();
        mockApplication.setId(mockId);
        mockApplication.setName("Test App");
    }

    @Test
    void createApplication_shouldReturn201() throws Exception {
        ApplicationCreateRequest request = new ApplicationCreateRequest();
        request.setName("Test App");

        when(applicationService.save(any(Application.class))).thenReturn(mockApplication);

        mockMvc.perform(post("/api/applications")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "http://localhost/api/applications/" + mockId))
                .andExpect(jsonPath("$.name").value("Test App"));
    }

    @Test
    void updateApplication_shouldReturn200() throws Exception {
        when(applicationService.updateApplication(eq(mockId), any(ApplicationUpdateRequest.class))).thenReturn(Optional.of(mockApplication));

        mockMvc.perform(patch("/api/applications/{id}", mockId)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"Test App\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Test App"));
    }
    
    @Test
    void updateApplication_shouldReturn404IfNotFound() throws Exception {
        when(applicationService.updateApplication(eq(mockId), any(ApplicationUpdateRequest.class))).thenReturn(Optional.empty());

        mockMvc.perform(patch("/api/applications/{id}", mockId)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"Test App\"}"))
                .andExpect(status().isNotFound());
    }

    @Test
    void searchApplications_shouldReturn200() throws Exception {
        when(applicationService.searchApplications(any(), any(), any(), any()))
                .thenReturn(List.of(mockApplication));

        mockMvc.perform(get("/api/applications")
                .param("name", "Test App"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("Test App"));
    }

    @Test
    void searchApplications_shouldReturn204IfEmpty() throws Exception {
        when(applicationService.searchApplications(any(), any(), any(), any()))
                .thenReturn(List.of());

        mockMvc.perform(get("/api/applications"))
                .andExpect(status().isNoContent());
    }
}
