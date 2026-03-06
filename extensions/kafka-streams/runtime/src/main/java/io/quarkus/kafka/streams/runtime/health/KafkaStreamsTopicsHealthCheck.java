package io.quarkus.kafka.streams.runtime.health;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import org.eclipse.microprofile.health.HealthCheckResponse;
import org.eclipse.microprofile.health.HealthCheckResponseBuilder;
import org.eclipse.microprofile.health.Readiness;
import org.jboss.logging.Logger;

import io.quarkus.kafka.streams.runtime.KafkaStreamsRuntimeConfig;
import io.quarkus.kafka.streams.runtime.KafkaStreamsTopologyManager;
import io.smallrye.health.api.AsyncHealthCheck;
import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.infrastructure.Infrastructure;

@Readiness
@ApplicationScoped
public class KafkaStreamsTopicsHealthCheck implements AsyncHealthCheck {

    private static final Logger LOGGER = Logger.getLogger(KafkaStreamsTopicsHealthCheck.class.getName());

    private final KafkaStreamsTopologyManager manager;
    private final KafkaStreamsRuntimeConfig runtimeConfig;

    private final List<String> checkedTopics;

    @Inject
    public KafkaStreamsTopicsHealthCheck(KafkaStreamsTopologyManager manager, KafkaStreamsRuntimeConfig runtimeConfig) {
        this.manager = manager;
        this.runtimeConfig = runtimeConfig;
        this.checkedTopics = new ArrayList<>();
        if (manager != null && manager.isTopicsCheckEnabled()) {
            checkedTopics.addAll(manager.getSourceTopics());
            checkedTopics.addAll(manager.getSourcePatterns().stream().map(Pattern::pattern).toList());
        }
    }

    @Override
    public Uni<HealthCheckResponse> call() {
        HealthCheckResponseBuilder builder = HealthCheckResponse.named("Kafka Streams topics health check").up();
        if (!runtimeConfig.healthRuntimeEnabled()) {
            return Uni.createFrom().item(builder.build());
        }
        if (manager == null) {
            builder.down().withData("technical_error", "KafkaStreamsTopologyManager not available");
            return Uni.createFrom().item(builder.build());
        }
        if (!manager.isTopicsCheckEnabled()) {
            return Uni.createFrom().item(builder.build());
        }
        // Run the blocking admin client call on a worker thread to avoid
        // blocking the event loop and causing health probe timeouts (GH-42882)
        return Uni.createFrom().item(() -> {
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
            return builder.build();
        }).runSubscriptionOn(Infrastructure.getDefaultWorkerPool());
    }
}
