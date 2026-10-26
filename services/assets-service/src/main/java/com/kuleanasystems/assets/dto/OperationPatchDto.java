package com.kuleanasystems.assets.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.Set;

@Data
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class OperationPatchDto {
    private String name;
    private LocalDate startDate;
    private LocalDate endDate;
    private String locationName;
    private Boolean isActive;
    private BigDecimal ticketPrice;
    private BigDecimal ccFee;
    private String ccFeeType;

    @JsonIgnore
    private final Set<String> dirtyFields = new HashSet<>();

    public void setName(String name) {
        this.name = name;
        this.dirtyFields.add("name");
    }

    public void setStartDate(LocalDate startDate) {
        this.startDate = startDate;
        this.dirtyFields.add("startDate");
    }

    public void setEndDate(LocalDate endDate) {
        this.endDate = endDate;
        this.dirtyFields.add("endDate");
    }

    public void setLocationName(String locationName) {
        this.locationName = locationName;
        this.dirtyFields.add("locationName");
    }

    public void setIsActive(Boolean isActive) {
        this.isActive = isActive;
        this.dirtyFields.add("isActive");
    }

    public void setTicketPrice(BigDecimal ticketPrice) {
        this.ticketPrice = ticketPrice;
        this.dirtyFields.add("ticketPrice");
    }

    public void setCcFee(BigDecimal ccFee) {
        this.ccFee = ccFee;
        this.dirtyFields.add("ccFee");
    }

    public void setCcFeeType(String ccFeeType) {
        this.ccFeeType = ccFeeType;
        this.dirtyFields.add("ccFeeType");
    }

    @JsonIgnore
    public boolean isDirty(String fieldName) {
        return dirtyFields.contains(fieldName);
    }
}