package com.kahua.tracker.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kahua.tracker.dto.UserRoleUpdateRequest;
import com.kahua.tracker.entity.Role;
import com.kahua.tracker.entity.User;
import com.kahua.tracker.entity.UserRole;
import com.kahua.tracker.repository.RoleRepository;
import com.kahua.tracker.repository.UserRepository;
import com.kahua.tracker.repository.UserRoleRepository;
import com.kahua.tracker.service.UserAuthCacheService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class UserControllerTest {

    private MockMvc mockMvc;

    @Mock
    private UserRepository userRepository;

    @Mock
    private RoleRepository roleRepository;

    @Mock
    private UserRoleRepository userRoleRepository;

    @Mock
    private UserAuthCacheService userAuthCacheService;

    @InjectMocks
    private UserController userController;

    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(userController).build();
        objectMapper = new ObjectMapper();
    }

    @Test
    void getUsers_shouldReturnUsersWithRolesAndDetails() throws Exception {
        UUID userId = UUID.randomUUID();
        User user = new User();
        user.setId(userId);
        user.setUsername("keolawong");
        user.setFirstName("Keola");
        user.setLastName("Wong");
        user.setAccountLockedUntil(OffsetDateTime.now().minusHours(1));

        UUID roleId = UUID.randomUUID();
        Role role = new Role();
        role.setId(roleId);
        role.setName("ADMIN");

        UserRole userRole = UserRole.builder()
                .userId(userId)
                .roleId(roleId)
                .role(role)
                .status("active")
                .build();

        when(userRepository.findAll(any(Specification.class))).thenReturn(List.of(user));
        when(userRoleRepository.findByUserIdIn(List.of(userId))).thenReturn(List.of(userRole));

        mockMvc.perform(get("/api/users"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].username", is("keolawong")))
                .andExpect(jsonPath("$[0].firstName", is("Keola")))
                .andExpect(jsonPath("$[0].lastName", is("Wong")))
                .andExpect(jsonPath("$[0].locked", is(false)))
                .andExpect(jsonPath("$[0].roles", hasSize(1)))
                .andExpect(jsonPath("$[0].roles[0].name", is("ADMIN")));
    }

    @Test
    void updateUserRoles_shouldAtomicallyDeleteCurrentRolesAndSaveNewRolesWithoutEvictingSession() throws Exception {
        UUID userId = UUID.randomUUID();
        User user = new User();
        user.setId(userId);
        user.setUsername("keolawong");

        UUID roleId = UUID.randomUUID();
        Role role = new Role();
        role.setId(roleId);
        role.setName("ASSET-IT");

        UserRoleUpdateRequest.RoleAssignment assignment = new UserRoleUpdateRequest.RoleAssignment();
        assignment.setRoleId(roleId);
        assignment.setStatus("active");

        UserRoleUpdateRequest request = new UserRoleUpdateRequest();
        request.setRoles(List.of(assignment));

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(roleRepository.findById(roleId)).thenReturn(Optional.of(role));

        mockMvc.perform(put("/api/users/" + userId + "/roles")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(content().string("User roles updated successfully."));

        // Verify deleteByUserId was called atomically
        verify(userRoleRepository).deleteByUserId(userId);
        // Verify saveAll was called with new roles
        verify(userRoleRepository).saveAll(anyList());
        // Verify userAuthCacheService.evictUser was NOT called (protecting active JWT session)
        verifyNoInteractions(userAuthCacheService);
    }

    @Test
    void updateUserRoles_whenUserNotFound_shouldReturn404() throws Exception {
        UUID userId = UUID.randomUUID();
        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        UserRoleUpdateRequest request = new UserRoleUpdateRequest();
        request.setRoles(List.of());

        mockMvc.perform(put("/api/users/" + userId + "/roles")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound());

        verifyNoInteractions(userRoleRepository);
    }
}
