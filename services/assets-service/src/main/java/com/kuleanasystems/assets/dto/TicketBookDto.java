package com.kuleanasystems.assets.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class TicketBookDto {
    private UUID ticketBookId;
    private String scanCode;

    @NotNull(message = "Start number is required")
    private Integer startNum;

    @NotNull(message = "End number is required")
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
}