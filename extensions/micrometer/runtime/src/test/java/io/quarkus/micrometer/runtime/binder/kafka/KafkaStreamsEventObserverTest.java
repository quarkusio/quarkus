package io.quarkus.micrometer.runtime.binder.kafka;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import io.micrometer.core.instrument.binder.kafka.KafkaStreamsMetrics;
import io.quarkus.runtime.ShutdownEvent;

class KafkaStreamsEventObserverTest {

    @Test
    void testKafkaStreamsMetricsClosedAfterShutdownEvent() {
        KafkaStreamsEventObserver sut = new KafkaStreamsEventObserver();
        sut.kafkaStreamsMetrics = Mockito.mock(KafkaStreamsMetrics.class);

        sut.onStop(new ShutdownEvent());

        Mockito.verify(sut.kafkaStreamsMetrics).close();
    }

}