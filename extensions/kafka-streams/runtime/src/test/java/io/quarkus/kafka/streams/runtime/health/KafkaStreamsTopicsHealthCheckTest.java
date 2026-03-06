package io.quarkus.kafka.streams.runtime.health;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Collections;
import java.util.Set;

import org.eclipse.microprofile.health.HealthCheckResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import io.quarkus.kafka.streams.runtime.KafkaStreamsRuntimeConfig;
import io.quarkus.kafka.streams.runtime.KafkaStreamsTopologyManager;

@ExtendWith(MockitoExtension.class)
public class KafkaStreamsTopicsHealthCheckTest {

    @InjectMocks
    KafkaStreamsTopicsHealthCheck healthCheck;

    @Mock
    private KafkaStreamsTopologyManager manager;

    @Mock
    private KafkaStreamsRuntimeConfig runtimeConfig;

    @Test
    public void shouldBeUpIfNoMissingTopic() throws InterruptedException {
        Mockito.when(runtimeConfig.healthRuntimeEnabled()).thenReturn(true);
        Mockito.when(manager.isTopicsCheckEnabled()).thenReturn(true);
        Mockito.when(manager.getMissingTopics()).thenReturn(Collections.emptySet());
        HealthCheckResponse response = healthCheck.call().await().indefinitely();
        assertThat(response.getStatus()).isEqualTo(HealthCheckResponse.Status.UP);
    }

    @Test
    public void shouldBeDownIfMissingTopic() throws InterruptedException {
        Mockito.when(runtimeConfig.healthRuntimeEnabled()).thenReturn(true);
        Mockito.when(manager.isTopicsCheckEnabled()).thenReturn(true);
        Mockito.when(manager.getMissingTopics()).thenReturn(Collections.singleton("topic"));
        HealthCheckResponse response = healthCheck.call().await().indefinitely();
        assertThat(response.getStatus()).isEqualTo(HealthCheckResponse.Status.DOWN);
    }

    @Test
    public void shouldBeDownIfManagerIsNull() {
        KafkaStreamsRuntimeConfig config = Mockito.mock(KafkaStreamsRuntimeConfig.class);
        Mockito.when(config.healthRuntimeEnabled()).thenReturn(true);
        KafkaStreamsTopicsHealthCheck check = new KafkaStreamsTopicsHealthCheck(null, config);
        HealthCheckResponse response = check.call().await().indefinitely();
        assertThat(response.getStatus()).isEqualTo(HealthCheckResponse.Status.DOWN);
        assertThat(response.getData().get().get("technical_error"))
                .isEqualTo("KafkaStreamsTopologyManager not available");
    }

    @Test
    public void shouldBeUpWhenRuntimeHealthDisabled() {
        Mockito.when(runtimeConfig.healthRuntimeEnabled()).thenReturn(false);
        HealthCheckResponse response = healthCheck.call().await().indefinitely();
        assertThat(response.getStatus()).isEqualTo(HealthCheckResponse.Status.UP);
    }

    @Test
    public void shouldBeUpWhenTopicsCheckIsDisabled() {
        Mockito.when(runtimeConfig.healthRuntimeEnabled()).thenReturn(true);
        Mockito.when(manager.isTopicsCheckEnabled()).thenReturn(false);
        HealthCheckResponse response = healthCheck.call().await().indefinitely();
        assertThat(response.getStatus()).isEqualTo(HealthCheckResponse.Status.UP);
    }

    @Test
    public void shouldReportAvailableAndMissingTopics() throws InterruptedException {
        Mockito.when(runtimeConfig.healthRuntimeEnabled()).thenReturn(true);
        Mockito.when(manager.isTopicsCheckEnabled()).thenReturn(true);
        Mockito.when(manager.getMissingTopics()).thenReturn(Set.of("missing-topic"));
        HealthCheckResponse response = healthCheck.call().await().indefinitely();
        assertThat(response.getStatus()).isEqualTo(HealthCheckResponse.Status.DOWN);
        assertThat(response.getData().get().get("missing_topics")).isEqualTo("missing-topic");
    }

    @Test
    public void shouldHandleInterruptedException() throws InterruptedException {
        Mockito.when(runtimeConfig.healthRuntimeEnabled()).thenReturn(true);
        Mockito.when(manager.isTopicsCheckEnabled()).thenReturn(true);
        Mockito.when(manager.getMissingTopics()).thenThrow(new InterruptedException("test interrupt"));
        HealthCheckResponse response = healthCheck.call().await().indefinitely();
        assertThat(response.getStatus()).isEqualTo(HealthCheckResponse.Status.DOWN);
        assertThat(response.getData().get().get("technical_error")).isEqualTo("test interrupt");
    }
}
