package com.voting.candidateservice.repository;

import com.voting.candidateservice.model.Candidate;
import com.voting.candidateservice.model.enums.CandidateStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.TestPropertySource;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
@TestPropertySource(properties = {
        "spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.H2Dialect",
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
class CandidateRepositoryTest {

    @Autowired
    private CandidateRepository candidateRepository;

    private UUID electionId = UUID.randomUUID();
    private Candidate candidate;

    @BeforeEach
    void setUp() {
        candidate = new Candidate();
        candidate.setName("Vikram Singh");
        candidate.setParty("Bharat Vikas Mandal");
        candidate.setElectionId(electionId);
        candidate.setStatus(CandidateStatus.ACTIVE);
        candidate.setExternalId(UUID.randomUUID());
        candidate.setDeleted(false);
    }

    @Test
    void findByExternalIdAndIsDeletedFalse_Success() {
        Candidate saved = candidateRepository.save(candidate);
        Optional<Candidate> found = candidateRepository.findByExternalIdAndIsDeletedFalse(saved.getExternalId());
        assertTrue(found.isPresent());
        assertEquals("Vikram Singh", found.get().getName());
    }

    @Test
    void findByElectionIdAndIsDeletedFalse_Success() {
        candidateRepository.save(candidate);
        List<Candidate> candidates = candidateRepository.findByElectionIdAndIsDeletedFalse(electionId);
        assertFalse(candidates.isEmpty());
        assertEquals(1, candidates.size());
    }

    @Test
    void existsByNameAndElectionIdAndIsDeletedFalse_ReturnsTrue() {
        candidateRepository.save(candidate);
        boolean exists = candidateRepository.existsByNameAndElectionIdAndIsDeletedFalse("Vikram Singh", electionId);
        assertTrue(exists);
    }

    @Test
    void findAllByIsDeletedFalse_FiltersDeleted() {
        Candidate deletedCandidate = new Candidate();
        deletedCandidate.setName("Neha Gupta");
        deletedCandidate.setElectionId(electionId);
        deletedCandidate.setExternalId(UUID.randomUUID());
        deletedCandidate.setDeleted(true);
        candidateRepository.save(deletedCandidate);

        Page<Candidate> result = candidateRepository.findAllByIsDeletedFalse(PageRequest.of(0, 10));

        long count = result.getContent().stream().filter(c -> c.getName().equals("Neha Gupta")).count();
        assertEquals(0, count, "Deleted candidate should not be returned");
    }
}
