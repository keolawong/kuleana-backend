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
public class TicketBookPatchDto {
    private String scanCode;
    private Integer startNum;
    private Integer endNum;
    private Integer nextTicket;
    private Boolean isReusable;
    private String status;
    private UUID operationId;
    private UUID shiftId;
    private UUID gateId;
    private String deviceId;
    private UUID parentTicketBookId;
    private UUID processId;
    private UUID personStickerId;

    @JsonIgnore
    private final Set<String> dirtyFields = new HashSet<>();

    public void setScanCode(String scanCode) {
        this.scanCode = scanCode;
        this.dirtyFields.add("scanCode");
    }

    public void setStartNum(Integer startNum) {
        this.startNum = startNum;
        this.dirtyFields.add("startNum");
    }

    public void setEndNum(Integer endNum) {
        this.endNum = endNum;
        this.dirtyFields.add("endNum");
    }

    public void setNextTicket(Integer nextTicket) {
        this.nextTicket = nextTicket;
        this.dirtyFields.add("nextTicket");
    }

    public void setIsReusable(Boolean isReusable) {
        this.isReusable = isReusable;
        this.dirtyFields.add("isReusable");
    }

    public void setStatus(String status) {
        this.status = status;
        this.dirtyFields.add("status");
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

    public void setParentTicketBookId(UUID parentTicketBookId) {
        this.parentTicketBookId = parentTicketBookId;
        this.dirtyFields.add("parentTicketBookId");
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