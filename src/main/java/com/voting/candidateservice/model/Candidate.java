package com.voting.candidateservice.model;

import com.voting.candidateservice.model.enums.CandidateStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "candidates")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Candidate {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "external_id", unique = true, nullable = false, updatable = false)
    private UUID externalId;

    @Column(nullable = false)
    private String name;

    @Column(name = "party")
    private String party;

    @Column(name = "election_id", nullable = false)
    private UUID electionId;

    @Enumerated(EnumType.STRING)
    @Builder.Default
    @Column(nullable = false)
    private CandidateStatus status = CandidateStatus.ACTIVE;

    @Builder.Default
    @Column(name = "is_deleted", nullable = false)
    private boolean isDeleted = false;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    public void ensureExternalId() {
        if (externalId == null) {
            this.externalId = UUID.randomUUID();
        }
    }
}
