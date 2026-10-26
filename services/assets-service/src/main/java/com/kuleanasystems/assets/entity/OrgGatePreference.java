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
@Table(name = "org_gate_preference", schema = "asset", uniqueConstraints = {
        @UniqueConstraint(name = "uq_shift_org_gate_pref", columnNames = {"shift_id", "organization", "gate_id"})
})
public class OrgGatePreference {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "preference_id", nullable = false)
    private UUID preferenceId;

    @ManyToOne(optional = false)
    @JoinColumn(name = "shift_id", nullable = false)
    private Shift shift;

    @ManyToOne(optional = false)
    @JoinColumn(name = "gate_id", nullable = false)
    private Gate gate;

    @Column(name = "organization", nullable = false, length = 150)
    private String organization;

    @Column(name = "priority", nullable = false)
    private Integer priority = 1;

    @Column(name = "notes", length = 255)
    private String notes;

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
