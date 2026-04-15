package com.voting.candidate_service.repository;

import com.voting.candidate_service.model.Candidate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ICandidateRepository extends JpaRepository<Candidate, UUID> {

    // Fetch all active candidates for a specific election
    List<Candidate> findByElectionIdAndIsDeletedFalse(UUID electionId);

    // Fetch all active candidates (paginated)
    Page<Candidate> findAllByIsDeletedFalse(Pageable pageable);

    // Check if a candidate with the same name exists in a specific election
    boolean existsByNameAndElectionIdAndIsDeletedFalse(String name, UUID electionId);
}
