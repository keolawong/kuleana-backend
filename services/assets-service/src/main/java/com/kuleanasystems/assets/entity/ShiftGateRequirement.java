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
@Table(name = "shift_gate_requirement", schema = "asset", uniqueConstraints = {
        @UniqueConstraint(name = "uq_shift_gate_role", columnNames = {"shift_id", "gate_id", "job_role"})
})
public class ShiftGateRequirement {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "requirement_id", nullable = false)
    private UUID requirementId;

    @ManyToOne(optional = false)
    @JoinColumn(name = "shift_id", nullable = false)
    private Shift shift;

    @ManyToOne(optional = false)
    @JoinColumn(name = "gate_id", nullable = false)
    private Gate gate;

    @Column(name = "job_role", nullable = false, length = 50)
    private String jobRole;

    @Column(name = "workers_needed", nullable = false)
    private Integer workersNeeded = 0;

    @Column(name = "min_workers", nullable = false)
    private Integer minWorkers = 0;

    @Column(name = "happy_workers", nullable = false)
    private Integer happyWorkers = 0;

    @Column(name = "priority", nullable = false)
    private Integer priority = 1;

    @Column(name = "created_by", length = 100)
    private String createdBy;

    @CreationTimestamp
    @Column(name = "created_at")
    private Instant createdAt;

    @Column(name = "updated_by", length = 100)
    private String updatedBy;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private Instant updatedAt;
}
