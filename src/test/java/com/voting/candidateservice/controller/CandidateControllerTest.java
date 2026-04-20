package com.voting.candidateservice.controller;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.voting.candidateservice.dto.*;
import com.voting.candidateservice.exception.DuplicateResourceException;
import com.voting.candidateservice.exception.GlobalExceptionHandler;
import com.voting.candidateservice.exception.ResourceNotFoundException;
import com.voting.candidateservice.model.enums.CandidateStatus;
import com.voting.candidateservice.service.CandidateService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.web.PageableHandlerMethodArgumentResolver;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class CandidateControllerTest {

        private final ObjectMapper objectMapper = new ObjectMapper();
        private final UUID candidateId = UUID.randomUUID();
        private final UUID electionId = UUID.randomUUID();
        private MockMvc mockMvc;
        @Mock
        private CandidateService candidateService;
        @InjectMocks
        private CandidateController candidateController;
        private CandidateRequestDTO requestDTO;
        private CandidateResponseDTO responseDTO;
        private AutoCloseable closeable;

        @BeforeEach
        void setUp() {
                closeable = MockitoAnnotations.openMocks(this);

                // Configure ObjectMapper
                objectMapper.registerModule(new JavaTimeModule());
                // Use MixIn to bypass problematic Pageable serialization
                objectMapper.addMixIn(Page.class, PageMixIn.class);

                MappingJackson2HttpMessageConverter converter = new MappingJackson2HttpMessageConverter();
                converter.setObjectMapper(objectMapper);

                mockMvc = MockMvcBuilders.standaloneSetup(candidateController)
                                .setCustomArgumentResolvers(new PageableHandlerMethodArgumentResolver())
                                .setMessageConverters(converter)
                                .setControllerAdvice(new GlobalExceptionHandler())
                                .build();

                requestDTO = CandidateRequestDTO.builder()
                                .name("Rahul Sharma")
                                .party("Swaraj Party")
                                .electionId(electionId)
                                .build();

                responseDTO = CandidateResponseDTO.builder()
                                .id(candidateId)
                                .name("Rahul Sharma")
                                .party("Swaraj Party")
                                .electionId(electionId)
                                .status(CandidateStatus.ACTIVE)
                                .build();
        }

        @AfterEach
        void tearDown() throws Exception {
                if (closeable != null) {
                        closeable.close();
                }
        }

        @Test
        void createCandidate_Success() throws Exception {
                when(candidateService.createCandidate(any())).thenReturn(responseDTO);

                mockMvc.perform(post("/api/v1/candidates")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(requestDTO)))
                                .andExpect(status().isCreated())
                                .andExpect(jsonPath("$.success").value(true))
                                .andExpect(jsonPath("$.data.name").value("Rahul Sharma"));
        }

        @Test
        void bulkRegister_Success() throws Exception {
                CandidateResponseDTO c1 = CandidateResponseDTO.builder().id(UUID.randomUUID()).name("Amit Verma")
                                .build();
                BulkCandidateRequestDTO bulkRequest = BulkCandidateRequestDTO.builder()
                                .candidates(List.of(requestDTO)).build();

                when(candidateService.bulkRegisterCandidates(any())).thenReturn(List.of(c1));

                mockMvc.perform(post("/api/v1/candidates/bulk")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(bulkRequest)))
                                .andExpect(status().isCreated())
                                .andExpect(jsonPath("$.data[0].name").value("Amit Verma"));
        }

        @Test
        void getCandidateById_Success() throws Exception {
                when(candidateService.getCandidateById(candidateId)).thenReturn(responseDTO);

                mockMvc.perform(get("/api/v1/candidates/{id}", candidateId))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.data.id").value(candidateId.toString()));
        }

        @Test
        void getAllCandidates_Success() throws Exception {
                Page<CandidateResponseDTO> page = new PageImpl<>(List.of(responseDTO));
                when(candidateService.getAllCandidates(any())).thenReturn(page);

                mockMvc.perform(get("/api/v1/candidates")
                                .param("page", "0")
                                .param("size", "10"))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.data.content[0].name").value("Rahul Sharma"));
        }

        @Test
        void getCandidatesByElection_Success() throws Exception {
                when(candidateService.getCandidatesByElection(electionId)).thenReturn(List.of(responseDTO));

                mockMvc.perform(get("/api/v1/candidates/election/{electionId}", electionId))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.data[0].name").value("Rahul Sharma"));
        }

        @Test
        void getActiveCandidatesByElection_Success() throws Exception {
                when(candidateService.getActiveCandidatesByElection(electionId)).thenReturn(List.of(responseDTO));

                mockMvc.perform(get("/api/v1/candidates/election/{electionId}/active", electionId))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.success").value(true));
        }

        @Test
        void updateCandidate_Success() throws Exception {
                CandidateUpdateDTO updateDTO = new CandidateUpdateDTO("Priya Patel", "National Front");
                when(candidateService.updateCandidate(eq(candidateId), any())).thenReturn(responseDTO);

                mockMvc.perform(put("/api/v1/candidates/{id}", candidateId)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(updateDTO)))
                                .andExpect(status().isOk());
        }

        @Test
        void updateStatus_Success() throws Exception {
                CandidateStatusUpdateDTO statusDTO = new CandidateStatusUpdateDTO(CandidateStatus.WITHDRAWN);
                responseDTO.setStatus(CandidateStatus.WITHDRAWN);
                when(candidateService.updateCandidateStatus(eq(candidateId), any())).thenReturn(responseDTO);

                mockMvc.perform(patch("/api/v1/candidates/{id}/status", candidateId)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(statusDTO)))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.data.status").value("WITHDRAWN"));
        }

        @Test
        void deleteCandidate_Success() throws Exception {
                mockMvc.perform(delete("/api/v1/candidates/{id}", candidateId))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.message").value("Candidate deleted successfully"));
        }

        @Test
        void candidateExists_Success() throws Exception {
                when(candidateService.candidateExists(candidateId)).thenReturn(true);

                mockMvc.perform(get("/api/v1/candidates/{id}/exists", candidateId))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.data").value(true));
        }

        @Test
        void validateCandidate_Success() throws Exception {
                CandidateValidationDTO validation = CandidateValidationDTO.builder()
                                .isValid(true).message("Valid").build();
                when(candidateService.validateCandidateForElection(candidateId, electionId)).thenReturn(validation);

                mockMvc.perform(get("/api/v1/candidates/{id}/validate", candidateId)
                                .param("electionId", electionId.toString()))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.data.valid").value(true));
        }

        @Test
        void createCandidate_ValidationFailed_Returns400_WhenNameBlank() throws Exception {
                CandidateRequestDTO badRequest = CandidateRequestDTO.builder()
                                .name("") // Blank name
                                .party("Some Party")
                                .electionId(electionId)
                                .build();

                mockMvc.perform(post("/api/v1/candidates")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(badRequest)))
                                .andExpect(status().isBadRequest())
                                .andExpect(jsonPath("$.success").value(false))
                                .andExpect(jsonPath("$.message").value("Validation failed"))
                                .andExpect(jsonPath("$.data.name").exists());
        }

        @Test
        void createCandidate_ValidationFailed_Returns400_WhenElectionIdNull() throws Exception {
                CandidateRequestDTO badRequest = CandidateRequestDTO.builder()
                                .name("Valid Name")
                                .party("Some Party")
                                .electionId(null) // Null electionId
                                .build();

                mockMvc.perform(post("/api/v1/candidates")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(badRequest)))
                                .andExpect(status().isBadRequest())
                                .andExpect(jsonPath("$.success").value(false))
                                .andExpect(jsonPath("$.message").value("Validation failed"))
                                .andExpect(jsonPath("$.data.electionId").exists());
        }

        @Test
        void getCandidateById_Returns404_WhenCandidateNotFound() throws Exception {
                when(candidateService.getCandidateById(candidateId))
                                .thenThrow(new ResourceNotFoundException("Candidate not found"));

                mockMvc.perform(get("/api/v1/candidates/{id}", candidateId))
                                .andExpect(status().isNotFound())
                                .andExpect(jsonPath("$.success").value(false))
                                .andExpect(jsonPath("$.message").value("Candidate not found"));
        }

        @Test
        void createCandidate_Returns409_WhenDuplicateCandidate() throws Exception {
                when(candidateService.createCandidate(any()))
                                .thenThrow(new DuplicateResourceException("Candidate already exists in this election"));

                mockMvc.perform(post("/api/v1/candidates")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(requestDTO)))
                                .andExpect(status().isConflict())
                                .andExpect(jsonPath("$.success").value(false))
                                .andExpect(jsonPath("$.message").value("Candidate already exists in this election"));
        }

        /**
         * MixIn to ignore 'pageable' property during JSON serialization of Page objects
         * in tests.
         * This prevents UnsupportedOperationException in standalone MockMvc tests.
         */
        @JsonIgnoreProperties("pageable")
        private interface PageMixIn {
        }
}