package com.voting.candidateservice.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
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
    @Size(max = 500, message = "At most 500 candidates per bulk request")
    @Valid
    private List<CandidateRequestDTO> candidates;
}
