package com.kahua.tracker.controller;

import com.kahua.tracker.dto.RoleCreateRequest;
import com.kahua.tracker.dto.RoleResponse;
import com.kahua.tracker.dto.RoleUpdateRequest;
import com.kahua.tracker.entity.Role;
import com.kahua.tracker.repository.RoleRepository;
import com.kahua.tracker.service.AdminScopeService;
import com.kahua.tracker.service.RoleService;
import com.kahua.tracker.spec.RoleSpecification;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import lombok.RequiredArgsConstructor;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/roles")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
public class RoleController {

    private final RoleRepository roleRepository;
    private final RoleService roleService;
    private final com.kahua.tracker.repository.RolePageRepository rolePageRepository;
    private final AdminScopeService adminScopeService;

    @PostMapping
    @PreAuthorize("hasAnyRole('SUPERADMIN', 'OWNER', 'ADMIN', 'ASSET-IT')")
    public ResponseEntity<?> createRole(@RequestBody RoleCreateRequest request, Authentication authentication) {
        if (roleRepository.findByName(request.getName()).isPresent()) {
            return ResponseEntity.badRequest().body("Role with name '" + request.getName() + "' already exists.");
        }

        if (request.getName().equalsIgnoreCase("SUPERADMIN") || request.getName().equalsIgnoreCase("ROLE_SUPERADMIN")) {
            return ResponseEntity.badRequest().body("The SUPERADMIN role is reserved.");
        }

        boolean isSuper = adminScopeService.isSuperAdmin(authentication);
        List<String> scopes = adminScopeService.getAdminScopes(authentication);

        if (!isSuper && scopes.isEmpty()) {
            return ResponseEntity.status(403).body("You do not have administrative authority to create roles.");
        }

        String targetApp = request.getAppName();
        if (!isSuper) {
            if (targetApp == null || targetApp.isBlank()) {
                targetApp = scopes.get(0);
            } else if (!scopes.contains(targetApp.toLowerCase())) {
                return ResponseEntity.status(403).body("You can only create roles for your assigned sub-app: " + scopes);
            }
        } else if (targetApp == null || targetApp.isBlank()) {
            targetApp = "admin";
        }

        Role role = Role.builder()
                .name(request.getName().toUpperCase())
                .description(request.getDescription())
                .appName(targetApp.toLowerCase())
                .isActive(true)
                .build();

        Role savedRole = roleRepository.save(role);

        if (request.getPageIds() != null && !request.getPageIds().isEmpty()) {
            roleService.updateRolePages(savedRole.getId(), request.getPageIds());
        }

        URI location = ServletUriComponentsBuilder
                .fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(savedRole.getId())
                .toUri();

        return ResponseEntity.created(location).body(mapToRoleResponse(savedRole));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('SUPERADMIN', 'VIEWER', 'ADMIN', 'OWNER', 'ASSET-IT')")
    public ResponseEntity<List<RoleResponse>> getRoles(
            @RequestParam(required = false) UUID id,
            @RequestParam(required = false) String name,
            @RequestParam(required = false) String appName,
            @RequestParam(required = false) Boolean isActive,
            Authentication authentication) {

        Specification<Role> spec = Specification.where(RoleSpecification.hasId(id))
                .and(RoleSpecification.hasName(name))
                .and(RoleSpecification.hasIsActive(isActive));

        boolean isSuper = adminScopeService.isSuperAdmin(authentication);
        List<String> scopes = adminScopeService.getAdminScopes(authentication);

        if (!isSuper && !scopes.isEmpty()) {
            if (appName != null && !appName.isBlank()) {
                if (scopes.contains(appName.toLowerCase())) {
                    spec = spec.and(RoleSpecification.hasAppName(appName));
                } else {
                    return ResponseEntity.ok(List.of());
                }
            } else {
                spec = spec.and(RoleSpecification.hasAppNameIn(scopes));
            }
        } else if (appName != null && !appName.isBlank()) {
            spec = spec.and(RoleSpecification.hasAppName(appName));
        }

        List<Role> roles = roleRepository.findAll(spec);
        
        List<RoleResponse> response = roles.stream()
                .map(this::mapToRoleResponse)
                .collect(Collectors.toList());
        return ResponseEntity.ok(response);
    }

    @PatchMapping("/{id}")
    @PreAuthorize("hasAnyRole('SUPERADMIN', 'OWNER', 'ADMIN', 'ASSET-IT')")
    public ResponseEntity<RoleResponse> updateRole(@PathVariable UUID id, @RequestBody RoleUpdateRequest request) {
        return roleService.updateRole(id, request)
                .map(this::mapToRoleResponse)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/{id}/pages")
    @PreAuthorize("hasAnyRole('SUPERADMIN', 'VIEWER', 'ADMIN', 'OWNER', 'ASSET-IT')")
    public ResponseEntity<List<com.kahua.tracker.dto.RolePageResponse>> getRolePages(
            @PathVariable UUID id,
            @RequestParam(required = false) UUID pageId,
            @RequestParam(required = false) Boolean isActive,
            @RequestParam(required = false) String createdBy,
            @RequestParam(required = false) String updatedBy) {
            
        Specification<com.kahua.tracker.entity.RolePage> spec = Specification.where(com.kahua.tracker.spec.RolePageSpecification.hasRoleId(id))
                .and(com.kahua.tracker.spec.RolePageSpecification.hasPageId(pageId))
                .and(com.kahua.tracker.spec.RolePageSpecification.hasIsActive(isActive))
                .and(com.kahua.tracker.spec.RolePageSpecification.hasCreatedBy(createdBy))
                .and(com.kahua.tracker.spec.RolePageSpecification.hasUpdatedBy(updatedBy));

        List<com.kahua.tracker.entity.RolePage> rolePages = rolePageRepository.findAll(spec);

        if (rolePages.isEmpty()) {
            return ResponseEntity.noContent().build();
        }

        List<com.kahua.tracker.dto.RolePageResponse> response = rolePages.stream()
                .map(rp -> com.kahua.tracker.dto.RolePageResponse.builder()
                        .roleId(rp.getRoleId())
                        .pageId(rp.getPageId())
                        .isActive(rp.getIsActive())
                        .createdBy(rp.getCreatedBy())
                        .createdAt(rp.getCreatedAt())
                        .updatedBy(rp.getUpdatedBy())
                        .updatedAt(rp.getUpdatedAt())
                        .page(com.kahua.tracker.dto.PageResponse.builder()
                                .id(rp.getPage().getId())
                                .path(rp.getPage().getPath())
                                .name(rp.getPage().getName())
                                .description(rp.getPage().getDescription())
                                .build())
                        .build())
                .collect(Collectors.toList());

        return ResponseEntity.ok(response);
    }

    @PatchMapping("/{id}/pages")
    @PreAuthorize("hasAnyRole('SUPERADMIN', 'OWNER', 'ADMIN', 'ASSET-IT')")
    public ResponseEntity<Void> updateRolePages(@PathVariable UUID id, @RequestBody com.kahua.tracker.dto.RolePageUpdateRequest request) {
        if (roleRepository.findById(id).isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        roleService.updateRolePages(id, request.getPageIds());
        return ResponseEntity.noContent().build();
    }

    private RoleResponse mapToRoleResponse(Role role) {
        return RoleResponse.builder()
                .id(role.getId())
                .name(role.getName())
                .description(role.getDescription())
                .appName(role.getAppName())
                .isActive(role.getIsActive())
                .createdAt(role.getCreatedAt())
                .build();
    }
}
