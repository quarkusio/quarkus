package io.quarkus.vertx.runtime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertFalse;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.quarkus.vertx.core.runtime.VertxCoreRecorder;

public class VertxProducerTest {

    private VertxRecorder recorder;
    private VertxProducer producer;

    @BeforeEach
    public void setUp() {
        producer = new VertxProducer();
        recorder = new VertxRecorder();
    }

    @AfterEach
    public void tearDown() {
        recorder.destroy();
    }

    @Test
    public void shouldNotFailWithoutConfig() {
        producer.vertx = VertxCoreRecorder.initialize(null, null);
        verifyProducer();
    }

    private void verifyProducer() {
        assertThat(producer.eventbus()).isNotNull();

        assertThat(producer.axle()).isNotNull();
        assertFalse(producer.axle().isClustered());
        assertThat(producer.axleEventBus(producer.axle())).isNotNull();

        assertThat(producer.rx()).isNotNull();
        assertFalse(producer.rx().isClustered());
        assertThat(producer.rxEventBus(producer.rx())).isNotNull();

        assertThat(producer.mutiny()).isNotNull();
        assertFalse(producer.mutiny().isClustered());
        assertThat(producer.mutinyEventBus(producer.mutiny())).isNotNull();

    }
}
