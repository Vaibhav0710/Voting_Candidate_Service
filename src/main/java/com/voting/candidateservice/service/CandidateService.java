package com.voting.candidateservice.service;

import com.voting.candidateservice.dto.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.UUID;

public interface CandidateService {

    CandidateResponseDTO createCandidate(CandidateRequestDTO requestDTO);

    CandidateResponseDTO getCandidateById(UUID id);

    Page<CandidateResponseDTO> getAllCandidates(Pageable pageable);

    CandidateResponseDTO updateCandidate(UUID id, CandidateUpdateDTO updateDTO);

    CandidateResponseDTO updateCandidateStatus(UUID id, CandidateStatusUpdateDTO statusUpdateDTO);

    List<CandidateResponseDTO> bulkRegisterCandidates(BulkCandidateRequestDTO bulkRequestDTO);

    void deleteCandidate(UUID id);
}
