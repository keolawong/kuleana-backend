package com.kahua.tracker.service;

import com.kahua.tracker.dto.RoleUpdateRequest;
import com.kahua.tracker.entity.Role;
import com.kahua.tracker.repository.RoleRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RoleServiceTest {

    @Mock
    private RoleRepository roleRepository;

    @InjectMocks
    private RoleService roleService;

    private Role mockRole;

    @BeforeEach
    void setUp() {
        mockRole = new Role();
        mockRole.setId(UUID.randomUUID());
        mockRole.setName("ADMIN");
        mockRole.setDescription("Administrator role");
        mockRole.setIsActive(true);
    }

    @Test
    void updateRole_shouldUpdateAndReturnRole() {
        // Arrange
        RoleUpdateRequest request = new RoleUpdateRequest();
        request.setName(Optional.of("SUPER_ADMIN"));

        when(roleRepository.findById(mockRole.getId())).thenReturn(Optional.of(mockRole));
        when(roleRepository.save(any(Role.class))).thenReturn(mockRole); // mockRole's state will be mutated by the service

        // Act
        Optional<Role> result = roleService.updateRole(mockRole.getId(), request);

        // Assert
        assertThat(result).isPresent();
        assertThat(result.get().getName()).isEqualTo("SUPER_ADMIN");
    }

    @Test
    void updateRole_whenRoleNotFound_shouldReturnEmpty() {
        // Arrange
        UUID fakeId = UUID.randomUUID();
        when(roleRepository.findById(fakeId)).thenReturn(Optional.empty());

        // Act
        Optional<Role> result = roleService.updateRole(fakeId, new RoleUpdateRequest());

        // Assert
        assertThat(result).isEmpty();
    }
}
