package io.quarkus.kafka.streams.runtime.health;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import org.apache.kafka.streams.KafkaStreams;
import org.eclipse.microprofile.health.HealthCheckResponse;
import org.eclipse.microprofile.health.HealthCheckResponseBuilder;
import org.eclipse.microprofile.health.Liveness;

import io.quarkus.kafka.streams.runtime.KafkaStreamsRuntimeConfig;
import io.smallrye.health.api.AsyncHealthCheck;
import io.smallrye.mutiny.Uni;

@Liveness
@ApplicationScoped
public class KafkaStreamsStateHealthCheck implements AsyncHealthCheck {

    @Inject
    protected KafkaStreams kafkaStreams;

    @Inject
    KafkaStreamsRuntimeConfig runtimeConfig;

    @Override
    public Uni<HealthCheckResponse> call() {
        HealthCheckResponseBuilder responseBuilder = HealthCheckResponse.named("Kafka Streams state health check");
        if (!runtimeConfig.healthRuntimeEnabled()) {
            responseBuilder.up();
            return Uni.createFrom().item(responseBuilder.build());
        }
        if (kafkaStreams == null) {
            responseBuilder.down().withData("technical_error", "KafkaStreams instance not available");
            return Uni.createFrom().item(responseBuilder.build());
        }
        try {
            KafkaStreams.State state = kafkaStreams.state();
            responseBuilder.status(state.isRunningOrRebalancing())
                    .withData("state", state.name());
        } catch (Exception e) {
            responseBuilder.down().withData("technical_error", e.getMessage());
        }
        return Uni.createFrom().item(responseBuilder.build());
    }
}
