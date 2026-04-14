package com.voting.candidate_service.service;

import com.voting.candidate_service.dto.CandidateRequestDTO;
import com.voting.candidate_service.dto.CandidateResponseDTO;
import com.voting.candidate_service.exception.ResourceNotFoundException;
import com.voting.candidate_service.model.Candidate;
import com.voting.candidate_service.repository.ICandidateRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class CandidateService implements ICandidateService {

    @Autowired
    private ICandidateRepository candidateRepository;

    @Override
    public CandidateResponseDTO createCandidate(CandidateRequestDTO requestDTO) {
        Candidate candidate = Candidate.builder()
                .name(requestDTO.getName())
                .party(requestDTO.getParty())
                .electionId(requestDTO.getElectionId())
                .build();
        
        Candidate savedCandidate = candidateRepository.save(candidate);
        return mapToResponseDTO(savedCandidate);
    }

    @Override
    public CandidateResponseDTO getCandidateById(UUID id) {
        Candidate candidate = findCandidateById(id);
        return mapToResponseDTO(candidate);
    }

    @Override
    public Page<CandidateResponseDTO> getAllCandidates(Pageable pageable) {
        return candidateRepository.findAllByIsDeletedFalse(pageable)
                .map(this::mapToResponseDTO);
    }

    @Override
    public CandidateResponseDTO updateCandidate(UUID id, CandidateRequestDTO requestDTO) {
        Candidate candidate = findCandidateById(id);
        
        candidate.setName(requestDTO.getName());
        candidate.setParty(requestDTO.getParty());
        candidate.setElectionId(requestDTO.getElectionId());
        
        Candidate updatedCandidate = candidateRepository.save(candidate);
        return mapToResponseDTO(updatedCandidate);
    }

    @Override
    public void deleteCandidate(UUID id) {
        Candidate candidate = findCandidateById(id);
        candidate.setDeleted(true);
        candidateRepository.save(candidate);
    }

    // Helper method to find candidate or throw exception
    private Candidate findCandidateById(UUID id) {
        Candidate candidate = candidateRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Candidate not found with id: " + id));
        
        if (candidate.isDeleted()) {
            throw new ResourceNotFoundException("Candidate not found with id: " + id);
        }
        return candidate;
    }

    // Helper method to map Entity to ResponseDTO
    private CandidateResponseDTO mapToResponseDTO(Candidate candidate) {
        return CandidateResponseDTO.builder()
                .id(candidate.getId())
                .name(candidate.getName())
                .party(candidate.getParty())
                .electionId(candidate.getElectionId())
                .status(candidate.getStatus())
                .createdAt(candidate.getCreatedAt())
                .updatedAt(candidate.getUpdatedAt())
                .build();
    }
}
