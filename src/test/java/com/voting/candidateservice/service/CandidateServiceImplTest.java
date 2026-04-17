package com.voting.candidateservice.service;

import com.voting.candidateservice.dto.BulkCandidateRequestDTO;
import com.voting.candidateservice.dto.CandidateRequestDTO;
import com.voting.candidateservice.dto.CandidateResponseDTO;
import com.voting.candidateservice.dto.CandidateStatusUpdateDTO;
import com.voting.candidateservice.dto.CandidateUpdateDTO;
import com.voting.candidateservice.dto.CandidateValidationDTO;
import com.voting.candidateservice.exception.DuplicateResourceException;
import com.voting.candidateservice.exception.ResourceNotFoundException;
import com.voting.candidateservice.mapper.CandidateMapper;
import com.voting.candidateservice.model.Candidate;
import com.voting.candidateservice.model.enums.CandidateStatus;
import com.voting.candidateservice.repository.CandidateRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CandidateServiceImplTest {

    @Mock
    private CandidateRepository candidateRepository;

    @Mock
    private CandidateMapper candidateMapper;

    @InjectMocks
    private CandidateServiceImpl candidateService;

    private Candidate candidate;
    private CandidateRequestDTO requestDTO;
    private CandidateResponseDTO responseDTO;

    private final UUID candidateId = UUID.randomUUID();
    private final UUID electionId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        candidate = new Candidate();
        candidate.setId(1L);
        candidate.setExternalId(candidateId);
        candidate.setName("Rahul Sharma");
        candidate.setParty("Swaraj Party");
        candidate.setElectionId(electionId);
        candidate.setStatus(CandidateStatus.ACTIVE);
        candidate.setDeleted(false);

        requestDTO = new CandidateRequestDTO();
        requestDTO.setName("Rahul Sharma");
        requestDTO.setParty("Swaraj Party");
        requestDTO.setElectionId(electionId);

        responseDTO = new CandidateResponseDTO();
        responseDTO.setId(candidateId);
        responseDTO.setName("Rahul Sharma");
        responseDTO.setParty("Swaraj Party");
        responseDTO.setElectionId(electionId);
        responseDTO.setStatus(CandidateStatus.ACTIVE);
    }

    @Test
    void createCandidate_Success() {
        when(candidateRepository.existsByNameAndElectionIdAndIsDeletedFalse(requestDTO.getName(),
                requestDTO.getElectionId()))
                .thenReturn(false);
        when(candidateMapper.toEntity(requestDTO)).thenReturn(candidate);
        when(candidateRepository.save(candidate)).thenReturn(candidate);
        when(candidateMapper.toResponseDTO(candidate)).thenReturn(responseDTO);

        CandidateResponseDTO result = candidateService.createCandidate(requestDTO);

        assertNotNull(result);
        assertEquals("Rahul Sharma", result.getName());
        verify(candidateRepository, times(1)).save(candidate);
    }

    @Test
    void createCandidate_DuplicateResourceThrowsException() {
        when(candidateRepository.existsByNameAndElectionIdAndIsDeletedFalse(requestDTO.getName(),
                requestDTO.getElectionId()))
                .thenReturn(true);

        assertThrows(DuplicateResourceException.class, () -> candidateService.createCandidate(requestDTO));
        verify(candidateRepository, never()).save(any(Candidate.class));
    }

    @Test
    void getCandidateById_Success() {
        when(candidateRepository.findByExternalIdAndIsDeletedFalse(candidateId)).thenReturn(Optional.of(candidate));
        when(candidateMapper.toResponseDTO(candidate)).thenReturn(responseDTO);

        CandidateResponseDTO result = candidateService.getCandidateById(candidateId);

        assertNotNull(result);
        assertEquals(candidateId, result.getId());
    }

    @Test
    void getCandidateById_NotFoundThrowsException() {
        when(candidateRepository.findByExternalIdAndIsDeletedFalse(candidateId)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> candidateService.getCandidateById(candidateId));
    }

    @Test
    void getAllCandidates_ReturnsPage() {
        Page<Candidate> candidatePage = new PageImpl<>(List.of(candidate));
        Pageable pageable = PageRequest.of(0, 10);

        when(candidateRepository.findAllByIsDeletedFalse(pageable)).thenReturn(candidatePage);
        when(candidateMapper.toResponseDTO(candidate)).thenReturn(responseDTO);

        Page<CandidateResponseDTO> result = candidateService.getAllCandidates(pageable);

        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
        assertEquals("Rahul Sharma", result.getContent().get(0).getName());
    }

    @Test
    void updateCandidate_Success() {
        CandidateUpdateDTO updateDTO = new CandidateUpdateDTO();
        updateDTO.setName("Priya Patel");
        updateDTO.setParty("National Front");

        when(candidateRepository.findByExternalIdAndIsDeletedFalse(candidateId)).thenReturn(Optional.of(candidate));

        Candidate updatedCandidate = new Candidate();
        updatedCandidate.setName("Priya Patel");
        updatedCandidate.setParty("National Front");

        CandidateResponseDTO updatedResponse = new CandidateResponseDTO();
        updatedResponse.setName("Priya Patel");

        when(candidateRepository.save(any(Candidate.class))).thenReturn(updatedCandidate);
        when(candidateMapper.toResponseDTO(updatedCandidate)).thenReturn(updatedResponse);

        CandidateResponseDTO result = candidateService.updateCandidate(candidateId, updateDTO);

        assertNotNull(result);
        assertEquals("Priya Patel", result.getName());
    }

    @Test
    void updateCandidateStatus_Success() {
        CandidateStatusUpdateDTO statusDTO = new CandidateStatusUpdateDTO();
        statusDTO.setStatus(CandidateStatus.WITHDRAWN);

        when(candidateRepository.findByExternalIdAndIsDeletedFalse(candidateId)).thenReturn(Optional.of(candidate));

        Candidate updatedCandidate = new Candidate();
        updatedCandidate.setStatus(CandidateStatus.WITHDRAWN);

        CandidateResponseDTO updatedResponse = new CandidateResponseDTO();
        updatedResponse.setStatus(CandidateStatus.WITHDRAWN);

        when(candidateRepository.save(any(Candidate.class))).thenReturn(updatedCandidate);
        when(candidateMapper.toResponseDTO(updatedCandidate)).thenReturn(updatedResponse);

        CandidateResponseDTO result = candidateService.updateCandidateStatus(candidateId, statusDTO);

        assertNotNull(result);
        assertEquals(CandidateStatus.WITHDRAWN, result.getStatus());
    }

    @Test
    void deleteCandidate_Success() {
        when(candidateRepository.findByExternalIdAndIsDeletedFalse(candidateId)).thenReturn(Optional.of(candidate));

        candidateService.deleteCandidate(candidateId);

        assertTrue(candidate.isDeleted());
        verify(candidateRepository, times(1)).save(candidate);
    }

    @Test
    void getCandidatesByElection_Success() {
        when(candidateRepository.findByElectionIdAndIsDeletedFalse(electionId))
                .thenReturn(Collections.singletonList(candidate));
        when(candidateMapper.toResponseDTO(candidate)).thenReturn(responseDTO);

        List<CandidateResponseDTO> result = candidateService.getCandidatesByElection(electionId);

        assertFalse(result.isEmpty());
        assertEquals(1, result.size());
        assertEquals(electionId, result.get(0).getElectionId());
    }

    @Test
    void validateCandidateForElection_Valid() {
        when(candidateRepository.findByExternalIdAndIsDeletedFalse(candidateId)).thenReturn(Optional.of(candidate));

        CandidateValidationDTO result = candidateService.validateCandidateForElection(candidateId, electionId);

        assertTrue(result.isValid());
        assertEquals("Valid", result.getMessage());
    }

    @Test
    void validateCandidateForElection_InvalidElection() {
        candidate.setElectionId(UUID.randomUUID()); // different election
        when(candidateRepository.findByExternalIdAndIsDeletedFalse(candidateId)).thenReturn(Optional.of(candidate));

        CandidateValidationDTO result = candidateService.validateCandidateForElection(candidateId, electionId);

        assertFalse(result.isValid());
        assertEquals("Candidate does not belong to this election", result.getMessage());
    }

    @Test
    void validateCandidateForElection_WithdrawnCandidate() {
        candidate.setStatus(CandidateStatus.WITHDRAWN);
        when(candidateRepository.findByExternalIdAndIsDeletedFalse(candidateId)).thenReturn(Optional.of(candidate));

        CandidateValidationDTO result = candidateService.validateCandidateForElection(candidateId, electionId);

        assertFalse(result.isValid());
        assertEquals("Candidate is WITHDRAWN", result.getMessage());
    }

    @Test
    void bulkRegisterCandidates_Success() {
        CandidateRequestDTO req1 = new CandidateRequestDTO();
        req1.setName("Amit Verma");
        req1.setParty("Bharat Vikas Mandal");
        req1.setElectionId(electionId);

        CandidateRequestDTO req2 = new CandidateRequestDTO();
        req2.setName("Neha Gupta");
        req2.setParty("Lok Shakti Dal");
        req2.setElectionId(electionId);

        BulkCandidateRequestDTO bulkRequest = new BulkCandidateRequestDTO(List.of(req1, req2));

        // Mock for mapping
        when(candidateMapper.toResponseDTO(any(Candidate.class))).thenAnswer(invocation -> {
            Candidate c = invocation.getArgument(0);
            CandidateResponseDTO dto = new CandidateResponseDTO();
            dto.setName(c.getName()); // Assuming mapper uses entity name
            return dto;
        });

        // Ensure entities have names for the mock to work
        Candidate entity1 = new Candidate();
        entity1.setName("Amit Verma");
        Candidate entity2 = new Candidate();
        entity2.setName("Neha Gupta");
        when(candidateMapper.toEntity(req1)).thenReturn(entity1);
        when(candidateMapper.toEntity(req2)).thenReturn(entity2);

        when(candidateRepository.save(any(Candidate.class))).thenAnswer(i -> i.getArgument(0));

        List<CandidateResponseDTO> result = candidateService.bulkRegisterCandidates(bulkRequest);

        assertEquals(2, result.size());
        assertEquals("Amit Verma", result.get(0).getName());
        assertEquals("Neha Gupta", result.get(1).getName());
    }

    @Test
    void getActiveCandidatesByElection_FiltersStatus() {
        Candidate activeCandidate = new Candidate();
        activeCandidate.setStatus(CandidateStatus.ACTIVE);

        when(candidateRepository.findByElectionIdAndStatusAndIsDeletedFalse(electionId, CandidateStatus.ACTIVE))
                .thenReturn(List.of(activeCandidate));
        when(candidateMapper.toResponseDTO(activeCandidate)).thenReturn(responseDTO);

        List<CandidateResponseDTO> result = candidateService.getActiveCandidatesByElection(electionId);

        assertFalse(result.isEmpty());
        assertEquals(CandidateStatus.ACTIVE, result.get(0).getStatus());
    }

    @Test
    void candidateExists_ReturnsTrue() {
        when(candidateRepository.existsByExternalIdAndIsDeletedFalse(candidateId)).thenReturn(true);
        assertTrue(candidateService.candidateExists(candidateId));
    }
}
