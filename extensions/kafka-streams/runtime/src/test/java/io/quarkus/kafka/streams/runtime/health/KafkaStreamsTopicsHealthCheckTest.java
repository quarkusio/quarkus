package io.quarkus.kafka.streams.runtime.health;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Collections;

import org.eclipse.microprofile.health.HealthCheckResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import io.quarkus.kafka.streams.runtime.KafkaStreamsTopologyManager;

@ExtendWith(MockitoExtension.class)
public class KafkaStreamsTopicsHealthCheckTest {

    @InjectMocks
    KafkaStreamsTopicsHealthCheck healthCheck;

    @Mock
    private KafkaStreamsTopologyManager manager;

    @Test
    public void shouldBeUpIfNoMissingTopic() throws InterruptedException {
        Mockito.when(manager.isTopicsCheckEnabled()).thenReturn(true);
        Mockito.when(manager.getMissingTopics()).thenReturn(Collections.emptySet());
        HealthCheckResponse response = healthCheck.call();
        assertThat(response.getStatus()).isEqualTo(HealthCheckResponse.Status.UP);
    }

    @Test
    public void shouldBeDownIfMissingTopic() throws InterruptedException {
        Mockito.when(manager.isTopicsCheckEnabled()).thenReturn(true);
        Mockito.when(manager.getMissingTopics()).thenReturn(Collections.singleton("topic"));
        HealthCheckResponse response = healthCheck.call();
        assertThat(response.getStatus()).isEqualTo(HealthCheckResponse.Status.DOWN);
    }
}
