package io.quarkus.kafka.streams.runtime.health;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.eclipse.microprofile.config.inject.ConfigProperty;
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

    @ConfigProperty(name = "quarkus.kafka-streams.topics")
    public List<String> topics;

    //    @ConfigProperty(name = "quarkus.kafka-streams.bootstrap-servers")
    //    public List<String> bootstrapServers;

    @Inject
    private KafkaStreamsTopologyManager manager;

    //    private String commaSeparatedBootstrapServersConfig;
    private List<String> trimmedTopics;

    @PostConstruct
    public void init() {
        //        commaSeparatedBootstrapServersConfig = String.join(",", bootstrapServers);
        trimmedTopics = topics.stream().map(String::trim).collect(Collectors.toList());
    }

    @Override
    public HealthCheckResponse call() {
        HealthCheckResponseBuilder builder = HealthCheckResponse.named("Kafka Streams topics health check").up();

        try {
            Set<String> missingTopics = manager.getMissingTopics(trimmedTopics);
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

        return builder.build();
    }
}
