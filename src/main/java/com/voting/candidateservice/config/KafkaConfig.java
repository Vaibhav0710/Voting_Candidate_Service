package com.voting.candidateservice.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
public class KafkaConfig {

    @Bean
    public NewTopic candidateCreatedTopic() {
        return TopicBuilder.name("candidate.created")
                .partitions(1)
                .replicas(1)
                .build();
    }

    @Bean
    public NewTopic candidateStatusChangedTopic() {
        return TopicBuilder.name("candidate.status-changed")
                .partitions(1)
                .replicas(1)
                .build();
    }

    @Bean
    public NewTopic candidateDeletedTopic() {
        return TopicBuilder.name("candidate.deleted")
                .partitions(1)
                .replicas(1)
                .build();
    }
}
