package com.kuleanasystems.auth.service;

import com.kuleanasystems.auth.dto.*;
import com.kuleanasystems.auth.entity.Organization;
import com.kuleanasystems.auth.entity.User;
import com.kuleanasystems.auth.entity.UserOrganization;
import com.kuleanasystems.auth.repository.OrganizationRepository;
import com.kuleanasystems.auth.repository.UserOrganizationRepository;
import com.kuleanasystems.auth.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class OrganizationService {

    private final OrganizationRepository organizationRepository;
    private final UserOrganizationRepository userOrganizationRepository;
    private final UserRepository userRepository;
    private final AdminScopeService adminScopeService;

    @Transactional(readOnly = true)
    public List<OrganizationDto> getAllOrganizations(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Not authenticated");
        }

        boolean isSuper = adminScopeService != null && adminScopeService.isSuperAdmin(authentication);
        User currentUser = userRepository.findByUsername(authentication.getName()).orElse(null);

        if (isSuper) {
            List<Organization> allOrgs = organizationRepository.findAll();
            Map<UUID, String> roleByOrgId = currentUser != null
                    ? userOrganizationRepository.findByUserId(currentUser.getId()).stream()
                            .collect(Collectors.toMap(UserOrganization::getOrgId, UserOrganization::getRole, (a, b) -> a))
                    : Collections.emptyMap();

            return allOrgs.stream()
                    .map(org -> toDto(org, roleByOrgId.getOrDefault(org.getId(), "ORG_ADMIN")))
                    .collect(Collectors.toList());
        }

        if (currentUser == null) {
            return Collections.emptyList();
        }

        List<UserOrganization> userOrgs = userOrganizationRepository.findByUserId(currentUser.getId());
        if (userOrgs.isEmpty()) {
            return Collections.emptyList();
        }

        List<UUID> orgIds = userOrgs.stream().map(UserOrganization::getOrgId).collect(Collectors.toList());
        Map<UUID, Organization> orgMap = organizationRepository.findAllById(orgIds).stream()
                .collect(Collectors.toMap(Organization::getId, o -> o));

        return userOrgs.stream()
                .map(uo -> {
                    Organization org = orgMap.get(uo.getOrgId());
                    return org != null ? toDto(org, uo.getRole()) : null;
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public OrganizationDto getOrganizationById(UUID id, Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Not authenticated");
        }

        Organization org = organizationRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Organization not found"));

        boolean isSuper = adminScopeService != null && adminScopeService.isSuperAdmin(authentication);
        User currentUser = userRepository.findByUsername(authentication.getName()).orElse(null);

        if (isSuper) {
            String role = (currentUser != null)
                    ? userOrganizationRepository.findByUserIdAndOrgId(currentUser.getId(), id)
                            .map(UserOrganization::getRole).orElse("ORG_ADMIN")
                    : "ORG_ADMIN";
            return toDto(org, role);
        }

        if (currentUser == null) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied to organization");
        }

        UserOrganization uo = userOrganizationRepository.findByUserIdAndOrgId(currentUser.getId(), id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied to organization"));

        return toDto(org, uo.getRole());
    }

    @Transactional
    public OrganizationDto createOrganization(OrganizationCreateRequest request, Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Not authenticated");
        }

        String rawSlug = request.getSlug() != null && !request.getSlug().trim().isEmpty()
                ? request.getSlug().trim().toLowerCase()
                : (request.getName() != null ? request.getName().trim().toLowerCase().replaceAll("[^a-z0-9]+", "-").replaceAll("^-|-$", "") : "");

        if (rawSlug.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Organization name or slug is required");
        }

        String normalizedSlug = rawSlug;
        if (organizationRepository.existsBySlug(normalizedSlug)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Organization with slug '" + normalizedSlug + "' already exists");
        }

        Organization org = Organization.builder()
                .slug(normalizedSlug)
                .name(request.getName() != null && !request.getName().trim().isEmpty() ? request.getName().trim() : normalizedSlug)
                .description(request.getDescription())
                .logoUrl(request.getLogoUrl())
                .status(request.getStatus() != null && !request.getStatus().trim().isEmpty() ? request.getStatus().trim() : "active")
                .build();

        org = organizationRepository.save(org);

        User currentUser = userRepository.findByUsername(authentication.getName()).orElse(null);
        if (currentUser != null) {
            UserOrganization uo = UserOrganization.builder()
                    .userId(currentUser.getId())
                    .orgId(org.getId())
                    .role("ORG_ADMIN")
                    .build();
            userOrganizationRepository.save(uo);
        }

        return toDto(org, "ORG_ADMIN");
    }

    @Transactional
    public OrganizationDto updateOrganization(UUID id, OrganizationUpdateRequest request, Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Not authenticated");
        }

        Organization org = organizationRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Organization not found"));

        boolean isSuper = adminScopeService != null && adminScopeService.isSuperAdmin(authentication);
        User currentUser = userRepository.findByUsername(authentication.getName()).orElse(null);

        boolean isOrgAdmin = false;
        String userRole = "MEMBER";
        if (currentUser != null) {
            Optional<UserOrganization> uo = userOrganizationRepository.findByUserIdAndOrgId(currentUser.getId(), id);
            if (uo.isPresent()) {
                userRole = uo.get().getRole();
                isOrgAdmin = "ORG_ADMIN".equalsIgnoreCase(userRole);
            }
        }

        if (!isSuper && !isOrgAdmin) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only organization admins or superadmins can update organizations");
        }

        if (request.getName() != null && !request.getName().trim().isEmpty()) {
            org.setName(request.getName().trim());
        }
        if (request.getDescription() != null) {
            org.setDescription(request.getDescription());
        }
        if (request.getLogoUrl() != null) {
            org.setLogoUrl(request.getLogoUrl());
        }
        if (request.getStatus() != null && !request.getStatus().trim().isEmpty()) {
            org.setStatus(request.getStatus().trim());
        }

        org = organizationRepository.save(org);
        return toDto(org, isSuper ? "ORG_ADMIN" : userRole);
    }

    @Transactional(readOnly = true)
    public List<UserOrganizationResponse> getMembers(UUID orgId, Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Not authenticated");
        }

        Organization org = organizationRepository.findById(orgId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Organization not found"));

        boolean isSuper = adminScopeService != null && adminScopeService.isSuperAdmin(authentication);
        User currentUser = userRepository.findByUsername(authentication.getName()).orElse(null);

        if (!isSuper) {
            if (currentUser == null || userOrganizationRepository.findByUserIdAndOrgId(currentUser.getId(), orgId).isEmpty()) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied to organization members");
            }
        }

        List<UserOrganization> members = userOrganizationRepository.findByOrgId(orgId);
        if (members.isEmpty()) {
            return Collections.emptyList();
        }

        List<UUID> userIds = members.stream().map(UserOrganization::getUserId).collect(Collectors.toList());
        Map<UUID, User> userMap = userRepository.findAllById(userIds).stream()
                .collect(Collectors.toMap(User::getId, u -> u));

        return members.stream().map(uo -> {
            User u = userMap.get(uo.getUserId());
            return UserOrganizationResponse.builder()
                    .id(uo.getId())
                    .userId(uo.getUserId())
                    .username(u != null ? u.getUsername() : null)
                    .firstName(u != null ? u.getFirstName() : null)
                    .lastName(u != null ? u.getLastName() : null)
                    .orgId(org.getId())
                    .orgName(org.getName())
                    .orgSlug(org.getSlug())
                    .role(uo.getRole())
                    .createdAt(uo.getCreatedAt())
                    .updatedAt(uo.getUpdatedAt())
                    .build();
        }).collect(Collectors.toList());
    }

    @Transactional
    public UserOrganizationResponse addMember(UUID orgId, AddMemberRequest request, Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Not authenticated");
        }

        Organization org = organizationRepository.findById(orgId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Organization not found"));

        boolean isSuper = adminScopeService != null && adminScopeService.isSuperAdmin(authentication);
        User currentUser = userRepository.findByUsername(authentication.getName()).orElse(null);

        boolean isOrgAdmin = false;
        if (currentUser != null) {
            isOrgAdmin = userOrganizationRepository.findByUserIdAndOrgId(currentUser.getId(), orgId)
                    .map(uo -> "ORG_ADMIN".equalsIgnoreCase(uo.getRole()))
                    .orElse(false);
        }

        if (!isSuper && !isOrgAdmin) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only organization admins or superadmins can add/update members");
        }

        User targetUser = null;
        if (request.getUserId() != null) {
            targetUser = userRepository.findById(request.getUserId()).orElse(null);
        }
        if (targetUser == null && request.getUsername() != null && !request.getUsername().trim().isEmpty()) {
            targetUser = userRepository.findByUsername(request.getUsername().trim()).orElse(null);
        }

        if (targetUser == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Target user not found");
        }

        String role = (request.getRole() != null && !request.getRole().trim().isEmpty())
                ? request.getRole().trim().toUpperCase()
                : "MEMBER";

        Optional<UserOrganization> existing = userOrganizationRepository.findByUserIdAndOrgId(targetUser.getId(), orgId);
        UserOrganization uo;
        if (existing.isPresent()) {
            uo = existing.get();
            uo.setRole(role);
        } else {
            uo = UserOrganization.builder()
                    .userId(targetUser.getId())
                    .orgId(orgId)
                    .role(role)
                    .build();
        }

        uo = userOrganizationRepository.save(uo);

        return UserOrganizationResponse.builder()
                .id(uo.getId())
                .userId(targetUser.getId())
                .username(targetUser.getUsername())
                .firstName(targetUser.getFirstName())
                .lastName(targetUser.getLastName())
                .orgId(org.getId())
                .orgName(org.getName())
                .orgSlug(org.getSlug())
                .role(uo.getRole())
                .createdAt(uo.getCreatedAt())
                .updatedAt(uo.getUpdatedAt())
                .build();
    }

    @Transactional
    public void removeMember(UUID orgId, UUID userId, Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Not authenticated");
        }

        Organization org = organizationRepository.findById(orgId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Organization not found"));

        boolean isSuper = adminScopeService != null && adminScopeService.isSuperAdmin(authentication);
        User currentUser = userRepository.findByUsername(authentication.getName()).orElse(null);

        boolean isOrgAdmin = false;
        if (currentUser != null) {
            isOrgAdmin = userOrganizationRepository.findByUserIdAndOrgId(currentUser.getId(), orgId)
                    .map(uo -> "ORG_ADMIN".equalsIgnoreCase(uo.getRole()))
                    .orElse(false);
        }

        boolean isSelf = currentUser != null && currentUser.getId().equals(userId);

        if (!isSuper && !isOrgAdmin && !isSelf) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only organization admins, superadmins, or the user themselves can remove membership");
        }

        userOrganizationRepository.deleteByUserIdAndOrgId(userId, orgId);
    }

    @Transactional
    public OrganizationDto setDefaultOrganization(UUID orgId, Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Not authenticated");
        }

        boolean isSuper = adminScopeService != null && adminScopeService.isSuperAdmin(authentication);
        if (!isSuper) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only super administrators can set the default organization");
        }

        Organization targetOrg = organizationRepository.findById(orgId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Organization not found"));

        // Unset any existing default
        List<Organization> existingDefaults = organizationRepository.findAll().stream()
                .filter(o -> Boolean.TRUE.equals(o.getIsDefault()))
                .collect(Collectors.toList());
        for (Organization o : existingDefaults) {
            o.setIsDefault(false);
        }
        if (!existingDefaults.isEmpty()) {
            organizationRepository.saveAll(existingDefaults);
            organizationRepository.flush();
        }

        targetOrg.setIsDefault(true);
        Organization saved = organizationRepository.save(targetOrg);

        return toDto(saved, "ORG_ADMIN");
    }

    public OrganizationDto toDto(Organization org, String role) {
        return OrganizationDto.builder()
                .id(org.getId())
                .slug(org.getSlug())
                .name(org.getName())
                .description(org.getDescription())
                .logoUrl(org.getLogoUrl())
                .status(org.getStatus())
                .role(role)
                .isDefault(Boolean.TRUE.equals(org.getIsDefault()))
                .createdAt(org.getCreatedAt())
                .updatedAt(org.getUpdatedAt())
                .build();
    }
}
