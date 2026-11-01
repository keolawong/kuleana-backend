package com.kahua.tracker.service;

import com.kahua.tracker.dto.flow.FlowCreateRequest;
import com.kahua.tracker.dto.flow.FlowUpdateRequest;
import com.kahua.tracker.entity.Flow;
import com.kahua.tracker.repository.FlowRepository;
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
class FlowServiceTest {

    @Mock private FlowRepository flowRepository;

    @InjectMocks
    private FlowService flowService;

    @BeforeEach
    void setUp() {
        SecurityContext securityContext = mock(SecurityContext.class);
        Authentication authentication = mock(Authentication.class);
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.getName()).thenReturn("admin");
        SecurityContextHolder.setContext(securityContext);
    }

    @Test
    void createFlow_shouldSaveWithCurrentUser() {
        // Arrange
        FlowCreateRequest req = new FlowCreateRequest();
        req.setFlowName("My Flow");
        
        when(flowRepository.save(any(Flow.class))).thenAnswer(i -> i.getArgument(0));

        // Act
        Flow result = flowService.createFlow(req);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getFlowName()).isEqualTo("My Flow");
        assertThat(result.getCreatedBy()).isEqualTo("admin");
        assertThat(result.getIsActive()).isTrue();
    }

    @Test
    void updateFlow_shouldUpdateFields() {
        // Arrange
        UUID flowId = UUID.randomUUID();
        Flow flow = new Flow();
        flow.setId(flowId);
        flow.setFlowName("Old");

        when(flowRepository.findById(flowId)).thenReturn(Optional.of(flow));
        when(flowRepository.save(any(Flow.class))).thenAnswer(i -> i.getArgument(0));

        FlowUpdateRequest req = new FlowUpdateRequest();
        req.setFlowName(Optional.of("New"));

        // Act
        Optional<Flow> result = flowService.updateFlow(flowId, req);

        // Assert
        assertThat(result).isPresent();
        assertThat(result.get().getFlowName()).isEqualTo("New");
        assertThat(result.get().getUpdatedBy()).isEqualTo("admin");
    }
}
