package com.voting.candidateservice.service;

import com.voting.candidateservice.dto.CandidateRequestDTO;
import com.voting.candidateservice.dto.CandidateResponseDTO;
import com.voting.candidateservice.dto.CandidateStatusUpdateDTO;
import com.voting.candidateservice.model.enums.CandidateStatus;
import com.voting.candidateservice.repository.CandidateRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.test.context.ActiveProfiles;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

/**
 * Cache annotations on {@link CandidateServiceImpl}, checked through the real Spring proxy
 * (in-memory cache in tests; Redis in production).
 */
@SpringBootTest
@ActiveProfiles("test")
class CandidateCachingTest {

    @Autowired private CandidateService candidateService;
    @SpyBean private CandidateRepository candidateRepository;
    @MockBean private CandidateEventProducer candidateEventProducer;

    private UUID electionId;
    private CandidateResponseDTO candidate;

    @BeforeEach
    void setUp() {
        electionId = UUID.randomUUID();
        candidate = candidateService.createCandidate(request("Asha Rao"));
        clearInvocations(candidateRepository);
    }

    private CandidateRequestDTO request(String name) {
        return CandidateRequestDTO.builder().name(name).party("Civic Party").electionId(electionId).build();
    }

    @Test
    void getById_secondCallIsServedFromCache() {
        candidateService.getCandidateById(candidate.getId());
        candidateService.getCandidateById(candidate.getId());

        verify(candidateRepository, times(1)).findByExternalIdAndIsDeletedFalse(candidate.getId());
    }

    @Test
    void statusUpdate_evictsCachedCandidate() {
        candidateService.getCandidateById(candidate.getId());

        candidateService.updateCandidateStatus(candidate.getId(),
                CandidateStatusUpdateDTO.builder().status(CandidateStatus.DISQUALIFIED).build());

        assertThat(candidateService.getCandidateById(candidate.getId()).getStatus())
                .isEqualTo(CandidateStatus.DISQUALIFIED);
    }

    @Test
    void electionList_isCachedUntilACandidateIsAdded() {
        candidateService.getCandidatesByElection(electionId);
        candidateService.getCandidatesByElection(electionId);
        verify(candidateRepository, times(1)).findByElectionIdAndIsDeletedFalse(any());

        candidateService.createCandidate(request("Kiran Mehta"));

        assertThat(candidateService.getCandidatesByElection(electionId)).hasSize(2);
    }

    @Test
    void activeList_dropsCandidateAfterWithdrawal() {
        assertThat(candidateService.getActiveCandidatesByElection(electionId)).hasSize(1);

        candidateService.updateCandidateStatus(candidate.getId(),
                CandidateStatusUpdateDTO.builder().status(CandidateStatus.WITHDRAWN).build());

        assertThat(candidateService.getActiveCandidatesByElection(electionId)).isEmpty();
    }

    @Test
    void delete_evictsCandidateAndLists() {
        candidateService.getCandidateById(candidate.getId());
        candidateService.getCandidatesByElection(electionId);

        candidateService.deleteCandidate(candidate.getId());

        assertThat(candidateService.getCandidatesByElection(electionId)).isEmpty();
        assertThat(candidateService.candidateExists(candidate.getId())).isFalse();
    }
}
