package com.kuleanasystems.assets.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.Instant;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@Data
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class ShiftPatchDto {
    private UUID operationId;
    private String name;
    private Instant startTime;
    private Instant endTime;
    private String traffic;
    private String status;

    @JsonIgnore
    private final Set<String> dirtyFields = new HashSet<>();

    public void setOperationId(UUID operationId) {
        this.operationId = operationId;
        this.dirtyFields.add("operationId");
    }

    public void setName(String name) {
        this.name = name;
        this.dirtyFields.add("name");
    }

    public void setStartTime(Instant startTime) {
        this.startTime = startTime;
        this.dirtyFields.add("startTime");
    }

    public void setEndTime(Instant endTime) {
        this.endTime = endTime;
        this.dirtyFields.add("endTime");
    }

    public void setTraffic(String traffic) {
        this.traffic = traffic;
        this.dirtyFields.add("traffic");
    }

    public void setStatus(String status) {
        this.status = status;
        this.dirtyFields.add("status");
    }

    @JsonIgnore
    public boolean isDirty(String fieldName) {
        return dirtyFields.contains(fieldName);
    }
}