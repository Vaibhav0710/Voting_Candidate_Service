package com.voting.candidateservice.service;

import com.voting.candidateservice.dto.CandidateResponseDTO;
import com.voting.candidateservice.model.enums.CandidateStatus;
import org.apache.kafka.common.errors.TimeoutException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.KafkaException;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CandidateEventProducerTest {

    @Mock private KafkaTemplate<String, Object> kafkaTemplate;
    @InjectMocks private CandidateEventProducer producer;

    private final CandidateResponseDTO candidate = CandidateResponseDTO.builder()
            .id(UUID.randomUUID())
            .electionId(UUID.randomUUID())
            .status(CandidateStatus.DISQUALIFIED)
            .build();

    @AfterEach
    void clearSynchronization() {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.clearSynchronization();
        }
    }

    @Test
    void outsideTransaction_sendsImmediatelyKeyedByCandidateId() {
        when(kafkaTemplate.send(anyString(), anyString(), any())).thenReturn(new CompletableFuture<>());

        producer.publishCandidateStatusChanged(candidate);

        verify(kafkaTemplate).send("candidate.status-changed", candidate.getId().toString(), candidate);
    }

    @Test
    void insideTransaction_sendsOnlyAfterCommit() {
        when(kafkaTemplate.send(anyString(), anyString(), any())).thenReturn(new CompletableFuture<>());
        TransactionSynchronizationManager.initSynchronization();

        producer.publishCandidateCreated(candidate);
        verify(kafkaTemplate, never()).send(anyString(), anyString(), any());

        TransactionSynchronizationManager.getSynchronizations().forEach(TransactionSynchronization::afterCommit);
        verify(kafkaTemplate).send("candidate.created", candidate.getId().toString(), candidate);
    }

    @Test
    void rolledBackTransaction_sendsNothing() {
        TransactionSynchronizationManager.initSynchronization();

        producer.publishCandidateDeleted(candidate);
        TransactionSynchronizationManager.getSynchronizations()
                .forEach(sync -> sync.afterCompletion(TransactionSynchronization.STATUS_ROLLED_BACK));

        verify(kafkaTemplate, never()).send(anyString(), anyString(), any());
    }

    @Test
    void brokerUnavailable_doesNotFailTheCaller() {
        when(kafkaTemplate.send(anyString(), anyString(), any()))
                .thenThrow(new KafkaException("Send failed", new TimeoutException("metadata")));

        assertThatCode(() -> producer.publishCandidateCreated(candidate)).doesNotThrowAnyException();
    }

    @Test
    void asyncSendFailure_doesNotFailTheCaller() {
        when(kafkaTemplate.send(anyString(), anyString(), any()))
                .thenReturn(CompletableFuture.failedFuture(new TimeoutException("broker down")));

        assertThatCode(() -> producer.publishCandidateCreated(candidate)).doesNotThrowAnyException();
    }
}
