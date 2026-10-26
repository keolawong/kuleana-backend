package com.kuleanasystems.assets.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.HashSet;
import java.util.Set;

@Data
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class DevicePatchDto {
    private String deviceId;
    private String deviceNumber;
    private Boolean isActive;

    @JsonIgnore
    private final Set<String> dirtyFields = new HashSet<>();

    public void setDeviceId(String deviceId) {
        this.deviceId = deviceId;
        this.dirtyFields.add("deviceId");
    }

    public void setDeviceNumber(String deviceNumber) {
        this.deviceNumber = deviceNumber;
        this.dirtyFields.add("deviceNumber");
    }

    public void setIsActive(Boolean isActive) {
        this.isActive = isActive;
        this.dirtyFields.add("isActive");
    }

    @JsonIgnore
    public boolean isDirty(String fieldName) {
        return dirtyFields.contains(fieldName);
    }
}
