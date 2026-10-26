package com.kuleanasystems.assets.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
@Entity
@Table(name = "shift_worker", schema = "asset")
public class ShiftWorker {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "shift_worker_id", nullable = false)
    private UUID shiftWorkerId;

    @ManyToOne(optional = false)
    @JoinColumn(name = "shift_id", nullable = false)
    private Shift shift;

    @Column(name = "role_name", nullable = false, length = 50)
    private String roleName;

    @Column(name = "hourly_rate", nullable = false, precision = 8, scale = 2)
    private BigDecimal hourlyRate;

    @Column(name = "count", nullable = false)
    private Integer count = 0;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private Instant updatedAt;
}
