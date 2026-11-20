package com.chubb.claims;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.kafka.clients.admin.NewTopic;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Configuration(proxyBeanMethods = false)
@EnableAsync
@Profile("kafka")
class DecisionEvents {
    private static final Logger LOG = LoggerFactory.getLogger(DecisionEvents.class);
    private static final String TOPIC = "claims.decision-recorded.v1";

    private final KafkaTemplate<String, String> kafka;
    private final ObjectMapper json;

    DecisionEvents(KafkaTemplate<String, String> kafka, ObjectMapper json) {
        this.kafka = kafka;
        this.json = json;
    }

    @Bean
    NewTopic decisionTopic() {
        return TopicBuilder.name(TOPIC).partitions(1).replicas(1).build();
    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void publish(ClaimDecisionRecorded event) {
        try {
            kafka.send(TOPIC, event.claimId().toString(), json.writeValueAsString(event))
                    .whenComplete((sent, failure) -> {
                        if (failure != null) {
                            LOG.warn("Could not publish decision for claim {}", event.claimId(), failure);
                        }
                    });
        } catch (JsonProcessingException | RuntimeException error) {
            LOG.warn("Could not publish decision for claim {}", event.claimId(), error);
        }
    }

    @KafkaListener(topics = TOPIC)
    public void consume(String payload) throws JsonProcessingException {
        var event = json.readValue(payload, ClaimDecisionRecorded.class);
        LOG.info("Consumed ClaimDecisionRecorded claimId={} status={} decidedAt={}",
                event.claimId(), event.status(), event.decidedAt());
    }
}
