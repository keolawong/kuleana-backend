package com.kuleanasystems.assets.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
@Entity
@Table(name = "ticket_book_history", schema = "asset")
public class TicketBookHistory {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "ticket_book_history_id", nullable = false)
    private UUID ticketBookHistoryId;

    @Column(name = "ticket_book_id")
    private UUID ticketBookId;

    @Column(name = "scan_code", length = 50)
    private String scanCode;

    @Column(name = "start_num", nullable = false)
    private Integer startNum;

    @Column(name = "end_num", nullable = false)
    private Integer endNum;

    @Column(name = "next_ticket")
    private Integer nextTicket;

    @Column(name = "is_reusable")
    private Boolean isReusable;

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

    @Column(name = "parent_ticket_book_id")
    private UUID parentTicketBookId;

    @Column(name = "process_id")
    private UUID processId;

    @Column(name = "created_by", length = 100)
    private String createdBy;

    @CreationTimestamp
    @Column(name = "created_at")
    private Instant createdAt;
}
