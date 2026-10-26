package com.kuleanasystems.assets.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
@Entity
@Table(name = "shift_sales", schema = "asset")
public class ShiftSales {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "shift_sales_id", nullable = false)
    private UUID shiftSalesId;

    @ManyToOne(optional = false)
    @JoinColumn(name = "shift_id", nullable = false)
    private Shift shift;

    @Column(name = "recorded_at", nullable = false)
    private Instant recordedAt;

    @Column(name = "total_sales", nullable = false, precision = 12, scale = 2)
    private BigDecimal totalSales;

    @Column(name = "labor_cost", nullable = false, precision = 12, scale = 2)
    private BigDecimal laborCost;

    @CreationTimestamp
    @Column(name = "created_at")
    private Instant createdAt;

    @OneToMany(mappedBy = "shiftSales", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<WindowSales> windowSales = new ArrayList<>();
}
