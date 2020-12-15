package io.quarkus.kafka.streams.runtime.health;

import static org.assertj.core.api.Assertions.assertThat;

import org.apache.kafka.streams.KafkaStreams;
import org.eclipse.microprofile.health.HealthCheckResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

import io.quarkus.kafka.streams.runtime.KafkaStreamsTopologyManager;

public class KafkaStreamsHealthCheckTest {

    @InjectMocks
    KafkaStreamsStateHealthCheck healthCheck = new KafkaStreamsStateHealthCheck();

    @Mock
    private KafkaStreamsTopologyManager manager;

    @Mock
    private KafkaStreams streams;

    @BeforeEach
    public void setUp() {
        MockitoAnnotations.initMocks(this);
    }

    @Test
    public void shouldBeUpIfStateRunning() {
        Mockito.when(streams.state()).thenReturn(KafkaStreams.State.RUNNING);
        HealthCheckResponse response = healthCheck.call();
        assertThat(response.getStatus()).isEqualTo(HealthCheckResponse.Status.UP);
    }

    @Test
    public void shouldBeUpIfStateRebalancing() {
        Mockito.when(streams.state()).thenReturn(KafkaStreams.State.REBALANCING);
        HealthCheckResponse response = healthCheck.call();
        assertThat(response.getStatus()).isEqualTo(HealthCheckResponse.Status.UP);
    }

    @Test
    public void shouldBeDownIfStateCreated() {
        Mockito.when(streams.state()).thenReturn(KafkaStreams.State.CREATED);
        HealthCheckResponse response = healthCheck.call();
        assertThat(response.getStatus()).isEqualTo(HealthCheckResponse.Status.DOWN);
    }

}
