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
@Table(name = "cash_bank", schema = "asset")
public class CashBank {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "cash_bank_id", nullable = false)
    private UUID cashBankId;

    @Column(name = "bank_name", length = 100)
    private String bankName;

    @Column(name = "starting_amount", precision = 12, scale = 2)
    private BigDecimal startingAmount;

    @Column(name = "official_count", precision = 12, scale = 2)
    private BigDecimal officialCount;

    @Column(name = "count_100s")
    private Integer count100s = 0;

    @Column(name = "count_50s")
    private Integer count50s = 0;

    @Column(name = "count_20s")
    private Integer count20s = 0;

    @Column(name = "count_10s")
    private Integer count10s = 0;

    @Column(name = "count_5s")
    private Integer count5s = 0;

    @Column(name = "count_1s")
    private Integer count1s = 0;

    @Column(name = "count_1cs")
    private Integer count1cs = 0;

    @Column(name = "status", length = 20)
    private String status;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "operation_id")
    private Operation operation;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "shift_id")
    private Shift shift;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "gate_id")
    private Gate gate;

    @Column(name = "device_id", length = 50)
    private String deviceId;

    @Column(name = "parent_cash_bank_id")
    private UUID parentCashBankId;

    @Column(name = "process_id")
    private UUID processId;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "person_sticker_id")
    private Person person;

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
