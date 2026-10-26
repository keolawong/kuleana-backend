package com.kuleanasystems.assets.dto.report;

import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GateBankSummaryDto {
    private UUID gateId;
    private String gateName;
    private Integer displayOrder;
    private int issued;
    private int used;
    private int notUsed;
}
