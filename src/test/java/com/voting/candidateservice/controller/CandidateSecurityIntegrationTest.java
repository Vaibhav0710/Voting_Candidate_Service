package com.voting.candidateservice.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.voting.candidateservice.dto.CandidateRequestDTO;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public class CandidateSecurityIntegrationTest {

        @Autowired
        private MockMvc mockMvc;

        @Autowired
        private ObjectMapper objectMapper;

        private final UUID electionId = UUID.randomUUID();

        @Test
        void getCandidates_IsPublic() throws Exception {
                mockMvc.perform(get("/api/v1/candidates"))
                                .andExpect(status().isOk());
        }

        @Test
        void createCandidate_WithoutUser_IsForbidden() throws Exception {
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
        void deleteCandidate_WithoutUser_IsForbidden() throws Exception {
                mockMvc.perform(delete("/api/v1/candidates/{id}", UUID.randomUUID()))
                                .andExpect(status().isForbidden());
        }

        @Test
        @WithMockUser(authorities = "ROLE_ADMIN")
        void deleteCandidate_WithAdminRole_IsNotForbidden() throws Exception {
                // We expect 404 because the ID doesn't exist, but 404 means we PASSED security!
                mockMvc.perform(delete("/api/v1/candidates/{id}", UUID.randomUUID()))
                                .andExpect(status().isNotFound());
        }
}
