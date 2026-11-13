package com.kahua.registration.entity;

import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

@Entity
@Table(
    name = "form_account_quotas",
    schema = "registration",
    uniqueConstraints = @UniqueConstraint(name = "uq_form_account_quotas_form_user", columnNames = {"form_id", "account_user_id"})
)
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FormAccountQuota {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "form_id", nullable = false)
    private RegistrationForm form;

    @Column(name = "account_user_id", nullable = false, length = 255)
    private String accountUserId;

    @Column(name = "override_max_submissions")
    private Integer overrideMaxSubmissions;
}
