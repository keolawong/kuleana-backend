package com.kahua.tracker.service;

import com.google.cloud.tasks.v2.CloudTasksClient;
import com.google.cloud.tasks.v2.Task;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CloudTasksServiceTest {

    @InjectMocks
    private CloudTasksService cloudTasksService;

    @AfterEach
    void tearDown() {
        RequestContextHolder.resetRequestAttributes();
    }

    @Test
    void resolveBaseUrl_shouldUseExplicitlyConfiguredUrl_whenPresent() {
        ReflectionTestUtils.setField(cloudTasksService, "configuredCloudRunUrl", "https://tracker-staging-custom.run.app/");

        String resolved = cloudTasksService.resolveBaseUrl();

        assertThat(resolved).isEqualTo("https://tracker-staging-custom.run.app");
    }

    @Test
    void resolveBaseUrl_shouldDeriveFromRequestContext_whenConfiguredUrlIsNull() {
        ReflectionTestUtils.setField(cloudTasksService, "configuredCloudRunUrl", null);

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setScheme("https");
        request.setServerName("tracker-staging-515872944260.us-central1.run.app");
        request.setServerPort(443);
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));

        String resolved = cloudTasksService.resolveBaseUrl();

        assertThat(resolved).isEqualTo("https://tracker-staging-515872944260.us-central1.run.app");
    }

    @Test
    void resolveBaseUrl_shouldConvertHttpToHttpsAndStripPort8080_onNonLocalhost() {
        ReflectionTestUtils.setField(cloudTasksService, "configuredCloudRunUrl", null);

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setScheme("http");
        request.setServerName("tracker-staging-515872944260.us-central1.run.app");
        request.setServerPort(8080);
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));

        String resolved = cloudTasksService.resolveBaseUrl();

        assertThat(resolved).isEqualTo("https://tracker-staging-515872944260.us-central1.run.app");
    }

    @Test
    void resolveBaseUrl_shouldPreserveHttpAndPort_onLocalhost() {
        ReflectionTestUtils.setField(cloudTasksService, "configuredCloudRunUrl", null);

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setScheme("http");
        request.setServerName("localhost");
        request.setServerPort(8080);
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));

        String resolved = cloudTasksService.resolveBaseUrl();

        assertThat(resolved).isEqualTo("http://localhost:8080");
    }

    @Test
    void resolveBaseUrl_shouldPreserveHttpAndPort_on127001() {
        ReflectionTestUtils.setField(cloudTasksService, "configuredCloudRunUrl", null);

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setScheme("http");
        request.setServerName("127.0.0.1");
        request.setServerPort(8080);
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));

        String resolved = cloudTasksService.resolveBaseUrl();

        assertThat(resolved).isEqualTo("http://127.0.0.1:8080");
    }

    @Test
    void resolveBaseUrl_shouldFallbackToDefault_whenNoContextOrConfig() {
        ReflectionTestUtils.setField(cloudTasksService, "configuredCloudRunUrl", null);
        RequestContextHolder.resetRequestAttributes();

        String resolved = cloudTasksService.resolveBaseUrl();

        assertThat(resolved).isEqualTo("https://tracker-service-515872944260.us-central1.run.app");
    }

    @Test
    void enqueueLandingZoneTask_shouldReuseClientAcrossInvocations() {
        CloudTasksClient mockClient = mock(CloudTasksClient.class);
        when(mockClient.createTask(anyString(), any(Task.class))).thenReturn(Task.newBuilder().setName("task-123").build());
        cloudTasksService.setClient(mockClient);

        UUID id1 = UUID.randomUUID();
        UUID id2 = UUID.randomUUID();

        cloudTasksService.enqueueLandingZoneTask(id1);
        cloudTasksService.enqueueLandingZoneTask(id2);

        verify(mockClient, times(2)).createTask(anyString(), any(Task.class));
    }

    @Test
    void cleanUp_shouldCloseClientWhenActive() {
        CloudTasksClient mockClient = mock(CloudTasksClient.class);
        when(mockClient.isShutdown()).thenReturn(false);
        cloudTasksService.setClient(mockClient);

        cloudTasksService.cleanUp();

        verify(mockClient, times(1)).close();
    }
}
