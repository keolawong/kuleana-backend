package com.kuleanasystems.assets.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ActiveCheckinSessionDto {
    private UUID sessionId;
    private UUID gateId;
    private String gateName;
    private UUID shiftId;
    private String shiftName;
    private String username;
    private String userDisplayName;
    private String status;
    private Instant lastHeartbeatAt;
}
