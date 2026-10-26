package com.kuleanasystems.auth.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "user_pages", schema = "auth")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserPage {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "page_path", nullable = false, length = 255)
    private String pagePath;

    @Builder.Default
    @Column(name = "can_read", nullable = false)
    private Boolean canRead = true;

    @Builder.Default
    @Column(name = "can_write", nullable = false)
    private Boolean canWrite = false;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}
