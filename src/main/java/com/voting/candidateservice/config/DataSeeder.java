package com.voting.candidateservice.config;

import com.voting.candidateservice.model.Candidate;
import com.voting.candidateservice.model.enums.CandidateStatus;
import com.voting.candidateservice.repository.CandidateRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;
import java.util.UUID;

@Configuration
public class DataSeeder {

    private static final Logger log = LoggerFactory.getLogger(DataSeeder.class);

    @Bean
    CommandLineRunner initDatabase(CandidateRepository repository) {
        return args -> {
            // Only seed if the database is completely empty
            if (repository.count() == 0) {
                log.info("Seeding database with sample candidates for manual testing...");

                // Simulate 2 different elections
                UUID presidentialElectionId = UUID.fromString("11111111-1111-1111-1111-111111111111");
                UUID localElectionId = UUID.fromString("22222222-2222-2222-2222-222222222222");

                List<Candidate> sampleCandidates = List.of(
                        createCandidate("Rahul Sharma", "Swaraj Party", presidentialElectionId, CandidateStatus.ACTIVE),
                        createCandidate("Priya Patel", "National Front", presidentialElectionId, CandidateStatus.ACTIVE),
                        createCandidate("Amit Verma", "Independent", presidentialElectionId, CandidateStatus.WITHDRAWN),
                        createCandidate("Neha Gupta", "Democratic Alliance", presidentialElectionId, CandidateStatus.ACTIVE),
                        createCandidate("Vikram Singh", "Bharat Vikas Mandal", presidentialElectionId, CandidateStatus.DISQUALIFIED),
                        createCandidate("John Doe", "Green Earth", localElectionId, CandidateStatus.ACTIVE),
                        createCandidate("Alice Smith", "Liberty Party", localElectionId, CandidateStatus.ACTIVE),
                        createCandidate("Bob Johnson", "Independent", localElectionId, CandidateStatus.ACTIVE),
                        createCandidate("Maria Garcia", "Tech Forward", localElectionId, CandidateStatus.ACTIVE),
                        createCandidate("Chen Wei", "United Citizens", localElectionId, CandidateStatus.WITHDRAWN)
                );

                repository.saveAll(sampleCandidates);
                log.info("Successfully seeded {} candidate records.", sampleCandidates.size());
            } else {
                log.info("Database already contains records. Skipping seed process.");
            }
        };
    }

    private Candidate createCandidate(String name, String party, UUID electionId, CandidateStatus status) {
        Candidate candidate = new Candidate();
        candidate.setName(name);
        candidate.setParty(party);
        candidate.setElectionId(electionId);
        candidate.setStatus(status);
        candidate.setExternalId(UUID.randomUUID());
        candidate.setDeleted(false);
        return candidate;
    }
}
