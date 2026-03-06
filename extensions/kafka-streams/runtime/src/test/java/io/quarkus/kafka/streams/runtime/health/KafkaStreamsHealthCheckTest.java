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

import io.quarkus.kafka.streams.runtime.KafkaStreamsRuntimeConfig;

@ExtendWith(MockitoExtension.class)
public class KafkaStreamsHealthCheckTest {

    @InjectMocks
    KafkaStreamsStateHealthCheck healthCheck;

    @Mock
    private KafkaStreams streams;

    @Mock
    private KafkaStreamsRuntimeConfig runtimeConfig;

    @Test
    public void shouldBeUpIfStateRunning() {
        Mockito.when(runtimeConfig.healthRuntimeEnabled()).thenReturn(true);
        Mockito.when(streams.state()).thenReturn(KafkaStreams.State.RUNNING);
        HealthCheckResponse response = healthCheck.call().await().indefinitely();
        assertThat(response.getStatus()).isEqualTo(HealthCheckResponse.Status.UP);
    }

    @Test
    public void shouldBeUpIfStateRebalancing() {
        Mockito.when(runtimeConfig.healthRuntimeEnabled()).thenReturn(true);
        Mockito.when(streams.state()).thenReturn(KafkaStreams.State.REBALANCING);
        HealthCheckResponse response = healthCheck.call().await().indefinitely();
        assertThat(response.getStatus()).isEqualTo(HealthCheckResponse.Status.UP);
    }

    @Test
    public void shouldBeDownIfStateCreated() {
        Mockito.when(runtimeConfig.healthRuntimeEnabled()).thenReturn(true);
        Mockito.when(streams.state()).thenReturn(KafkaStreams.State.CREATED);
        HealthCheckResponse response = healthCheck.call().await().indefinitely();
        assertThat(response.getStatus()).isEqualTo(HealthCheckResponse.Status.DOWN);
    }

    @Test
    public void shouldBeDownIfKafkaStreamsIsNull() {
        KafkaStreamsStateHealthCheck check = new KafkaStreamsStateHealthCheck();
        check.runtimeConfig = Mockito.mock(KafkaStreamsRuntimeConfig.class);
        Mockito.when(check.runtimeConfig.healthRuntimeEnabled()).thenReturn(true);
        HealthCheckResponse response = check.call().await().indefinitely();
        assertThat(response.getStatus()).isEqualTo(HealthCheckResponse.Status.DOWN);
        assertThat(response.getData().get().get("technical_error"))
                .isEqualTo("KafkaStreams instance not available");
    }

    @Test
    public void shouldBeUpWhenRuntimeHealthDisabled() {
        Mockito.when(runtimeConfig.healthRuntimeEnabled()).thenReturn(false);
        HealthCheckResponse response = healthCheck.call().await().indefinitely();
        assertThat(response.getStatus()).isEqualTo(HealthCheckResponse.Status.UP);
    }

    @Test
    public void shouldBeDownIfStateNotReady() {
        Mockito.when(runtimeConfig.healthRuntimeEnabled()).thenReturn(true);
        Mockito.when(streams.state()).thenReturn(KafkaStreams.State.NOT_RUNNING);
        HealthCheckResponse response = healthCheck.call().await().indefinitely();
        assertThat(response.getStatus()).isEqualTo(HealthCheckResponse.Status.DOWN);
        assertThat(response.getData().get().get("state")).isEqualTo("NOT_RUNNING");
    }

    @Test
    public void shouldBeDownIfStateThrowsException() {
        Mockito.when(runtimeConfig.healthRuntimeEnabled()).thenReturn(true);
        Mockito.when(streams.state()).thenThrow(new RuntimeException("kafka error"));
        HealthCheckResponse response = healthCheck.call().await().indefinitely();
        assertThat(response.getStatus()).isEqualTo(HealthCheckResponse.Status.DOWN);
        assertThat(response.getData().get().get("technical_error")).isEqualTo("kafka error");
    }

    @Test
    public void shouldNotInteractWithKafkaStreamsWhenRuntimeDisabled() {
        Mockito.when(runtimeConfig.healthRuntimeEnabled()).thenReturn(false);
        healthCheck.call().await().indefinitely();
        Mockito.verify(streams, Mockito.never()).state();
    }
}
