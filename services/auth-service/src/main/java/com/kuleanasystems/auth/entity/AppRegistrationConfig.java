package com.kuleanasystems.auth.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "app_registration_configs", schema = "auth")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AppRegistrationConfig {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "app_name", nullable = false, unique = true, length = 50)
    private String appName;

    @Column(name = "display_title", nullable = false, length = 100)
    private String displayTitle;

    @Builder.Default
    @Column(name = "default_primary_role", nullable = false, length = 50)
    private String defaultPrimaryRole = "VIEWER";

    @Column(name = "default_roles", columnDefinition = "text[]")
    private List<String> defaultRoles;

    @Builder.Default
    @Column(name = "require_captcha", nullable = false)
    private Boolean requireCaptcha = true;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private OffsetDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private OffsetDateTime updatedAt;
}
