package com.kahua.registration.entity;

import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

@Entity
@Table(name = "registration_submission_selections", schema = "registration")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RegistrationSubmissionSelection {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "submission_id", nullable = false)
    private RegistrationSubmission submission;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "option_id", nullable = false)
    private RegistrationOption option;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private SelectionStatus status;
}
