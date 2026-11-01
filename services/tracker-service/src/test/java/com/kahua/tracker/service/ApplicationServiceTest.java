package com.kahua.tracker.service;

import com.kahua.tracker.dto.application.ApplicationUpdateRequest;
import com.kahua.tracker.entity.Application;
import com.kahua.tracker.repository.ApplicationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ApplicationServiceTest {

    @Mock private ApplicationRepository applicationRepository;

    @InjectMocks
    private ApplicationService applicationService;

    @BeforeEach
    void setUp() {
        SecurityContext securityContext = mock(SecurityContext.class);
        Authentication authentication = mock(Authentication.class);
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.getName()).thenReturn("admin");
        SecurityContextHolder.setContext(securityContext);
    }

    @Test
    void searchApplications_shouldReturnList() {
        when(applicationRepository.findAll(any(Specification.class))).thenReturn(List.of(new Application()));
        List<Application> result = applicationService.searchApplications("test", null, null, true);
        assertThat(result).hasSize(1);
    }

    @Test
    void updateApplication_shouldUpdateAndSetUpdatedBy() {
        // Arrange
        UUID appId = UUID.randomUUID();
        Application app = new Application();
        app.setId(appId);
        app.setName("Old");

        when(applicationRepository.findById(appId)).thenReturn(Optional.of(app));
        when(applicationRepository.save(any(Application.class))).thenAnswer(i -> i.getArgument(0));

        ApplicationUpdateRequest req = new ApplicationUpdateRequest();
        req.setName(Optional.of("New"));
        req.setIsActive(Optional.of(false));

        // Act
        Optional<Application> result = applicationService.updateApplication(appId, req);

        // Assert
        assertThat(result).isPresent();
        assertThat(result.get().getName()).isEqualTo("New");
        assertThat(result.get().getIsActive()).isFalse();
        assertThat(result.get().getUpdatedBy()).isEqualTo("admin");
    }
}
