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
public class CashBankPatchDto {
    private String bankName;
    private BigDecimal startingAmount;
    private BigDecimal officialCount;
    private Integer count100s;
    private Integer count50s;
    private Integer count20s;
    private Integer count10s;
    private Integer count5s;
    private Integer count1s;
    private Integer count1cs;
    private String status;
    private UUID operationId;
    private UUID shiftId;
    private UUID gateId;
    private String deviceId;
    private UUID parentCashBankId;
    private UUID processId;
    private UUID personStickerId;

    @JsonIgnore
    private final Set<String> dirtyFields = new HashSet<>();

    public void setBankName(String bankName) {
        this.bankName = bankName;
        this.dirtyFields.add("bankName");
    }

    public void setStartingAmount(BigDecimal startingAmount) {
        this.startingAmount = startingAmount;
        this.dirtyFields.add("startingAmount");
    }

    public void setOfficialCount(BigDecimal officialCount) {
        this.officialCount = officialCount;
        this.dirtyFields.add("officialCount");
    }

    public void setCount100s(Integer count100s) {
        this.count100s = count100s;
        this.dirtyFields.add("count100s");
    }

    public void setCount50s(Integer count50s) {
        this.count50s = count50s;
        this.dirtyFields.add("count50s");
    }

    public void setCount20s(Integer count20s) {
        this.count20s = count20s;
        this.dirtyFields.add("count20s");
    }

    public void setCount10s(Integer count10s) {
        this.count10s = count10s;
        this.dirtyFields.add("count10s");
    }

    public void setCount5s(Integer count5s) {
        this.count5s = count5s;
        this.dirtyFields.add("count5s");
    }

    public void setCount1s(Integer count1s) {
        this.count1s = count1s;
        this.dirtyFields.add("count1s");
    }

    public void setCount1cs(Integer count1cs) {
        this.count1cs = count1cs;
        this.dirtyFields.add("count1cs");
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

    public void setParentCashBankId(UUID parentCashBankId) {
        this.parentCashBankId = parentCashBankId;
        this.dirtyFields.add("parentCashBankId");
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