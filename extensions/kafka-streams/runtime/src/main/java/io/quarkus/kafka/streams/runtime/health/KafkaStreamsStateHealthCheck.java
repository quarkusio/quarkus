package io.quarkus.kafka.streams.runtime.health;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import org.apache.kafka.streams.KafkaStreams;
import org.eclipse.microprofile.health.HealthCheck;
import org.eclipse.microprofile.health.HealthCheckResponse;
import org.eclipse.microprofile.health.HealthCheckResponseBuilder;
import org.eclipse.microprofile.health.Liveness;

@Liveness
@ApplicationScoped
public class KafkaStreamsStateHealthCheck implements HealthCheck {

    @Inject
    protected KafkaStreams kafkaStreams;

    @Override
    public HealthCheckResponse call() {
        HealthCheckResponseBuilder responseBuilder = HealthCheckResponse.named("Kafka Streams state health check");
        try {
            KafkaStreams.State state = kafkaStreams.state();
            responseBuilder.status(state.isRunningOrRebalancing())
                    .withData("state", state.name());
        } catch (Exception e) {
            responseBuilder.down().withData("technical_error", e.getMessage());
        }
        return responseBuilder.build();
    }
}
