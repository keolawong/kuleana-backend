package com.kuleanasystems.auth.controller;

import com.kuleanasystems.auth.dto.application.ApplicationCreateRequest;
import com.kuleanasystems.auth.dto.application.ApplicationResponse;
import com.kuleanasystems.auth.dto.application.ApplicationUpdateRequest;
import com.kuleanasystems.auth.entity.Application;
import com.kuleanasystems.auth.service.ApplicationService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/applications")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
public class ApplicationController {

    private final ApplicationService applicationService;

    @PostMapping
    @PreAuthorize("hasRole('OWNER')")
    public ResponseEntity<ApplicationResponse> createApplication(@RequestBody ApplicationCreateRequest request) {
        String currentUser = SecurityContextHolder.getContext().getAuthentication().getName();

        Application application = Application.builder()
                .name(request.getName())
                .writeRoleId(request.getWriteRoleId())
                .readRoleId(request.getReadRoleId())
                .isActive(request.getIsActive() != null ? request.getIsActive() : true)
                .createdBy(currentUser)
                .updatedBy(currentUser)
                .build();

        Application savedApplication = applicationService.save(application);

        URI location = ServletUriComponentsBuilder
                .fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(savedApplication.getId())
                .toUri();

        return ResponseEntity.created(location).body(mapToApplicationResponse(savedApplication));
    }

    @PatchMapping("/{id}")
    @PreAuthorize("hasAnyRole('OWNER', 'ADMIN')")
    public ResponseEntity<ApplicationResponse> updateApplication(@PathVariable UUID id, @RequestBody ApplicationUpdateRequest request) {
        return applicationService.updateApplication(id, request)
                .map(this::mapToApplicationResponse)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping
    @PreAuthorize("hasRole('VIEWER')")
    public ResponseEntity<List<ApplicationResponse>> searchApplications(
            @RequestParam(required = false) String name,
            @RequestParam(required = false) UUID writeRoleId,
            @RequestParam(required = false) UUID readRoleId,
            @RequestParam(required = false) Boolean isActive) {
        List<Application> applications = applicationService.searchApplications(name, writeRoleId, readRoleId, isActive);
        if (applications.isEmpty()) {
            return ResponseEntity.noContent().build();
        }
        List<ApplicationResponse> response = applications.stream()
                .map(this::mapToApplicationResponse)
                .collect(Collectors.toList());
        return ResponseEntity.ok(response);
    }

    private ApplicationResponse mapToApplicationResponse(Application application) {
        ApplicationResponse response = new ApplicationResponse();
        response.setId(application.getId());
        response.setName(application.getName());
        response.setWriteRoleId(application.getWriteRoleId());
        response.setReadRoleId(application.getReadRoleId());
        response.setIsActive(application.getIsActive());
        response.setCreatedBy(application.getCreatedBy());
        response.setCreatedAt(application.getCreatedAt());
        response.setUpdatedBy(application.getUpdatedBy());
        response.setUpdatedAt(application.getUpdatedAt());
        return response;
    }
}
