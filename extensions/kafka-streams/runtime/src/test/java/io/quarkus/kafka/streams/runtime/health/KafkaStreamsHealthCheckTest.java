package io.quarkus.kafka.streams.runtime.health;

import static org.assertj.core.api.Assertions.assertThat;

import org.apache.kafka.streams.KafkaStreams;
import org.eclipse.microprofile.health.HealthCheckResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class KafkaStreamsHealthCheckTest {

    @InjectMocks
    KafkaStreamsStateHealthCheck healthCheck;

    @Mock
    private KafkaStreams streams;

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

    @Test
    public void shouldBeDownIfKafkaStreamsIsNull() {
        KafkaStreamsStateHealthCheck check = new KafkaStreamsStateHealthCheck();
        HealthCheckResponse response = check.call();
        assertThat(response.getStatus()).isEqualTo(HealthCheckResponse.Status.DOWN);
        assertThat(response.getData().get().get("technical_error"))
                .isEqualTo("KafkaStreams instance not available");
    }

}
