package com.voting.candidate_service.dto;

import jakarta.validation.constraints.NotBlank;
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
    private String name;
    
    @NotBlank(message = "Party name cannot be blank")
    private String party;
}
