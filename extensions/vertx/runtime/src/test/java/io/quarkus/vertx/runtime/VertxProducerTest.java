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
    public void setUp() throws Exception {
        producer = new VertxProducer();
        recorder = new VertxRecorder();
    }

    @AfterEach
    public void tearDown() throws Exception {
        recorder.destroy();
    }

    @Test
    public void shouldNotFailWithoutConfig() {
        producer.vertx = VertxCoreRecorder.initialize(null);
        producer.initialize();
        verifyProducer();
    }

    private void verifyProducer() {
        assertThat(producer.eventbus()).isNotNull();

        assertThat(producer.axle()).isNotNull();
        assertFalse(producer.axle().isClustered());
        assertThat(producer.axleEventbus()).isNotNull();

        assertThat(producer.rx()).isNotNull();
        assertFalse(producer.rx().isClustered());
        assertThat(producer.rxRventbus()).isNotNull();
    }
}
