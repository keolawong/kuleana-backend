package com.kuleanasystems.assets.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
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
@Table(name = "person", schema = "asset")
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class Person {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "person_id", nullable = false)
    private UUID personId;

    @Column(name = "first_name", length = 100)
    private String firstName;

    @Column(name = "last_name", length = 100)
    private String lastName;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "shift_id")
    private Shift shift;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "gate_id")
    private Gate gate;

    @Column(name = "organization", length = 150)
    private String organization = "Individual / None";

    @Column(name = "job_role", length = 50)
    private String jobRole = "Ticket Seller";

    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;

    @Column(name = "notes", length = 255)
    private String notes;

    @Column(name = "external_id", length = 100)
    private String externalId;

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
