package com.kuleanasystems.auth.service;

import com.kuleanasystems.auth.dto.RoleUpdateRequest;
import com.kuleanasystems.auth.entity.Role;
import com.kuleanasystems.auth.entity.RolePage;
import com.kuleanasystems.auth.repository.PageRepository;
import com.kuleanasystems.auth.repository.RolePageRepository;
import com.kuleanasystems.auth.repository.RoleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class RoleService {

    private final RoleRepository roleRepository;
    private final RolePageRepository rolePageRepository;
    private final PageRepository pageRepository;

    @Transactional
    public Optional<Role> updateRole(UUID id, RoleUpdateRequest request) {
        Optional<Role> existingRole = roleRepository.findById(id);
        if (existingRole.isEmpty()) {
            return Optional.empty();
        }

        Role roleToUpdate = existingRole.get();

        request.getName().ifPresent(roleToUpdate::setName);
        request.getDescription().ifPresent(roleToUpdate::setDescription);
        request.getAppName().ifPresent(roleToUpdate::setAppName);
        request.getIsActive().ifPresent(roleToUpdate::setIsActive);

        return Optional.of(roleRepository.save(roleToUpdate));
    }

    @Transactional
    public void updateRolePages(UUID roleId, List<UUID> newPageIds) {
        List<RolePage> existingRolePages = rolePageRepository.findByRoleId(roleId);

        existingRolePages.forEach(rp -> rp.setIsActive(false));

        if (newPageIds != null) {
            for (UUID pageId : newPageIds) {
                Optional<RolePage> existing = existingRolePages.stream()
                        .filter(rp -> rp.getPageId().equals(pageId))
                        .findFirst();

                if (existing.isPresent()) {
                    existing.get().setIsActive(true);
                } else {
                    if (pageRepository.existsById(pageId)) {
                        RolePage newRp = RolePage.builder()
                                .roleId(roleId)
                                .pageId(pageId)
                                .isActive(true)
                                .build();
                        rolePageRepository.save(newRp);
                    }
                }
            }
        }
    }
}
