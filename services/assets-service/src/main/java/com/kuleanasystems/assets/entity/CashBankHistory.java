package com.kuleanasystems.assets.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
@Entity
@Table(name = "cash_bank_history", schema = "asset")
public class CashBankHistory {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "cash_bank_history_id", nullable = false)
    private UUID cashBankHistoryId;

    @Column(name = "cash_bank_id")
    private UUID cashBankId;

    @Column(name = "bank_name", length = 100)
    private String bankName;

    @Column(name = "starting_amount", precision = 12, scale = 2)
    private BigDecimal startingAmount;

    @Column(name = "official_count", precision = 12, scale = 2)
    private BigDecimal officialCount;

    @Column(name = "count_100s")
    private Integer count100s;

    @Column(name = "count_50s")
    private Integer count50s;

    @Column(name = "count_20s")
    private Integer count20s;

    @Column(name = "count_10s")
    private Integer count10s;

    @Column(name = "count_5s")
    private Integer count5s;

    @Column(name = "count_1s")
    private Integer count1s;

    @Column(name = "count_1cs")
    private Integer count1cs;

    @Column(name = "status", length = 20)
    private String status;

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

    @Column(name = "parent_cash_bank_id")
    private UUID parentCashBankId;

    @Column(name = "process_id")
    private UUID processId;

    @Column(name = "created_by", length = 100)
    private String createdBy;

    @CreationTimestamp
    @Column(name = "created_at")
    private Instant createdAt;
}
