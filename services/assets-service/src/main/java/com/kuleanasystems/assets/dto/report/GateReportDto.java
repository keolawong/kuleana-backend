package com.kuleanasystems.assets.dto.report;

import java.util.List;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GateReportDto {
    private UUID gateId;
    private String gateName;
    private Integer displayOrder;
    private List<DeviceReportDto> devices;
    private GateTotalsDto totals;
}
