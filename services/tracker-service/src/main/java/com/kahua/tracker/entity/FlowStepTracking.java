package com.kahua.tracker.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "flow_step_tracking", schema = "tracker")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FlowStepTracking {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "flow_tracking_id", nullable = false)
    private UUID flowTrackingId;

    @Column(name = "flow_step_id", nullable = false)
    private UUID flowStepId;

    @Column(length = 50)
    private String status;

    @Column(columnDefinition = "TEXT")
    private String message;

    @Column(precision = 20, scale = 4)
    private BigDecimal amount;

    private Integer count;

    @Column(name = "created_by", nullable = false, length = 100)
    private String createdBy;

    @Column(name = "created_at", updatable = false)
    private OffsetDateTime createdAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "flow_tracking_id", insertable = false, updatable = false)
    private FlowTracking flowTracking;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "flow_step_id", insertable = false, updatable = false)
    private FlowStep flowStep;
}
