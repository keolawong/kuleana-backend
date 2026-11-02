package com.kuleanasystems.auth.service;

import com.kuleanasystems.auth.entity.User;
import com.kuleanasystems.auth.entity.UserApplication;
import com.kuleanasystems.auth.repository.UserApplicationRepository;
import com.kuleanasystems.auth.repository.UserRepository;
import com.kuleanasystems.auth.repository.UserRoleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
@RequiredArgsConstructor
public class AdminScopeService {

    private final UserRepository userRepository;
    private final UserApplicationRepository userApplicationRepository;
    private final UserRoleRepository userRoleRepository;

    public boolean isSuperAdmin(Authentication authentication) {
        if (authentication == null) return false;
        boolean fromToken = authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch(a -> a.equalsIgnoreCase("ROLE_SUPERADMIN") || a.equalsIgnoreCase("SUPERADMIN") || a.equalsIgnoreCase("ROLE_OWNER") || a.equalsIgnoreCase("OWNER"));
        if (fromToken) return true;

        String username = authentication.getName();
        return userRepository.findByUsername(username)
                .map(u -> userRoleRepository.findByUserId(u.getId()).stream()
                        .anyMatch(ur -> {
                            String rName = ur.getRole() != null ? ur.getRole().getName() : "";
                            return "SUPERADMIN".equalsIgnoreCase(rName) || "owner".equalsIgnoreCase(rName);
                        }))
                .orElse(false);
    }

    public List<String> getAdminScopes(Authentication authentication) {
        if (authentication == null) return Collections.emptyList();
        if (isSuperAdmin(authentication)) {
            return List.of("*");
        }

        String username = authentication.getName();
        Optional<User> userOpt = userRepository.findByUsername(username);
        if (userOpt.isEmpty()) return Collections.emptyList();

        User user = userOpt.get();
        List<UserApplication> userApps = userApplicationRepository.findByUserId(user.getId());

        boolean hasAdminAccess = authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch(a -> a.equalsIgnoreCase("ROLE_ADMIN") || a.equalsIgnoreCase("ADMIN") || a.equalsIgnoreCase("ROLE_SUPERADMIN") || a.equalsIgnoreCase("SUPERADMIN"))
                || userApps.stream().anyMatch(ua -> "admin".equalsIgnoreCase(ua.getAppName()) && "active".equalsIgnoreCase(ua.getStatus()));

        if (!hasAdminAccess) {
            return Collections.emptyList();
        }

        Set<String> scopes = new HashSet<>();
        for (UserApplication ua : userApps) {
            if ("active".equalsIgnoreCase(ua.getStatus()) && "ADMIN".equalsIgnoreCase(ua.getPrimaryRole())) {
                if (!"admin".equalsIgnoreCase(ua.getAppName())) {
                    scopes.add(ua.getAppName().toLowerCase());
                }
            }
        }

        boolean hasAssetAdminRole = authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch(a -> a.equalsIgnoreCase("ROLE_ASSET-IT") || a.equalsIgnoreCase("ROLE_ASSET-ADMIN"));
        if (hasAssetAdminRole) {
            scopes.add("assets");
        }

        return new ArrayList<>(scopes);
    }
}
