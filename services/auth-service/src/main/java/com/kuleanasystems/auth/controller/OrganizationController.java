package com.kuleanasystems.auth.controller;

import com.kuleanasystems.auth.dto.*;
import com.kuleanasystems.auth.service.OrganizationService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/organizations")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
public class OrganizationController {

    private final OrganizationService organizationService;

    @GetMapping
    public ResponseEntity<List<OrganizationDto>> listOrganizations(Authentication authentication) {
        return ResponseEntity.ok(organizationService.getAllOrganizations(authentication));
    }

    @GetMapping("/{id}")
    public ResponseEntity<OrganizationDto> getOrganization(@PathVariable UUID id, Authentication authentication) {
        return ResponseEntity.ok(organizationService.getOrganizationById(id, authentication));
    }

    @PostMapping
    public ResponseEntity<OrganizationDto> createOrganization(
            @RequestBody OrganizationCreateRequest request,
            Authentication authentication) {
        return ResponseEntity.ok(organizationService.createOrganization(request, authentication));
    }

    @PutMapping("/{id}")
    public ResponseEntity<OrganizationDto> updateOrganization(
            @PathVariable UUID id,
            @RequestBody OrganizationUpdateRequest request,
            Authentication authentication) {
        return ResponseEntity.ok(organizationService.updateOrganization(id, request, authentication));
    }

    @PutMapping("/{id}/default")
    public ResponseEntity<OrganizationDto> setDefaultOrganization(
            @PathVariable UUID id,
            Authentication authentication) {
        return ResponseEntity.ok(organizationService.setDefaultOrganization(id, authentication));
    }

    @GetMapping("/{id}/members")
    public ResponseEntity<List<UserOrganizationResponse>> getMembers(
            @PathVariable UUID id,
            Authentication authentication) {
        return ResponseEntity.ok(organizationService.getMembers(id, authentication));
    }

    @PostMapping("/{id}/members")
    public ResponseEntity<UserOrganizationResponse> addMember(
            @PathVariable UUID id,
            @RequestBody AddMemberRequest request,
            Authentication authentication) {
        return ResponseEntity.ok(organizationService.addMember(id, request, authentication));
    }

    @DeleteMapping("/{id}/members/{userId}")
    public ResponseEntity<?> removeMember(
            @PathVariable UUID id,
            @PathVariable UUID userId,
            Authentication authentication) {
        organizationService.removeMember(id, userId, authentication);
        return ResponseEntity.noContent().build();
    }
}
