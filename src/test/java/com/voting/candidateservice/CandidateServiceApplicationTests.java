package com.voting.candidateservice;

import com.voting.candidateservice.service.CandidateEventProducer;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
class CandidateServiceApplicationTests {

    @MockBean
    private CandidateEventProducer candidateEventProducer;

    @Test
    void contextLoads() {
    }

}
