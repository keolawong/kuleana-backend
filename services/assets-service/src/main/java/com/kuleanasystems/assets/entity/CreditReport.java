package com.kuleanasystems.assets.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
@Entity
@Table(name = "credit_report", schema = "asset")
public class CreditReport {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "credit_report_id", nullable = false)
    private UUID creditReportId;

    @Column(name = "total_amount", precision = 12, scale = 2)
    private BigDecimal totalAmount;

    @ManyToOne
    @JoinColumn(name = "operation_id")
    private Operation operation;

    @ManyToOne
    @JoinColumn(name = "shift_id")
    private Shift shift;

    @ManyToOne
    @JoinColumn(name = "gate_id")
    private Gate gate;

    @Column(name = "device_id", length = 50)
    private String deviceId;

    @Column(name = "process_id")
    private UUID processId;

    @ManyToOne
    @JoinColumn(name = "person_sticker_id")
    private Person person;

    public Person getPersonSticker() {
        return this.person;
    }

    public void setPersonSticker(Person person) {
        this.person = person;
    }

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