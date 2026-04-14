package com.voting.candidate_service.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CandidateRequestDTO {

    @NotBlank(message = "Candidate name cannot be blank")
    private String name;

    private String party; // Optional

    @NotNull(message = "Election ID must be provided")
    private UUID electionId;
}
