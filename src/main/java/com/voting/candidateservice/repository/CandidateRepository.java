package com.voting.candidateservice.repository;

import com.voting.candidateservice.model.Candidate;
import com.voting.candidateservice.model.enums.CandidateStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CandidateRepository extends JpaRepository<Candidate, Long> {

    // Fetch all candidates for a specific election
    List<Candidate> findByElectionIdAndIsDeletedFalse(UUID electionId);

    // Fetch only active candidates for an election
    List<Candidate> findByElectionIdAndStatusAndIsDeletedFalse(UUID electionId, CandidateStatus status);

    // Fetch all active candidates (paginated)
    Page<Candidate> findAllByIsDeletedFalse(Pageable pageable);

    // Check if a candidate exists and is not deleted (by Public ID)
    boolean existsByExternalIdAndIsDeletedFalse(UUID externalId);

    // Find candidate by Public ID
    Optional<Candidate> findByExternalIdAndIsDeletedFalse(UUID externalId);

    // Check if a candidate with the same name exists in a specific election
    boolean existsByNameAndElectionIdAndIsDeletedFalse(String name, UUID electionId);
}
