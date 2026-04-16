package com.voting.candidateservice.controller;

import com.voting.candidateservice.dto.*;
import com.voting.candidateservice.service.CandidateService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/candidates")
@RequiredArgsConstructor
public class CandidateController {

    private final CandidateService candidateService;

    @PostMapping
    public ResponseEntity<ApiResponse<CandidateResponseDTO>> createCandidate(
            @Valid @RequestBody CandidateRequestDTO requestDTO) {
        CandidateResponseDTO response = candidateService.createCandidate(requestDTO);
        return new ResponseEntity<>(
                ApiResponse.success(response, "Candidate created successfully"),
                HttpStatus.CREATED);
    }

    @PostMapping("/bulk")
    public ResponseEntity<ApiResponse<List<CandidateResponseDTO>>> bulkRegister(
            @Valid @RequestBody BulkCandidateRequestDTO bulkRequestDTO) {
        List<CandidateResponseDTO> response = candidateService.bulkRegisterCandidates(bulkRequestDTO);
        return new ResponseEntity<>(
                ApiResponse.success(response, "Bulk registration successful"),
                HttpStatus.CREATED);
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<CandidateResponseDTO>> getCandidateById(@PathVariable UUID id) {
        CandidateResponseDTO response = candidateService.getCandidateById(id);
        return ResponseEntity.ok(ApiResponse.success(response, "Candidate retrieved successfully"));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<Page<CandidateResponseDTO>>> getAllCandidates(Pageable pageable) {
        Page<CandidateResponseDTO> response = candidateService.getAllCandidates(pageable);
        return ResponseEntity.ok(ApiResponse.success(response, "Candidates retrieved successfully"));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<CandidateResponseDTO>> updateCandidate(
            @PathVariable UUID id,
            @Valid @RequestBody CandidateUpdateDTO updateDTO) {
        CandidateResponseDTO response = candidateService.updateCandidate(id, updateDTO);
        return ResponseEntity.ok(ApiResponse.success(response, "Candidate updated successfully"));
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<ApiResponse<CandidateResponseDTO>> updateStatus(
            @PathVariable UUID id,
            @Valid @RequestBody CandidateStatusUpdateDTO statusUpdateDTO) {
        CandidateResponseDTO response = candidateService.updateCandidateStatus(id, statusUpdateDTO);
        return ResponseEntity.ok(ApiResponse.success(response, "Candidate status updated successfully"));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteCandidate(@PathVariable UUID id) {
        candidateService.deleteCandidate(id);
        return ResponseEntity.ok(ApiResponse.success(null, "Candidate deleted successfully"));
    }

    @GetMapping("/election/{electionId}")
    public ResponseEntity<ApiResponse<List<CandidateResponseDTO>>> getCandidatesByElection(
            @PathVariable UUID electionId) {
        List<CandidateResponseDTO> response = candidateService.getCandidatesByElection(electionId);
        return ResponseEntity.ok(ApiResponse.success(response, "Candidates for election retrieved successfully"));
    }

    @GetMapping("/election/{electionId}/active")
    public ResponseEntity<ApiResponse<List<CandidateResponseDTO>>> getActiveCandidatesByElection(
            @PathVariable UUID electionId) {
        List<CandidateResponseDTO> response = candidateService.getActiveCandidatesByElection(electionId);
        return ResponseEntity.ok(ApiResponse.success(response, "Active candidates for election retrieved successfully"));
    }

    @GetMapping("/{id}/exists")
    public ResponseEntity<ApiResponse<Boolean>> candidateExists(@PathVariable UUID id) {
        boolean exists = candidateService.candidateExists(id);
        return ResponseEntity.ok(ApiResponse.success(exists, "Check completed"));
    }

    @GetMapping("/{id}/validate")
    public ResponseEntity<ApiResponse<CandidateValidationDTO>> validateCandidate(
            @PathVariable UUID id,
            @RequestParam UUID electionId) {
        CandidateValidationDTO response = candidateService.validateCandidateForElection(id, electionId);
        return ResponseEntity.ok(ApiResponse.success(response, "Validation completed"));
    }
}
