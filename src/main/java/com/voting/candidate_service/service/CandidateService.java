package com.voting.candidate_service.service;

import com.voting.candidate_service.dto.*;
import com.voting.candidate_service.exception.DuplicateResourceException;
import com.voting.candidate_service.exception.ResourceNotFoundException;
import com.voting.candidate_service.mapper.CandidateMapper;
import com.voting.candidate_service.model.Candidate;
import com.voting.candidate_service.repository.ICandidateRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CandidateService implements ICandidateService {

    private final ICandidateRepository candidateRepository;
    private final CandidateMapper candidateMapper;

    @Override
    @Transactional
    public CandidateResponseDTO createCandidate(CandidateRequestDTO requestDTO) {
        // Prevent double registration
        if (candidateRepository.existsByNameAndElectionIdAndIsDeletedFalse(requestDTO.getName(), requestDTO.getElectionId())) {
            throw new DuplicateResourceException("Candidate with name " + requestDTO.getName() + " already registered for this election");
        }

        Candidate candidate = candidateMapper.toEntity(requestDTO);
        Candidate savedCandidate = candidateRepository.save(candidate);
        return candidateMapper.toResponseDTO(savedCandidate);
    }

    @Override
    public CandidateResponseDTO getCandidateById(UUID id) {
        Candidate candidate = findCandidateById(id);
        return candidateMapper.toResponseDTO(candidate);
    }

    @Override
    public Page<CandidateResponseDTO> getAllCandidates(Pageable pageable) {
        return candidateRepository.findAllByIsDeletedFalse(pageable)
                .map(candidateMapper::toResponseDTO);
    }

    @Override
    @Transactional
    public CandidateResponseDTO updateCandidate(UUID id, CandidateUpdateDTO updateDTO) {
        Candidate candidate = findCandidateById(id);
        
        candidate.setName(updateDTO.getName());
        candidate.setParty(updateDTO.getParty());
        
        Candidate updatedCandidate = candidateRepository.save(candidate);
        return candidateMapper.toResponseDTO(updatedCandidate);
    }

    @Override
    @Transactional
    public CandidateResponseDTO updateCandidateStatus(UUID id, CandidateStatusUpdateDTO statusUpdateDTO) {
        Candidate candidate = findCandidateById(id);
        candidate.setStatus(statusUpdateDTO.getStatus());
        Candidate updatedCandidate = candidateRepository.save(candidate);
        return candidateMapper.toResponseDTO(updatedCandidate);
    }

    @Override
    @Transactional
    public List<CandidateResponseDTO> bulkRegisterCandidates(BulkCandidateRequestDTO bulkRequestDTO) {
        return bulkRequestDTO.getCandidates().stream()
                .map(this::createCandidate)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
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
}
