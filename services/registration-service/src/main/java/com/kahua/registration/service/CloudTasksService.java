package com.kahua.registration.service;

import com.google.cloud.tasks.v2.*;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.io.IOException;
import java.net.URI;
import java.util.UUID;

@Service
@Slf4j
public class CloudTasksService {

    @Value("${gcp.project-id:project-3eaeea86-ca1c-4d5b-ae6}")
    private String projectId = "project-3eaeea86-ca1c-4d5b-ae6";

    @Value("${gcp.location:us-central1}")
    private String location = "us-central1";

    @Value("${gcp.queue-name:registration-outbox-queue}")
    private String queueName = "registration-outbox-queue";

    @Value("${gcp.tasks-enabled:false}")
    private boolean tasksEnabled = false;

    @Value("${cloud.run.url:#{null}}")
    private String configuredCloudRunUrl;

    @Value("${security.internal-task-api-key:default-internal-key-change-me}")
    private String internalApiKey = "default-internal-key-change-me";

    private CloudTasksClient cloudTasksClient;

    public CloudTasksService() {
    }

    public CloudTasksService(CloudTasksClient cloudTasksClient) {
        this.cloudTasksClient = cloudTasksClient;
        this.tasksEnabled = true;
    }

    public synchronized CloudTasksClient getClient() throws IOException {
        if (this.cloudTasksClient == null || this.cloudTasksClient.isShutdown() || this.cloudTasksClient.isTerminated()) {
            this.cloudTasksClient = CloudTasksClient.create();
        }
        return this.cloudTasksClient;
    }

    @PreDestroy
    public synchronized void cleanUp() {
        if (this.cloudTasksClient != null && !this.cloudTasksClient.isShutdown()) {
            try {
                this.cloudTasksClient.close();
            } catch (Exception e) {
                log.warn("Error closing CloudTasksClient: {}", e.getMessage());
            } finally {
                this.cloudTasksClient = null;
            }
        }
    }

    public String resolveBaseUrl() {
        if (configuredCloudRunUrl != null && !configuredCloudRunUrl.isBlank()) {
            return configuredCloudRunUrl.replaceAll("/+$", "");
        }
        try {
            ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attributes != null && attributes.getRequest() != null) {
                String dynamicUrl = ServletUriComponentsBuilder.fromCurrentContextPath().toUriString();
                if (dynamicUrl != null && !dynamicUrl.isBlank()) {
                    dynamicUrl = dynamicUrl.replaceAll("/+$", "");
                    URI uri = URI.create(dynamicUrl);
                    String host = uri.getHost();
                    boolean isLocal = host != null && (host.equalsIgnoreCase("localhost") || host.equals("127.0.0.1"));
                    if (!isLocal) {
                        String scheme = "http".equalsIgnoreCase(uri.getScheme()) ? "https" : uri.getScheme();
                        int port = uri.getPort() == 8080 ? -1 : uri.getPort();
                        dynamicUrl = new URI(scheme, uri.getUserInfo(), host, port, uri.getPath(), uri.getQuery(), uri.getFragment())
                                .toString().replaceAll("/+$", "");
                    }
                    return dynamicUrl;
                }
            }
        } catch (Exception e) {
            log.debug("Could not resolve dynamic request base URL: {}", e.getMessage());
        }
        return "https://registration-service-515872944260.us-central1.run.app";
    }

    public void enqueueOutboxTask(UUID outboxId) {
        if (!tasksEnabled) {
            log.debug("Cloud Tasks disabled; skipping enqueue for outbox {}", outboxId);
            return;
        }
        String url = resolveBaseUrl() + "/api/internal/tasks/process-outbox/" + outboxId;
        try {
            CloudTasksClient client = getClient();
            String queuePath = QueueName.of(projectId, location, queueName).toString();
            Task task = Task.newBuilder()
                    .setHttpRequest(
                            HttpRequest.newBuilder()
                                    .setUrl(url)
                                    .setHttpMethod(HttpMethod.POST)
                                    .putHeaders("X-Internal-Task-Key", internalApiKey)
                                    .putHeaders("Content-Type", "application/json")
                                    .build()
                    )
                    .build();
            client.createTask(queuePath, task);
        } catch (Exception e) {
            log.error("Failed to enqueue Cloud Task for outbox {}: {}", outboxId, e.getMessage());
        }
    }
}
