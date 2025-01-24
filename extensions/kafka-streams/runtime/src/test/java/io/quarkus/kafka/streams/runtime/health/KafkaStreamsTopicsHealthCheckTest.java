package io.quarkus.kafka.streams.runtime.health;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;

import java.time.Duration;
import java.util.Collections;
import java.util.Optional;

import org.eclipse.microprofile.health.HealthCheckResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

import io.quarkus.kafka.streams.runtime.KafkaStreamsRuntimeConfig;
import io.quarkus.kafka.streams.runtime.KafkaStreamsTopologyManager;

public class KafkaStreamsTopicsHealthCheckTest {

    @InjectMocks
    KafkaStreamsTopicsHealthCheck healthCheck = new KafkaStreamsTopicsHealthCheck();

    @Mock
    private KafkaStreamsTopologyManager manager;

    @BeforeEach
    public void setUp() {
        MockitoAnnotations.initMocks(this);

        KafkaStreamsRuntimeConfig configMock = Mockito.mock(KafkaStreamsRuntimeConfig.class);
        Mockito.doReturn(Optional.of(Collections.singletonList("topic")))
                .when(configMock).topics();

        Mockito.doReturn(Duration.ofSeconds(10))
                .when(configMock).topicsTimeout();
        healthCheck.kafkaStreamsRuntimeConfig = configMock;

        healthCheck.init();
    }

    @Test
    public void shouldBeUpIfNoMissingTopic() throws InterruptedException {
        Mockito.when(manager.getMissingTopics(anyList(), any())).thenReturn(Collections.emptySet());
        HealthCheckResponse response = healthCheck.call();
        assertThat(response.getStatus()).isEqualTo(HealthCheckResponse.Status.UP);
    }

    @Test
    public void shouldBeDownIfMissingTopic() throws InterruptedException {
        Mockito.when(manager.getMissingTopics(anyList(), any())).thenReturn(Collections.singleton("topic"));
        HealthCheckResponse response = healthCheck.call();
        assertThat(response.getStatus()).isEqualTo(HealthCheckResponse.Status.DOWN);
    }
}
