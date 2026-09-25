package io.quarkus.kafka.streams.runtime.health;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;

import org.eclipse.microprofile.health.HealthCheckResponse;
import org.junit.jupiter.api.Test;

/**
 * When the application has no {@code Topology} producer, {@code KafkaStreamsProducer} produces {@code null} beans and
 * the health checks must not report the application as down.
 */
public class KafkaStreamsHealthChecksWithoutTopologyTest {

    @Test
    public void stateCheckIsUpWithoutKafkaStreams() {
        KafkaStreamsStateHealthCheck healthCheck = new KafkaStreamsStateHealthCheck();
        HealthCheckResponse response = healthCheck.call();
        assertThat(response.getStatus()).isEqualTo(HealthCheckResponse.Status.UP);
        assertThat(response.getData()).contains(Map.of("state", "NOT_STARTED"));
    }

    @Test
    public void topicsCheckIsUpWithoutTopologyManager() {
        KafkaStreamsTopicsHealthCheck healthCheck = new KafkaStreamsTopicsHealthCheck(null);
        HealthCheckResponse response = healthCheck.call();
        assertThat(response.getStatus()).isEqualTo(HealthCheckResponse.Status.UP);
    }
}
