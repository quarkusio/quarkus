package io.quarkus.kafka.streams.runtime.health;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import org.eclipse.microprofile.health.HealthCheck;
import org.eclipse.microprofile.health.HealthCheckResponse;
import org.eclipse.microprofile.health.HealthCheckResponseBuilder;
import org.eclipse.microprofile.health.Readiness;
import org.jboss.logging.Logger;

import io.quarkus.kafka.streams.runtime.KafkaStreamsRuntimeConfig;
import io.quarkus.kafka.streams.runtime.KafkaStreamsTopologyManager;

@Readiness
@ApplicationScoped
public class KafkaStreamsTopicsHealthCheck implements HealthCheck {

    private static final Logger LOGGER = Logger.getLogger(KafkaStreamsTopicsHealthCheck.class.getName());

    @Inject
    KafkaStreamsRuntimeConfig kafkaStreamsRuntimeConfig;

    @Inject
    KafkaStreamsTopologyManager manager;

    private List<String> trimmedTopics;

    @PostConstruct
    public void init() {
        if (kafkaStreamsRuntimeConfig.topicsTimeout.compareTo(Duration.ZERO) > 0) {
            trimmedTopics = kafkaStreamsRuntimeConfig.topics
                    .orElseThrow(() -> new IllegalArgumentException("Missing list of topics"))
                    .stream()
                    .map(String::trim)
                    .collect(Collectors.toList());
        }
    }

    @Override
    public HealthCheckResponse call() {
        HealthCheckResponseBuilder builder = HealthCheckResponse.named("Kafka Streams topics health check").up();
        if (trimmedTopics != null) {
            try {
                Set<String> missingTopics = manager.getMissingTopics(trimmedTopics, kafkaStreamsRuntimeConfig.topicsTimeout);
                List<String> availableTopics = new ArrayList<>(trimmedTopics);
                availableTopics.removeAll(missingTopics);

                if (!availableTopics.isEmpty()) {
                    builder.withData("available_topics", String.join(",", availableTopics));
                }
                if (!missingTopics.isEmpty()) {
                    builder.down().withData("missing_topics", String.join(",", missingTopics));
                }
            } catch (InterruptedException e) {
                LOGGER.error("error when retrieving missing topics", e);
                builder.down().withData("technical_error", e.getMessage());
            }
        }
        return builder.build();
    }
}
