package com.kahua.tracker.service;

import com.kahua.tracker.dto.RoleUpdateRequest;
import com.kahua.tracker.entity.Role;
import com.kahua.tracker.repository.RoleRepository;
import com.kahua.tracker.repository.RolePageRepository;
import com.kahua.tracker.repository.PageRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
        request.getIsActive().ifPresent(roleToUpdate::setIsActive);

        return Optional.of(roleRepository.save(roleToUpdate));
    }

    @Transactional
    public void updateRolePages(UUID roleId, java.util.List<UUID> newPageIds) {
        java.util.List<com.kahua.tracker.entity.RolePage> existingRolePages = rolePageRepository.findByRoleId(roleId);
        
        existingRolePages.forEach(rp -> rp.setIsActive(false));

        if (newPageIds != null) {
            for (UUID pageId : newPageIds) {
                java.util.Optional<com.kahua.tracker.entity.RolePage> existing = existingRolePages.stream()
                        .filter(rp -> rp.getPageId().equals(pageId))
                        .findFirst();

                if (existing.isPresent()) {
                    existing.get().setIsActive(true);
                } else {
                    if (pageRepository.existsById(pageId)) {
                        com.kahua.tracker.entity.RolePage newRp = com.kahua.tracker.entity.RolePage.builder()
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
