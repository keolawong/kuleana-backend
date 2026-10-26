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
@Table(name = "device", schema = "asset", uniqueConstraints = {
        @UniqueConstraint(name = "uq_device_id", columnNames = {"device_id"}),
        @UniqueConstraint(name = "uq_device_number", columnNames = {"device_number"})
})
public class Device {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "id", nullable = false)
    private UUID id;

    @Column(name = "device_id", nullable = false, length = 100)
    private String deviceId;

    @Column(name = "device_number", nullable = false, length = 100)
    private String deviceNumber;

    @Builder.Default
    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;

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
