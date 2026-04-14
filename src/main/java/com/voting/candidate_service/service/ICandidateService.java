package com.voting.candidate_service.service;

import com.voting.candidate_service.dto.CandidateRequestDTO;
import com.voting.candidate_service.dto.CandidateResponseDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface ICandidateService {

    CandidateResponseDTO createCandidate(CandidateRequestDTO requestDTO);

    CandidateResponseDTO getCandidateById(UUID id);

    Page<CandidateResponseDTO> getAllCandidates(Pageable pageable);

    CandidateResponseDTO updateCandidate(UUID id, CandidateRequestDTO requestDTO);

    void deleteCandidate(UUID id);
}
