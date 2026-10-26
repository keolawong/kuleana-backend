package com.kuleanasystems.assets.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@Data
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class CreditReportPatchDto {
    private BigDecimal totalAmount;
    private UUID operationId;
    private UUID shiftId;
    private UUID gateId;
    private String deviceId;
    private UUID processId;
    private UUID personStickerId;

    @JsonIgnore
    private final Set<String> dirtyFields = new HashSet<>();

    public void setTotalAmount(BigDecimal totalAmount) {
        this.totalAmount = totalAmount;
        this.dirtyFields.add("totalAmount");
    }

    public void setOperationId(UUID operationId) {
        this.operationId = operationId;
        this.dirtyFields.add("operationId");
    }

    public void setShiftId(UUID shiftId) {
        this.shiftId = shiftId;
        this.dirtyFields.add("shiftId");
    }

    public void setGateId(UUID gateId) {
        this.gateId = gateId;
        this.dirtyFields.add("gateId");
    }

    public void setDeviceId(String deviceId) {
        this.deviceId = deviceId;
        this.dirtyFields.add("deviceId");
    }

    public void setProcessId(UUID processId) {
        this.processId = processId;
        this.dirtyFields.add("processId");
    }

    public void setPersonStickerId(UUID personStickerId) {
        this.personStickerId = personStickerId;
        this.dirtyFields.add("personStickerId");
    }

    @JsonIgnore
    public boolean isDirty(String fieldName) {
        return dirtyFields.contains(fieldName);
    }
}