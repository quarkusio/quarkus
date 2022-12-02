package io.quarkus.micrometer.runtime.binder.kafka;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import io.micrometer.core.instrument.binder.kafka.KafkaClientMetrics;
import io.quarkus.runtime.ShutdownEvent;

class KafkaEventObserverTest {

    @Test
    void testAllKafkaClientMetricsClosed() {
        KafkaEventObserver sut = new KafkaEventObserver();

        KafkaClientMetrics firstClientMetrics = Mockito.mock(KafkaClientMetrics.class);
        KafkaClientMetrics secondClientMetrics = Mockito.mock(KafkaClientMetrics.class);
        sut.clientMetrics.put(firstClientMetrics, firstClientMetrics);
        sut.clientMetrics.put(secondClientMetrics, secondClientMetrics);

        sut.onStop(new ShutdownEvent());

        Mockito.verify(firstClientMetrics).close();
        Mockito.verify(secondClientMetrics).close();
    }

}
