package io.quarkus.kafka.streams.runtime.health;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import org.eclipse.microprofile.health.HealthCheck;
import org.eclipse.microprofile.health.HealthCheckResponse;
import org.eclipse.microprofile.health.HealthCheckResponseBuilder;
import org.eclipse.microprofile.health.Readiness;
import org.jboss.logging.Logger;

import io.quarkus.kafka.streams.runtime.KafkaStreamsTopologyManager;

@Readiness
@ApplicationScoped
public class KafkaStreamsTopicsHealthCheck implements HealthCheck {

    private static final Logger LOGGER = Logger.getLogger(KafkaStreamsTopicsHealthCheck.class.getName());

    @Inject
    KafkaStreamsTopologyManager manager;

    private final List<String> checkedTopics;

    @Inject
    public KafkaStreamsTopicsHealthCheck(KafkaStreamsTopologyManager manager) {
        this.manager = manager;
        this.checkedTopics = new ArrayList<>();
        if (manager.isTopicsCheckEnabled()) {
            checkedTopics.addAll(manager.getSourceTopics());
            checkedTopics.addAll(manager.getSourcePatterns().stream().map(Pattern::pattern).toList());
        }
    }

    @Override
    public HealthCheckResponse call() {
        HealthCheckResponseBuilder builder = HealthCheckResponse.named("Kafka Streams topics health check").up();
        if (manager.isTopicsCheckEnabled()) {
            try {
                Set<String> missingTopics = manager.getMissingTopics();
                List<String> availableTopics = new ArrayList<>(checkedTopics);
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
