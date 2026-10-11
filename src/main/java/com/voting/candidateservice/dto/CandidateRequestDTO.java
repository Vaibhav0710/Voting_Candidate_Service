package com.voting.candidateservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CandidateRequestDTO {

    @NotBlank(message = "Candidate name cannot be blank")
    @Size(max = 100, message = "Candidate name must be at most 100 characters")
    private String name;

    @Size(max = 100, message = "Party name must be at most 100 characters")
    private String party; // Optional

    @NotNull(message = "Election ID must be provided")
    private UUID electionId;
}
