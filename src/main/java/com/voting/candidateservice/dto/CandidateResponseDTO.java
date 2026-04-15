package com.voting.candidateservice.dto;

import com.voting.candidateservice.model.enums.CandidateStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CandidateResponseDTO {

    private UUID id;
    private String name;
    private String party;
    private UUID electionId;
    private CandidateStatus status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
