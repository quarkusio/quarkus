package io.quarkus.kafka.streams.runtime.health;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Collections;

import org.eclipse.microprofile.health.HealthCheckResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

import io.quarkus.kafka.streams.runtime.KafkaStreamsTopologyManager;

public class KafkaStreamsTopicsHealthCheckTest {

    @InjectMocks
    KafkaStreamsTopicsHealthCheck healthCheck = new KafkaStreamsTopicsHealthCheck();

    @Mock
    private KafkaStreamsTopologyManager manager;

    @BeforeEach
    public void setUp() {
        MockitoAnnotations.initMocks(this);
    }

    @Test
    public void shouldBeUpIfNoMissingTopic() throws InterruptedException {
        Mockito.when(manager.getTopicsToCheck()).thenReturn(Collections.singleton("topic"));
        Mockito.when(manager.getMissingTopics()).thenReturn(Collections.emptySet());

        HealthCheckResponse response = healthCheck.call();

        assertThat(response.getState()).isEqualTo(HealthCheckResponse.State.UP);
        assertThat(response.getData().get()).doesNotContainKey("missing_topics");
        assertThat(response.getData().get()).containsKey("available_topics");
    }

    @Test
    public void shouldBeDownIfMissingTopic() throws InterruptedException {
        Mockito.when(manager.getMissingTopics()).thenReturn(Collections.singleton("topic"));

        HealthCheckResponse response = healthCheck.call();

        assertThat(response.getState()).isEqualTo(HealthCheckResponse.State.DOWN);
        assertThat(response.getData().get()).containsKey("missing_topics");
        assertThat(response.getData().get()).doesNotContainKey("available_topics");
    }

    @Test
    public void shouldBeUpIfNoTopicToCheck() throws InterruptedException {
        Mockito.when(manager.getTopicsToCheck()).thenReturn(Collections.emptySet());
        Mockito.when(manager.getMissingTopics()).thenReturn(Collections.emptySet());

        HealthCheckResponse response = healthCheck.call();

        assertThat(response.getState()).isEqualTo(HealthCheckResponse.State.UP);
        assertThat(response.getData()).isNotPresent();
    }
}
