package com.voting.candidateservice.service;

import com.voting.candidateservice.audit.AuditLogger;
import com.voting.candidateservice.config.CacheConfig;
import com.voting.candidateservice.dto.*;
import com.voting.candidateservice.exception.DuplicateResourceException;
import com.voting.candidateservice.exception.ResourceNotFoundException;
import com.voting.candidateservice.mapper.CandidateMapper;
import com.voting.candidateservice.model.Candidate;
import com.voting.candidateservice.model.enums.CandidateStatus;
import com.voting.candidateservice.repository.CandidateRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CandidateServiceImpl implements CandidateService {

    private final CandidateRepository candidateRepository;
    private final CandidateMapper candidateMapper;
    private final CandidateEventProducer candidateEventProducer;
    private final AuditLogger auditLogger;

    @Override
    @Transactional
    @CacheEvict(cacheNames = {CacheConfig.CANDIDATES_BY_ELECTION, CacheConfig.ACTIVE_CANDIDATES_BY_ELECTION},
            key = "#requestDTO.electionId")
    public CandidateResponseDTO createCandidate(CandidateRequestDTO requestDTO) {
        // Prevent double registration
        if (candidateRepository.existsByNameAndElectionIdAndIsDeletedFalse(requestDTO.getName(),
                requestDTO.getElectionId())) {
            throw new DuplicateResourceException(
                    "Candidate with name " + requestDTO.getName() + " already registered for this election");
        }

        Candidate candidate = candidateMapper.toEntity(requestDTO);
        Candidate savedCandidate = candidateRepository.save(candidate);
        CandidateResponseDTO response = candidateMapper.toResponseDTO(savedCandidate);
        candidateEventProducer.publishCandidateCreated(response);
        auditLogger.recordForCurrentUser("CANDIDATE_CREATED", "candidate:" + response.getId(), "SUCCESS");
        return response;
    }

    @Override
    @Cacheable(cacheNames = CacheConfig.CANDIDATE, key = "#id")
    public CandidateResponseDTO getCandidateById(UUID id) {
        Candidate candidate = findCandidateByExternalId(id);
        return candidateMapper.toResponseDTO(candidate);
    }

    @Override
    public Page<CandidateResponseDTO> getAllCandidates(Pageable pageable) {
        return candidateRepository.findAllByIsDeletedFalse(pageable)
                .map(candidateMapper::toResponseDTO);
    }

    @Override
    @Transactional
    @Caching(evict = {
            @CacheEvict(cacheNames = CacheConfig.CANDIDATE, key = "#id"),
            @CacheEvict(cacheNames = {CacheConfig.CANDIDATES_BY_ELECTION, CacheConfig.ACTIVE_CANDIDATES_BY_ELECTION},
                    allEntries = true)
    })
    public CandidateResponseDTO updateCandidate(UUID id, CandidateUpdateDTO updateDTO) {
        Candidate candidate = findCandidateByExternalId(id);

        candidate.setName(updateDTO.getName());
        candidate.setParty(updateDTO.getParty());

        Candidate updatedCandidate = candidateRepository.save(candidate);
        auditLogger.recordForCurrentUser("CANDIDATE_UPDATED", "candidate:" + id, "SUCCESS");
        return candidateMapper.toResponseDTO(updatedCandidate);
    }

    @Override
    @Transactional
    @Caching(evict = {
            @CacheEvict(cacheNames = CacheConfig.CANDIDATE, key = "#id"),
            @CacheEvict(cacheNames = {CacheConfig.CANDIDATES_BY_ELECTION, CacheConfig.ACTIVE_CANDIDATES_BY_ELECTION},
                    allEntries = true)
    })
    public CandidateResponseDTO updateCandidateStatus(UUID id, CandidateStatusUpdateDTO statusUpdateDTO) {
        Candidate candidate = findCandidateByExternalId(id);
        candidate.setStatus(statusUpdateDTO.getStatus());
        Candidate updatedCandidate = candidateRepository.save(candidate);
        CandidateResponseDTO response = candidateMapper.toResponseDTO(updatedCandidate);
        candidateEventProducer.publishCandidateStatusChanged(response);
        auditLogger.recordForCurrentUser("CANDIDATE_STATUS_CHANGED", "candidate:" + id, statusUpdateDTO.getStatus().name());
        return response;
    }

    @Override
    @Transactional
    @CacheEvict(cacheNames = {CacheConfig.CANDIDATES_BY_ELECTION, CacheConfig.ACTIVE_CANDIDATES_BY_ELECTION},
            allEntries = true)
    public List<CandidateResponseDTO> bulkRegisterCandidates(BulkCandidateRequestDTO bulkRequestDTO) {
        return bulkRequestDTO.getCandidates().stream()
                .map(this::createCandidate)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    @Caching(evict = {
            @CacheEvict(cacheNames = CacheConfig.CANDIDATE, key = "#id"),
            @CacheEvict(cacheNames = {CacheConfig.CANDIDATES_BY_ELECTION, CacheConfig.ACTIVE_CANDIDATES_BY_ELECTION},
                    allEntries = true)
    })
    public void deleteCandidate(UUID id) {
        Candidate candidate = findCandidateByExternalId(id);
        candidate.setDeleted(true);
        candidateRepository.save(candidate);
        candidateEventProducer.publishCandidateDeleted(candidateMapper.toResponseDTO(candidate));
        auditLogger.recordForCurrentUser("CANDIDATE_DELETED", "candidate:" + id, "SUCCESS");
    }

    @Override
    @Cacheable(cacheNames = CacheConfig.CANDIDATES_BY_ELECTION, key = "#electionId")
    public List<CandidateResponseDTO> getCandidatesByElection(UUID electionId) {
        return candidateRepository.findByElectionIdAndIsDeletedFalse(electionId)
                .stream()
                .map(candidateMapper::toResponseDTO)
                .collect(Collectors.toList());
    }

    @Override
    @Cacheable(cacheNames = CacheConfig.ACTIVE_CANDIDATES_BY_ELECTION, key = "#electionId")
    public List<CandidateResponseDTO> getActiveCandidatesByElection(UUID electionId) {
        return candidateRepository.findByElectionIdAndStatusAndIsDeletedFalse(electionId,
                        CandidateStatus.ACTIVE)
                .stream()
                .map(candidateMapper::toResponseDTO)
                .collect(Collectors.toList());
    }

    @Override
    public boolean candidateExists(UUID id) {
        return candidateRepository.existsByExternalIdAndIsDeletedFalse(id);
    }

    @Override
    public CandidateValidationDTO validateCandidateForElection(UUID candidateId, UUID electionId) {
        Candidate candidate = candidateRepository.findByExternalIdAndIsDeletedFalse(candidateId)
                .orElse(null);

        if (candidate == null || candidate.isDeleted()) {
            return CandidateValidationDTO.builder()
                    .candidateId(candidateId)
                    .electionId(electionId)
                    .isValid(false)
                    .message("Candidate not found")
                    .build();
        }

        if (!candidate.getElectionId().equals(electionId)) {
            return CandidateValidationDTO.builder()
                    .candidateId(candidateId)
                    .electionId(electionId)
                    .isValid(false)
                    .currentStatus(candidate.getStatus())
                    .message("Candidate does not belong to this election")
                    .build();
        }

        boolean isActive = candidate.getStatus() == CandidateStatus.ACTIVE;

        return CandidateValidationDTO.builder()
                .candidateId(candidateId)
                .electionId(electionId)
                .isValid(isActive)
                .currentStatus(candidate.getStatus())
                .message(isActive ? "Valid" : "Candidate is " + candidate.getStatus())
                .build();
    }

    // Helper method to find candidate by Public ID or throw exception
    private Candidate findCandidateByExternalId(UUID externalId) {
        return candidateRepository.findByExternalIdAndIsDeletedFalse(externalId)
                .orElseThrow(() -> new ResourceNotFoundException("Candidate not found with id: " + externalId));
    }
}
