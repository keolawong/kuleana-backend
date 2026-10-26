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
@Table(name = "gate_captain", schema = "asset")
public class GateCaptain {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "gate_captain_id", nullable = false)
    private UUID gateCaptainId;

    @Column(name = "captain_name", nullable = false)
    private String captainName;

    @ManyToOne
    @JoinColumn(name = "gate_id", nullable = false)
    private Gate gate;

    @ManyToOne
    @JoinColumn(name = "operation_id", nullable = false)
    private Operation operation;

    @ManyToOne
    @JoinColumn(name = "shift_id", nullable = false)
    private Shift shift;

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