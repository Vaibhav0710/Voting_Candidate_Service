package com.voting.candidateservice.dto;

import com.voting.candidateservice.model.enums.CandidateStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class CandidateValidationDTO {
    private UUID candidateId;
    private UUID electionId;
    private boolean isValid;
    private CandidateStatus currentStatus;
    private String message;
}
