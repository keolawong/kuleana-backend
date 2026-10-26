package com.kuleanasystems.assets.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.UUID;

@Getter
@Setter
@Entity
@Table(name = "window_sales", schema = "asset")
public class WindowSales {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "window_sales_id", nullable = false)
    private UUID windowSalesId;

    @JsonIgnore
    @ManyToOne(optional = false)
    @JoinColumn(name = "shift_sales_id", nullable = false)
    private ShiftSales shiftSales;

    @Column(name = "window_name", nullable = false, length = 60)
    private String windowName;

    @Column(name = "sales_amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal salesAmount;
}
