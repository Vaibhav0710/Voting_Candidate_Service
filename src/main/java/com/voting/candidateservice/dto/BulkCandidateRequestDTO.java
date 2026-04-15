package com.voting.candidateservice.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class BulkCandidateRequestDTO {
    @NotEmpty(message = "Candidate list cannot be empty")
    @Valid
    private List<CandidateRequestDTO> candidates;
}
