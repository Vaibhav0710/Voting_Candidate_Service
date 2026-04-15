package com.voting.candidate_service.service;

import com.voting.candidate_service.dto.CandidateRequestDTO;
import com.voting.candidate_service.dto.CandidateResponseDTO;
import com.voting.candidate_service.dto.BulkCandidateRequestDTO;
import com.voting.candidate_service.dto.CandidateStatusUpdateDTO;
import com.voting.candidate_service.dto.CandidateUpdateDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.UUID;

public interface ICandidateService {

    CandidateResponseDTO createCandidate(CandidateRequestDTO requestDTO);

    CandidateResponseDTO getCandidateById(UUID id);

    Page<CandidateResponseDTO> getAllCandidates(Pageable pageable);

    CandidateResponseDTO updateCandidate(UUID id, CandidateUpdateDTO updateDTO);

    CandidateResponseDTO updateCandidateStatus(UUID id, CandidateStatusUpdateDTO statusUpdateDTO);

    List<CandidateResponseDTO> bulkRegisterCandidates(BulkCandidateRequestDTO bulkRequestDTO);

    void deleteCandidate(UUID id);
}
