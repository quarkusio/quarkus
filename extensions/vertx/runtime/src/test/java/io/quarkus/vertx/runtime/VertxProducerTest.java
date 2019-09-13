package io.quarkus.vertx.runtime;

import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import io.quarkus.vertx.core.runtime.VertxCoreRecorder;

public class VertxProducerTest {

    private VertxRecorder recorder;
    private VertxProducer producer;

    @Before
    public void setUp() throws Exception {
        producer = new VertxProducer();
        recorder = new VertxRecorder();
    }

    @After
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
        assertThat(producer.eventbus(), is(notNullValue()));

        assertThat(producer.axle(), is(notNullValue()));
        assertFalse(producer.axle().isClustered());
        assertThat(producer.axleEventbus(), is(notNullValue()));

        assertThat(producer.rx(), is(notNullValue()));
        assertFalse(producer.rx().isClustered());
        assertThat(producer.rxRventbus(), is(notNullValue()));
    }
}
