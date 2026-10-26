package com.kuleanasystems.assets.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@Data
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class GateCaptainPatchDto {
    private String captainName;
    private UUID gateId;
    private UUID operationId;
    private UUID shiftId;

    @JsonIgnore
    private final Set<String> dirtyFields = new HashSet<>();

    public void setCaptainName(String captainName) {
        this.captainName = captainName;
        this.dirtyFields.add("captainName");
    }

    public void setGateId(UUID gateId) {
        this.gateId = gateId;
        this.dirtyFields.add("gateId");
    }

    public void setOperationId(UUID operationId) {
        this.operationId = operationId;
        this.dirtyFields.add("operationId");
    }

    public void setShiftId(UUID shiftId) {
        this.shiftId = shiftId;
        this.dirtyFields.add("shiftId");
    }

    @JsonIgnore
    public boolean isDirty(String fieldName) {
        return dirtyFields.contains(fieldName);
    }
}