package com.kuleanasystems.security;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

public final class TenantSecurityUtil {

    private TenantSecurityUtil() {}

    public static boolean isSuperAdmin(Authentication auth) {
        if (auth == null) return false;
        return auth.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch(a -> a.equalsIgnoreCase("ROLE_SUPERADMIN") || a.equalsIgnoreCase("SUPERADMIN"));
    }

    public static boolean isMemberOf(Authentication auth, UUID orgId) {
        if (auth == null || orgId == null) return false;
        if (isSuperAdmin(auth)) return true;
        String orgStr = orgId.toString().toLowerCase();
        return auth.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch(a -> a.equalsIgnoreCase("ORG_" + orgStr) || a.equalsIgnoreCase("ROLE_ORG_" + orgStr));
    }

    public static List<UUID> getUserOrgIds(Authentication auth) {
        if (auth == null) return List.of();
        return auth.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .filter(a -> a.toUpperCase().startsWith("ORG_"))
                .map(a -> {
                    try {
                        return UUID.fromString(a.substring(4).trim());
                    } catch (IllegalArgumentException e) {
                        return null;
                    }
                })
                .filter(id -> id != null)
                .distinct()
                .collect(Collectors.toList());
    }

    public static UUID resolveEffectiveOrgId(String orgIdHeader, Authentication auth) {
        boolean superAdmin = isSuperAdmin(auth);

        // 1. Explicit 'all' scope (superadmin only)
        if ("all".equalsIgnoreCase(orgIdHeader)) {
            if (superAdmin) {
                return null; // unscoped global view
            }
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only superadmin can query cross-organization data");
        }

        // 2. Specific org requested in header
        if (orgIdHeader != null && !orgIdHeader.trim().isEmpty()) {
            UUID requestedOrgId;
            try {
                requestedOrgId = UUID.fromString(orgIdHeader.trim());
            } catch (IllegalArgumentException e) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid X-Org-Id format");
            }

            if (superAdmin || isMemberOf(auth, requestedOrgId)) {
                return requestedOrgId;
            }
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied to organization: " + requestedOrgId);
        }

        // 3. Header not provided: fallback to user's assigned organization
        List<UUID> userOrgs = getUserOrgIds(auth);
        if (!userOrgs.isEmpty()) {
            return userOrgs.get(0);
        }

        if (superAdmin) {
            return null; // superadmin defaults to unscoped if no orgs assigned
        }

        throw new ResponseStatusException(HttpStatus.FORBIDDEN, "User does not belong to any organization");
    }
}
