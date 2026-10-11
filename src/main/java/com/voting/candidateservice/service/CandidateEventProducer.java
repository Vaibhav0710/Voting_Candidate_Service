package com.voting.candidateservice.service;

import com.voting.candidateservice.dto.CandidateResponseDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.KafkaException;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/**
 * Publishes candidate lifecycle events ({@code candidate.created}, {@code candidate.status-changed},
 * {@code candidate.deleted}), keyed by candidate id so one candidate's events stay in order.
 * <p>
 * Inside a transaction the event is sent only after commit, so consumers (the Voting Service's
 * status cache) never see a change that was rolled back. A broker failure is logged, not thrown:
 * the change is already saved.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class CandidateEventProducer {

    static final String CREATED_TOPIC = "candidate.created";
    static final String STATUS_CHANGED_TOPIC = "candidate.status-changed";
    static final String DELETED_TOPIC = "candidate.deleted";

    private final KafkaTemplate<String, Object> kafkaTemplate;

    public void publishCandidateCreated(CandidateResponseDTO candidate) {
        publishAfterCommit(CREATED_TOPIC, candidate);
    }

    public void publishCandidateStatusChanged(CandidateResponseDTO candidate) {
        publishAfterCommit(STATUS_CHANGED_TOPIC, candidate);
    }

    public void publishCandidateDeleted(CandidateResponseDTO candidate) {
        publishAfterCommit(DELETED_TOPIC, candidate);
    }

    private void publishAfterCommit(String topic, CandidateResponseDTO candidate) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            send(topic, candidate);
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                send(topic, candidate);
            }
        });
    }

    private void send(String topic, CandidateResponseDTO candidate) {
        String candidateId = candidate.getId().toString();
        try {
            kafkaTemplate.send(topic, candidateId, candidate).whenComplete((result, error) -> {
                if (error != null) {
                    log.error("Failed to publish {} for candidate {}: {}", topic, candidateId, error.getMessage());
                }
            });
            log.info("Published {} for candidate {}", topic, candidateId);
        } catch (KafkaException e) {
            log.error("Failed to publish {} for candidate {}: {}", topic, candidateId, e.getMessage());
        }
    }
}
