package com.voting.candidate_service.controller;

import com.voting.candidate_service.dto.CandidateRequestDTO;
import com.voting.candidate_service.dto.CandidateResponseDTO;
import com.voting.candidate_service.service.ICandidateService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/candidates")
public class CandidateController {

    @Autowired
    private ICandidateService candidateService;

    @PostMapping
    public ResponseEntity<CandidateResponseDTO> createCandidate(@Valid @RequestBody CandidateRequestDTO requestDTO) {
        CandidateResponseDTO response = candidateService.createCandidate(requestDTO);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @GetMapping("/{id}")
    public ResponseEntity<CandidateResponseDTO> getCandidateById(@PathVariable UUID id) {
        CandidateResponseDTO response = candidateService.getCandidateById(id);
        return ResponseEntity.ok(response);
    }

    @GetMapping
    public ResponseEntity<Page<CandidateResponseDTO>> getAllCandidates(Pageable pageable) {
        Page<CandidateResponseDTO> response = candidateService.getAllCandidates(pageable);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{id}")
    public ResponseEntity<CandidateResponseDTO> updateCandidate(
            @PathVariable UUID id, 
            @Valid @RequestBody CandidateRequestDTO requestDTO) {
        CandidateResponseDTO response = candidateService.updateCandidate(id, requestDTO);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteCandidate(@PathVariable UUID id) {
        candidateService.deleteCandidate(id);
        return ResponseEntity.noContent().build();
    }
}
