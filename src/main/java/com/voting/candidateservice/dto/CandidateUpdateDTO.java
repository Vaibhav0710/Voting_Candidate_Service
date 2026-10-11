package com.voting.candidateservice.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class CandidateUpdateDTO {
    @NotBlank(message = "Candidate name cannot be blank")
    @Size(max = 100, message = "Candidate name must be at most 100 characters")
    private String name;

    @NotBlank(message = "Party name cannot be blank")
    @Size(max = 100, message = "Party name must be at most 100 characters")
    private String party;
}
