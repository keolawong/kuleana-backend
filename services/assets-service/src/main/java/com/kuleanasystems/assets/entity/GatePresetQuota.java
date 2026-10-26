package com.kuleanasystems.assets.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
@Entity
@Table(name = "gate_preset_quota", schema = "asset", uniqueConstraints = {
        @UniqueConstraint(name = "uq_preset_gate_role", columnNames = {"preset_type", "gate_id", "job_role"})
})
public class GatePresetQuota {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "preset_quota_id", nullable = false)
    private UUID presetQuotaId;

    @Column(name = "preset_type", nullable = false, length = 20)
    private String presetType;

    @ManyToOne(optional = false)
    @JoinColumn(name = "gate_id", nullable = false)
    private Gate gate;

    @Column(name = "job_role", nullable = false, length = 50)
    private String jobRole;

    @Column(name = "planned_workers", nullable = false)
    private Integer plannedWorkers = 0;

    @Column(name = "min_workers", nullable = false)
    private Integer minWorkers = 0;

    @Column(name = "happy_workers", nullable = false)
    private Integer happyWorkers = 0;

    @Column(name = "priority", nullable = false)
    private Integer priority = 1;

    @CreationTimestamp
    @Column(name = "created_at")
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private Instant updatedAt;
}
