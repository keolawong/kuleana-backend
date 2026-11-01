package com.kahua.tracker.service;

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

    @Value("${gcp.queue-name:tracker-flow-lz-queue}")
    private String queueName = "tracker-flow-lz-queue";

    @Value("${cloud.run.url:#{null}}")
    private String configuredCloudRunUrl;

    @Value("${security.internal-task-api-key:default-internal-key-change-me}")
    private String internalApiKey = "default-internal-key-change-me";

    private CloudTasksClient cloudTasksClient;

    public CloudTasksService() {
    }

    public CloudTasksService(CloudTasksClient cloudTasksClient) {
        this.cloudTasksClient = cloudTasksClient;
    }

    public synchronized CloudTasksClient getClient() throws IOException {
        if (this.cloudTasksClient == null || this.cloudTasksClient.isShutdown() || this.cloudTasksClient.isTerminated()) {
            this.cloudTasksClient = CloudTasksClient.create();
        }
        return this.cloudTasksClient;
    }

    public synchronized void setClient(CloudTasksClient client) {
        this.cloudTasksClient = client;
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
                        String scheme = uri.getScheme();
                        int port = uri.getPort();
                        if ("http".equalsIgnoreCase(scheme)) {
                            scheme = "https";
                        }
                        if (port == 8080) {
                            port = -1;
                        }
                        URI modifiedUri = new URI(scheme, uri.getUserInfo(), host, port, uri.getPath(), uri.getQuery(), uri.getFragment());
                        dynamicUrl = modifiedUri.toString().replaceAll("/+$", "");
                    }
                    return dynamicUrl;
                }
            }
        } catch (Exception e) {
            log.debug("Could not determine dynamic request base URL: {}", e.getMessage());
        }

        return "https://tracker-service-515872944260.us-central1.run.app";
    }

    public void enqueueLandingZoneTask(UUID landingZoneId) {
        String baseUrl = resolveBaseUrl();
        String url = baseUrl + "/api/internal/tasks/process-landing-zone/" + landingZoneId;

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

            Task createdTask = client.createTask(queuePath, task);
            log.info("Enqueued Cloud Task [{}] with target URL [{}] for Landing Zone record [{}]", createdTask.getName(), url, landingZoneId);
        } catch (Exception e) {
            log.error("Failed to enqueue Cloud Task for Landing Zone record [{}] with target URL [{}]", landingZoneId, url, e);
            throw new RuntimeException("Could not enqueue background processing task", e);
        }
    }
}
