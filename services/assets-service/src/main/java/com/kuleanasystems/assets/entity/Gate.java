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
@Table(name = "gate", schema = "asset", uniqueConstraints = {
        @UniqueConstraint(name = "uq_gate_name", columnNames = {"name"})
})
public class Gate {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "gate_id", nullable = false)
    private UUID gateId;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;

    @Column(name = "cash_banks_required_for_busy")
    private Integer cashBanksRequiredForBusy;

    @Column(name = "cash_banks_required_for_moderate")
    private Integer cashBanksRequiredForModerate;

    @Column(name = "cash_banks_required_for_lite")
    private Integer cashBanksRequiredForLite;

    @Column(name = "display_order")
    private Integer displayOrder;

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
