package com.kahua.tracker.controller;

import com.kahua.tracker.dto.UserResponse;
import com.kahua.tracker.dto.UserRoleUpdateRequest;
import com.kahua.tracker.entity.Role;
import com.kahua.tracker.entity.User;
import com.kahua.tracker.entity.UserRole;
import com.kahua.tracker.repository.RoleRepository;
import com.kahua.tracker.repository.UserRepository;
import com.kahua.tracker.repository.UserRoleRepository;
import com.kahua.tracker.service.UserAuthCacheService;
import com.kahua.tracker.spec.UserSpecification;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
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
    private UserAuthCacheService userAuthCacheService;

    @GetMapping
    @PreAuthorize("hasAnyRole('VIEWER', 'ADMIN', 'OWNER', 'ASSET-IT')")
    public ResponseEntity<List<UserResponse>> getUsers(
            @RequestParam(required = false) UUID id,
            @RequestParam(required = false) String username) {
        
        Specification<User> spec = Specification.where(UserSpecification.hasId(id))
                .and(UserSpecification.hasUsername(username));

        List<User> users = userRepository.findAll(spec);
        
        List<UUID> userIds = users.stream().map(User::getId).collect(Collectors.toList());
        List<UserRole> allRoles = userIds.isEmpty() ? List.of() : userRoleRepository.findByUserIdIn(userIds);
        Map<UUID, List<UserRole>> rolesByUserId = allRoles.stream()
                .collect(Collectors.groupingBy(UserRole::getUserId));
        
        List<UserResponse> response = users.stream()
                .map(user -> mapToUserResponse(user, rolesByUserId.getOrDefault(user.getId(), List.of())))
                .collect(Collectors.toList());
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{userId}/roles")
    @PreAuthorize("hasAnyRole('OWNER', 'ADMIN', 'ASSET-IT')")
    @org.springframework.transaction.annotation.Transactional
    public ResponseEntity<?> updateUserRoles(
            @PathVariable UUID userId,
            @RequestBody UserRoleUpdateRequest request) {

        Optional<User> userOptional = userRepository.findById(userId);
        if (userOptional.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        // 1. Fetch and delete current roles atomically via direct DML delete
        userRoleRepository.deleteByUserId(userId);

        // 2. Add the new roles with their specified statuses
        if (request.getRoles() != null && !request.getRoles().isEmpty()) {
            List<UserRole> newRoles = request.getRoles().stream().map(roleAssignment -> {
                Role role = roleRepository.findById(roleAssignment.getRoleId())
                        .orElseThrow(() -> new IllegalArgumentException("Role not found: " + roleAssignment.getRoleId()));

                return UserRole.builder()
                        .userId(userId)
                        .roleId(roleAssignment.getRoleId())
                        .status(roleAssignment.getStatus() != null ? roleAssignment.getStatus() : "inactive") 
                        .build();
            }).collect(Collectors.toList());

            userRoleRepository.saveAll(newRoles);
        }

        return ResponseEntity.ok("User roles updated successfully.");
    }

    @Autowired
    private org.springframework.security.crypto.password.PasswordEncoder passwordEncoder;

    @PostMapping("/{userId}/reset-password")
    @PreAuthorize("hasAnyRole('OWNER', 'ADMIN', 'ASSET-IT')")
    public ResponseEntity<?> resetPassword(@PathVariable UUID userId, @RequestBody Map<String, String> request) {
        Optional<User> userOptional = userRepository.findById(userId);
        if (userOptional.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        User user = userOptional.get();
        String tempPassword = request.get("newPassword") != null ? request.get("newPassword") : request.get("temporaryPassword");

        try {
            com.kahua.tracker.security.PasswordValidator.validate(tempPassword, user.getUsername(), user.getFirstName(), user.getLastName());
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }

        user.setPasswordHash(passwordEncoder.encode(tempPassword));
        user.setForcePasswordChange(true);
        user.setPasswordResetExpiresAt(java.time.OffsetDateTime.now().plusHours(24));
        user.setFailedLoginAttempts(0);
        user.setLockoutRound(0);
        user.setAccountLockedUntil(null);

        userRepository.save(user);
        userAuthCacheService.evictUser(user.getUsername());

        return ResponseEntity.ok(Map.of("message", "Password reset successfully."));
    }

    @PostMapping("/{userId}/unlock")
    @PreAuthorize("hasAnyRole('OWNER', 'ADMIN', 'ASSET-IT')")
    public ResponseEntity<?> unlockAccount(@PathVariable UUID userId) {
        Optional<User> userOptional = userRepository.findById(userId);
        if (userOptional.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        User user = userOptional.get();
        user.setFailedLoginAttempts(0);
        user.setLockoutRound(0);
        user.setAccountLockedUntil(null);

        userRepository.save(user);

        return ResponseEntity.ok("Account unlocked successfully.");
    }

    private UserResponse mapToUserResponse(User user, List<UserRole> userRoles) {
        List<UserResponse.RoleInfo> roleInfos = userRoles.stream()
                .map(ur -> UserResponse.RoleInfo.builder()
                        .roleId(ur.getRole().getId())
                        .name(ur.getRole().getName())
                        .status(ur.getStatus())
                        .build())
                .collect(Collectors.toList());

        boolean isLocked = user.getAccountLockedUntil() != null && user.getAccountLockedUntil().isAfter(java.time.OffsetDateTime.now());

        return UserResponse.builder()
                .id(user.getId())
                .username(user.getUsername())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .locked(isLocked)
                .createdAt(user.getCreatedAt())
                .roles(roleInfos)
                .build();
    }
}
