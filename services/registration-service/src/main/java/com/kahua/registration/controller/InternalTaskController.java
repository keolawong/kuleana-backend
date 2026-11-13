package com.kahua.registration.controller;

import com.kahua.registration.service.RegistrationOutboxService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/internal/tasks")
@RequiredArgsConstructor
@Slf4j
public class InternalTaskController {

    private final RegistrationOutboxService outboxService;

    @Value("${security.internal-task-api-key:default-internal-key-change-me}")
    private String internalApiKey;

    @PostMapping("/process-outbox/{id}")
    public ResponseEntity<Void> processOutbox(
            @PathVariable("id") UUID id,
            @RequestHeader(value = "X-Internal-Task-Key", required = false) String taskKey,
            @RequestHeader(value = "X-CloudTasks-TaskRetryCount", required = false, defaultValue = "0") int retryCount) {

        if (taskKey == null || !taskKey.equals(internalApiKey)) {
            log.warn("Unauthorized internal task access for outbox ID: {}", id);
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        try {
            outboxService.processOutboxRecord(id, retryCount);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            log.error("Failed executing outbox task [{}]: {}", id, e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}
