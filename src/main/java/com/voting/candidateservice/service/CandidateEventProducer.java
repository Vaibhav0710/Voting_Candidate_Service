package com.voting.candidateservice.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class CandidateEventProducer {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    public void publishCandidateCreated(Object eventPayload) {
        log.info("Publishing candidate.created event: {}", eventPayload);
        kafkaTemplate.send("candidate.created", eventPayload);
    }

    public void publishCandidateStatusChanged(Object eventPayload) {
        log.info("Publishing candidate.status-changed event: {}", eventPayload);
        kafkaTemplate.send("candidate.status-changed", eventPayload);
    }

    public void publishCandidateDeleted(Object eventPayload) {
        log.info("Publishing candidate.deleted event: {}", eventPayload);
        kafkaTemplate.send("candidate.deleted", eventPayload);
    }
}
