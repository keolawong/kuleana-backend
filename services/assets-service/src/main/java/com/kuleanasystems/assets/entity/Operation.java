package com.kuleanasystems.assets.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Getter
@Setter
@Entity
@Table(name = "operation", schema = "asset")
public class Operation {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "operation_id", nullable = false)
    private UUID operationId;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "start_date")
    private LocalDate startDate;

    @Column(name = "end_date")
    private LocalDate endDate;

    @Column(name = "location_name")
    private String locationName;

    @Column(name = "is_active")
    private Boolean isActive = true;

    @Column(name = "ticket_price", precision = 12, scale = 2)
    private BigDecimal ticketPrice;

    @Column(name = "cc_fee", precision = 12, scale = 2)
    private BigDecimal ccFee;

    @Column(name = "cc_fee_type")
    private String ccFeeType;

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
