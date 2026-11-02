package com.kuleanasystems.auth.controller;

import com.kuleanasystems.auth.dto.UserApplicationUpdateRequest;
import com.kuleanasystems.auth.dto.UserResponse;
import com.kuleanasystems.auth.dto.UserRoleUpdateRequest;
import com.kuleanasystems.auth.entity.Role;
import com.kuleanasystems.auth.entity.User;
import com.kuleanasystems.auth.entity.UserApplication;
import com.kuleanasystems.auth.entity.UserRole;
import com.kuleanasystems.auth.repository.RoleRepository;
import com.kuleanasystems.auth.repository.UserApplicationRepository;
import com.kuleanasystems.auth.repository.UserRepository;
import com.kuleanasystems.auth.repository.UserRoleRepository;
import com.kuleanasystems.auth.service.AdminScopeService;
import com.kuleanasystems.auth.service.PasswordValidator;
import com.kuleanasystems.auth.service.UserAuthCacheService;
import com.kuleanasystems.auth.spec.UserSpecification;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.time.OffsetDateTime;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/users")
@SecurityRequirement(name = "bearerAuth")
public class UserController {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private UserRoleRepository userRoleRepository;

    @Autowired
    private UserApplicationRepository userApplicationRepository;

    @Autowired
    private UserAuthCacheService userAuthCacheService;

    @Autowired
    private AdminScopeService adminScopeService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @GetMapping
    @PreAuthorize("hasAnyRole('SUPERADMIN', 'VIEWER', 'ADMIN', 'OWNER', 'ASSET-IT')")
    public ResponseEntity<List<UserResponse>> getUsers(
            @RequestParam(required = false) UUID id,
            @RequestParam(required = false) String username,
            Authentication authentication) {

        Specification<User> spec = Specification.where(UserSpecification.hasId(id))
                .and(UserSpecification.hasUsername(username));

        List<User> users = userRepository.findAll(spec);

        boolean isSuper = adminScopeService != null && adminScopeService.isSuperAdmin(authentication);
        List<String> scopes = adminScopeService != null ? adminScopeService.getAdminScopes(authentication) : List.of();

        if (!isSuper && !scopes.isEmpty()) {
            List<UserApplication> scopedApps = userApplicationRepository.findAll().stream()
                    .filter(ua -> scopes.contains(ua.getAppName().toLowerCase()) && "active".equalsIgnoreCase(ua.getStatus()))
                    .collect(Collectors.toList());
            Set<UUID> allowedUserIds = new HashSet<>(scopedApps.stream().map(UserApplication::getUserId).collect(Collectors.toSet()));

            List<UserRole> allRolesList = userRoleRepository.findAll();
            for (UserRole ur : allRolesList) {
                if (ur.getRole() != null) {
                    String rName = ur.getRole().getName() != null ? ur.getRole().getName().toLowerCase() : "";
                    boolean matches = scopes.stream().anyMatch(s -> rName.startsWith(s));
                    if (matches) {
                        allowedUserIds.add(ur.getUserId());
                    }
                }
            }
            users = users.stream().filter(u -> allowedUserIds.contains(u.getId())).collect(Collectors.toList());
        }

        List<UUID> userIds = users.stream().map(User::getId).collect(Collectors.toList());
        List<UserRole> allRoles = userIds.isEmpty() ? List.of() : userRoleRepository.findByUserIdIn(userIds);
        Map<UUID, List<UserRole>> rolesByUserId = allRoles.stream()
                .collect(Collectors.groupingBy(UserRole::getUserId));

        List<UserApplication> allApps = userIds.isEmpty() ? List.of() : userApplicationRepository.findByUserIdIn(userIds);
        Map<UUID, List<UserApplication>> appsByUserId = allApps.stream()
                .collect(Collectors.groupingBy(UserApplication::getUserId));

        List<UserResponse> response = users.stream()
                .map(user -> mapToUserResponse(
                        user,
                        rolesByUserId.getOrDefault(user.getId(), List.of()),
                        appsByUserId.getOrDefault(user.getId(), List.of())
                ))
                .collect(Collectors.toList());
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{userId}/applications")
    @PreAuthorize("hasAnyRole('SUPERADMIN', 'OWNER', 'ADMIN', 'ASSET-IT')")
    @Transactional
    public ResponseEntity<?> updateUserApplications(
            @PathVariable UUID userId,
            @RequestBody UserApplicationUpdateRequest request,
            Authentication authentication) {

        Optional<User> userOptional = userRepository.findById(userId);
        if (userOptional.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        boolean isSuper = adminScopeService != null && adminScopeService.isSuperAdmin(authentication);
        List<String> scopes = adminScopeService != null ? adminScopeService.getAdminScopes(authentication) : List.of();

        if (!isSuper && scopes.isEmpty()) {
            return ResponseEntity.status(403).body("You do not have administrative authority to modify applications.");
        }

        List<UserApplication> existingApps = userApplicationRepository.findByUserId(userId);
        List<UserApplication> preservedApps = new ArrayList<>();
        if (!isSuper) {
            for (UserApplication existing : existingApps) {
                if (!scopes.contains(existing.getAppName().toLowerCase())) {
                    preservedApps.add(existing);
                }
            }
        }

        userApplicationRepository.deleteByUserId(userId);

        List<UserApplication> newApps = new ArrayList<>(preservedApps);
        List<String> rolesToAssign = new ArrayList<>();

        if (!isSuper) {
            List<UserRole> existingRoles = userRoleRepository.findByUserId(userId);
            for (UserRole ur : existingRoles) {
                roleRepository.findById(ur.getRoleId()).ifPresent(r -> {
                    String app = r.getAppName();
                    if (app != null && !scopes.contains(app.toLowerCase())) {
                        rolesToAssign.add(r.getName());
                    } else if (app == null) {
                        String name = r.getName().toLowerCase();
                        boolean isMyScope = scopes.stream().anyMatch(s -> name.startsWith(s));
                        if (!isMyScope && !name.contains("admin") && !name.contains("superadmin")) {
                            rolesToAssign.add(r.getName());
                        }
                    }
                });
            }
        }

        if (request.getApplications() != null && !request.getApplications().isEmpty()) {
            for (var app : request.getApplications()) {
                String appName = app.getAppName().toLowerCase();
                if (!isSuper && !scopes.contains(appName)) {
                    continue;
                }

                if ("active".equalsIgnoreCase(app.getStatus())) {
                    newApps.add(UserApplication.builder()
                            .userId(userId)
                            .appName(appName)
                            .status("active")
                            .primaryRole(app.getPrimaryRole() != null ? app.getPrimaryRole().toUpperCase() : "DEFAULT")
                            .build());

                    String roleTier = app.getPrimaryRole() != null ? app.getPrimaryRole().toUpperCase() : "";

                    if ("assets".equals(appName)) {
                        if ("LEAD".equals(roleTier)) {
                            rolesToAssign.add("ASSET-LEAD");
                            rolesToAssign.add("ASSET-READ");
                            rolesToAssign.add("ASSET-WRITE");
                        } else if ("IT".equals(roleTier)) {
                            rolesToAssign.add("ASSET-IT");
                            rolesToAssign.add("ASSET-READ");
                            rolesToAssign.add("ASSET-WRITE");
                        } else if ("ADMIN".equals(roleTier)) {
                            rolesToAssign.add("ASSET-IT");
                            rolesToAssign.add("ASSET-READ");
                            rolesToAssign.add("ASSET-WRITE");
                            if (isSuper) {
                                rolesToAssign.add("admin");
                            }
                        } else {
                            rolesToAssign.add("ASSET-READ");
                            rolesToAssign.add("ASSET-WRITE");
                        }
                    } else if ("admin".equals(appName) && isSuper) {
                        rolesToAssign.add("admin");
                    } else if ("tracker".equals(appName)) {
                        if ("OWNER".equals(roleTier)) {
                            rolesToAssign.add("owner");
                        } else if ("EDITOR".equals(roleTier)) {
                            rolesToAssign.add("editor");
                        } else {
                            rolesToAssign.add("viewer");
                        }
                    }
                }
            }
        }

        if (!newApps.isEmpty()) {
            userApplicationRepository.saveAll(newApps);
        }

        userRoleRepository.deleteByUserId(userId);
        List<UserRole> newRoles = new ArrayList<>();
        for (String rName : rolesToAssign.stream().distinct().toList()) {
            roleRepository.findByName(rName).ifPresent(r -> {
                newRoles.add(UserRole.builder()
                        .userId(userId)
                        .roleId(r.getId())
                        .status("active")
                        .build());
            });
        }
        if (!newRoles.isEmpty()) {
            userRoleRepository.saveAll(newRoles);
        }

        userAuthCacheService.evictUser(userOptional.get().getUsername());
        return ResponseEntity.ok(Map.of("message", "User application access updated successfully."));
    }

    @PutMapping("/{userId}/roles")
    @PreAuthorize("hasAnyRole('SUPERADMIN', 'OWNER', 'ADMIN', 'ASSET-IT')")
    @Transactional
    public ResponseEntity<?> updateUserRoles(
            @PathVariable UUID userId,
            @RequestBody UserRoleUpdateRequest request,
            Authentication authentication) {

        Optional<User> userOptional = userRepository.findById(userId);
        if (userOptional.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        boolean isSuper = adminScopeService != null && adminScopeService.isSuperAdmin(authentication);
        List<String> scopes = adminScopeService != null ? adminScopeService.getAdminScopes(authentication) : List.of();

        if (!isSuper && scopes.isEmpty()) {
            return ResponseEntity.status(403).body("You do not have administrative authority to modify roles.");
        }

        List<UserRole> existingRoles = userRoleRepository.findByUserId(userId);
        List<UserRole> preservedRoles = new ArrayList<>();

        if (!isSuper) {
            for (UserRole ur : existingRoles) {
                roleRepository.findById(ur.getRoleId()).ifPresent(r -> {
                    String app = r.getAppName();
                    if (app != null && !scopes.contains(app.toLowerCase())) {
                        preservedRoles.add(ur);
                    }
                });
            }
        }

        userRoleRepository.deleteByUserId(userId);

        List<UserRole> newRoles = new ArrayList<>(preservedRoles);
        if (request.getRoles() != null && !request.getRoles().isEmpty()) {
            for (var roleAssignment : request.getRoles()) {
                Role role = roleRepository.findById(roleAssignment.getRoleId())
                        .orElseThrow(() -> new IllegalArgumentException("Role not found: " + roleAssignment.getRoleId()));

                if (!isSuper) {
                    if (role.getAppName() != null && !scopes.contains(role.getAppName().toLowerCase())) {
                        continue;
                    }
                    if (role.getName().equalsIgnoreCase("SUPERADMIN") || role.getName().equalsIgnoreCase("admin")) {
                        continue;
                    }
                }

                newRoles.add(UserRole.builder()
                        .userId(userId)
                        .roleId(roleAssignment.getRoleId())
                        .status(roleAssignment.getStatus() != null ? roleAssignment.getStatus() : "inactive")
                        .build());
            }
        }

        if (!newRoles.isEmpty()) {
            userRoleRepository.saveAll(newRoles);
        }

        userAuthCacheService.evictUser(userOptional.get().getUsername());
        return ResponseEntity.ok("User roles updated successfully.");
    }

    @PostMapping("/{userId}/reset-password")
    @PreAuthorize("hasAnyRole('SUPERADMIN', 'OWNER', 'ADMIN', 'ASSET-IT')")
    public ResponseEntity<?> resetPassword(@PathVariable UUID userId, @RequestBody Map<String, String> request, Authentication authentication) {
        Optional<User> userOptional = userRepository.findById(userId);
        if (userOptional.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        boolean isSuper = adminScopeService != null && adminScopeService.isSuperAdmin(authentication);
        List<String> scopes = adminScopeService != null ? adminScopeService.getAdminScopes(authentication) : List.of();

        if (!isSuper) {
            List<UserApplication> userApps = userApplicationRepository.findByUserId(userId);
            boolean belongsToScope = userApps.stream().anyMatch(ua -> scopes.contains(ua.getAppName().toLowerCase()));
            if (!belongsToScope) {
                return ResponseEntity.status(403).body(Map.of("message", "You cannot reset password for users outside your sub-app."));
            }
        }

        User user = userOptional.get();
        String tempPassword = request.get("newPassword") != null ? request.get("newPassword") : request.get("temporaryPassword");

        try {
            PasswordValidator.validate(tempPassword, user.getUsername(), user.getFirstName(), user.getLastName());
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }

        user.setPasswordHash(passwordEncoder.encode(tempPassword));
        user.setForcePasswordChange(true);
        user.setPasswordResetExpiresAt(OffsetDateTime.now().plusHours(24));
        user.setFailedLoginAttempts(0);
        user.setLockoutRound(0);
        user.setAccountLockedUntil(null);

        userRepository.save(user);
        userAuthCacheService.evictUser(user.getUsername());

        return ResponseEntity.ok(Map.of("message", "Password reset successfully."));
    }

    @PostMapping("/{userId}/unlock")
    @PreAuthorize("hasAnyRole('SUPERADMIN', 'OWNER', 'ADMIN', 'ASSET-IT')")
    public ResponseEntity<?> unlockAccount(@PathVariable UUID userId, Authentication authentication) {
        Optional<User> userOptional = userRepository.findById(userId);
        if (userOptional.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        boolean isSuper = adminScopeService != null && adminScopeService.isSuperAdmin(authentication);
        List<String> scopes = adminScopeService != null ? adminScopeService.getAdminScopes(authentication) : List.of();

        if (!isSuper) {
            List<UserApplication> userApps = userApplicationRepository.findByUserId(userId);
            boolean belongsToScope = userApps.stream().anyMatch(ua -> scopes.contains(ua.getAppName().toLowerCase()));
            if (!belongsToScope) {
                return ResponseEntity.status(403).body("You cannot unlock accounts for users outside your sub-app.");
            }
        }

        User user = userOptional.get();
        user.setFailedLoginAttempts(0);
        user.setLockoutRound(0);
        user.setAccountLockedUntil(null);

        userRepository.save(user);

        return ResponseEntity.ok("Account unlocked successfully.");
    }

    private UserResponse mapToUserResponse(User user, List<UserRole> userRoles, List<UserApplication> userApps) {
        List<UserResponse.RoleInfo> roleInfos = userRoles.stream()
                .map(ur -> UserResponse.RoleInfo.builder()
                        .roleId(ur.getRole().getId())
                        .name(ur.getRole().getName())
                        .status(ur.getStatus())
                        .build())
                .collect(Collectors.toList());

        List<UserResponse.ApplicationInfo> appInfos = new ArrayList<>(userApps.stream()
                .map(ua -> UserResponse.ApplicationInfo.builder()
                        .appName(ua.getAppName())
                        .status(ua.getStatus())
                        .primaryRole(ua.getPrimaryRole())
                        .build())
                .collect(Collectors.toList()));

        List<String> activeAppNames = userApps.stream()
                .filter(ua -> "active".equalsIgnoreCase(ua.getStatus()))
                .map(UserApplication::getAppName)
                .collect(Collectors.toList());

        if (activeAppNames.isEmpty() && !roleInfos.isEmpty()) {
            Set<String> inferred = new HashSet<>();
            for (UserResponse.RoleInfo r : roleInfos) {
                String rName = r.getName() != null ? r.getName().toLowerCase() : "";
                if (rName.equals("admin") || rName.equals("superadmin") || rName.equals("owner")) inferred.add("admin");
                if (rName.startsWith("asset")) inferred.add("assets");
                if (rName.equals("viewer") || rName.equals("editor") || rName.equals("owner")) inferred.add("tracker");
            }
            if ("keolawong".equalsIgnoreCase(user.getUsername())) {
                inferred.add("admin");
                inferred.add("assets");
            }
            activeAppNames = new ArrayList<>(inferred);

            for (String aName : activeAppNames) {
                String primary = "DEFAULT";
                if ("assets".equals(aName)) {
                    primary = roleInfos.stream().anyMatch(r -> r.getName() != null && r.getName().toUpperCase().contains("LEAD")) ? "LEAD" :
                              (roleInfos.stream().anyMatch(r -> r.getName() != null && r.getName().toUpperCase().contains("IT")) ? "IT" : "OPERATOR");
                } else if ("tracker".equals(aName)) {
                    primary = roleInfos.stream().anyMatch(r -> r.getName() != null && r.getName().equalsIgnoreCase("owner")) ? "OWNER" :
                              (roleInfos.stream().anyMatch(r -> r.getName() != null && r.getName().equalsIgnoreCase("editor")) ? "EDITOR" : "VIEWER");
                } else if ("admin".equals(aName)) {
                    primary = "ADMIN";
                }
                appInfos.add(UserResponse.ApplicationInfo.builder()
                        .appName(aName)
                        .status("active")
                        .primaryRole(primary)
                        .build());
            }
        }

        boolean isLocked = user.getAccountLockedUntil() != null && user.getAccountLockedUntil().isAfter(OffsetDateTime.now());

        return UserResponse.builder()
                .id(user.getId())
                .username(user.getUsername())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .locked(isLocked)
                .createdAt(user.getCreatedAt())
                .roles(roleInfos)
                .applications(appInfos)
                .apps(activeAppNames)
                .build();
    }
}
