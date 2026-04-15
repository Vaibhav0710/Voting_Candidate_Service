package com.voting.candidate_service.dto;

import com.voting.candidate_service.model.enums.CandidateStatus;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class CandidateStatusUpdateDTO {
    @NotNull(message = "Candidate status cannot be null")
    private CandidateStatus status;
}
