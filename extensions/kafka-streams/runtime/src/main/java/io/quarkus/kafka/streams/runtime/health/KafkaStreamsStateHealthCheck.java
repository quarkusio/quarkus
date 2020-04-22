package io.quarkus.kafka.streams.runtime.health;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.apache.kafka.streams.KafkaStreams;
import org.eclipse.microprofile.health.HealthCheck;
import org.eclipse.microprofile.health.HealthCheckResponse;
import org.eclipse.microprofile.health.HealthCheckResponseBuilder;
import org.eclipse.microprofile.health.Liveness;

import io.quarkus.kafka.streams.runtime.KafkaStreamsTopologyManager;

@Liveness
@ApplicationScoped
public class KafkaStreamsStateHealthCheck implements HealthCheck {

    @Inject
    private KafkaStreamsTopologyManager manager;

    @Override
    public HealthCheckResponse call() {
        HealthCheckResponseBuilder responseBuilder = HealthCheckResponse.named("Kafka Streams state health check");
        try {
            KafkaStreams.State state = manager.getStreams().state();
            responseBuilder.state(state == KafkaStreams.State.RUNNING)
                    .withData("state", state.name());
        } catch (Exception e) {
            responseBuilder.down().withData("technical_error", e.getMessage());
        }
        return responseBuilder.build();
    }
}
