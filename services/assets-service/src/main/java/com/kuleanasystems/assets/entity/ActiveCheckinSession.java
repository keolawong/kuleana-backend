package com.kuleanasystems.assets.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "active_checkin_session", schema = "asset")
public class ActiveCheckinSession {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "session_id", nullable = false)
    private UUID sessionId;

    @ManyToOne
    @JoinColumn(name = "gate_id", nullable = false)
    private Gate gate;

    @ManyToOne
    @JoinColumn(name = "shift_id", nullable = false)
    private Shift shift;

    @Column(name = "username", nullable = false, length = 100)
    private String username;

    @Column(name = "user_display_name", length = 150)
    private String userDisplayName;

    @Column(name = "status", nullable = false, length = 50)
    private String status;

    @Column(name = "last_heartbeat_at")
    private Instant lastHeartbeatAt;

    @CreationTimestamp
    @Column(name = "created_at")
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private Instant updatedAt;
}
