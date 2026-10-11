package com.voting.candidateservice.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.voting.candidateservice.dto.BulkCandidateRequestDTO;
import com.voting.candidateservice.dto.CandidateRequestDTO;
import com.voting.candidateservice.service.CandidateEventProducer;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.UUID;
import java.util.stream.IntStream;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public class CandidateSecurityIntegrationTest {

        @Autowired
        private MockMvc mockMvc;

        @Autowired
        private ObjectMapper objectMapper;

        @MockBean
        private CandidateEventProducer candidateEventProducer;

        private final UUID electionId = UUID.randomUUID();

        @Test
        void getCandidates_IsPublic() throws Exception {
                mockMvc.perform(get("/api/v1/candidates"))
                                .andExpect(status().isOk());
        }

        @Test
        void createCandidate_WithoutUser_IsUnauthorized() throws Exception {
                CandidateRequestDTO request = CandidateRequestDTO.builder()
                                .name("Test Candidate")
                                .party("Test Party")
                                .electionId(electionId)
                                .build();

                mockMvc.perform(post("/api/v1/candidates")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                                .andExpect(status().isUnauthorized());
        }

        @Test
        @WithMockUser(authorities = "ROLE_VOTER")
        void createCandidate_WithVoterRole_IsForbidden() throws Exception {
                CandidateRequestDTO request = CandidateRequestDTO.builder()
                                .name("Test Candidate")
                                .party("Test Party")
                                .electionId(electionId)
                                .build();

                mockMvc.perform(post("/api/v1/candidates")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                                .andExpect(status().isForbidden());
        }

        @Test
        @WithMockUser(authorities = "ROLE_ADMIN")
        void createCandidate_WithAdminRole_IsCreatedOrError() throws Exception {
                CandidateRequestDTO request = CandidateRequestDTO.builder()
                                .name("Test Candidate")
                                .party("Test Party")
                                .electionId(electionId)
                                .build();

                // The important part is it's NOT 403.
                mockMvc.perform(post("/api/v1/candidates")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                                .andExpect(status().isCreated());
        }

        @Test
        void deleteCandidate_WithoutUser_IsUnauthorized() throws Exception {
                mockMvc.perform(delete("/api/v1/candidates/{id}", UUID.randomUUID()))
                                .andExpect(status().isUnauthorized());
        }

        @Test
        @WithMockUser(authorities = "ROLE_ADMIN")
        void deleteCandidate_WithAdminRole_IsNotForbidden() throws Exception {
                // We expect 404 because the ID doesn't exist, but 404 means we PASSED security!
                mockMvc.perform(delete("/api/v1/candidates/{id}", UUID.randomUUID()))
                                .andExpect(status().isNotFound());
        }

        @Test
        @WithMockUser(authorities = "ROLE_VOTER")
        void changeStatus_WithVoterRole_IsForbidden() throws Exception {
                mockMvc.perform(patch("/api/v1/candidates/{id}/status", UUID.randomUUID())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"status\":\"DISQUALIFIED\"}"))
                                .andExpect(status().isForbidden());
        }

        @Test
        @WithMockUser(authorities = "ROLE_ADMIN")
        void createCandidate_NameTooLong_IsBadRequest() throws Exception {
                CandidateRequestDTO request = CandidateRequestDTO.builder()
                                .name("x".repeat(101))
                                .party("Test Party")
                                .electionId(electionId)
                                .build();

                mockMvc.perform(post("/api/v1/candidates")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                                .andExpect(status().isBadRequest())
                                .andExpect(jsonPath("$.data.name").exists());
        }

        @Test
        @WithMockUser(authorities = "ROLE_ADMIN")
        void bulkRegister_MoreThan500_IsBadRequest() throws Exception {
                List<CandidateRequestDTO> candidates = IntStream.range(0, 501)
                                .mapToObj(i -> CandidateRequestDTO.builder()
                                                .name("Candidate " + i)
                                                .electionId(electionId)
                                                .build())
                                .toList();

                mockMvc.perform(post("/api/v1/candidates/bulk")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(new BulkCandidateRequestDTO(candidates))))
                                .andExpect(status().isBadRequest());
        }
}
