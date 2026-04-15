package com.voting.candidate_service.mapper;

import com.voting.candidate_service.dto.CandidateRequestDTO;
import com.voting.candidate_service.dto.CandidateResponseDTO;
import com.voting.candidate_service.model.Candidate;
import org.springframework.stereotype.Component;

@Component
public class CandidateMapper {

    public Candidate toEntity(CandidateRequestDTO requestDTO) {
        if (requestDTO == null) {
            return null;
        }
        return Candidate.builder()
                .name(requestDTO.getName())
                .party(requestDTO.getParty())
                .electionId(requestDTO.getElectionId())
                .build();
    }

    public CandidateResponseDTO toResponseDTO(Candidate candidate) {
        if (candidate == null) {
            return null;
        }
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
