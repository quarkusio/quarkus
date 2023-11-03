package io.quarkus.micrometer.runtime.binder.kafka;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;

import org.apache.kafka.streams.KafkaStreams;
import org.jboss.logging.Logger;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Metrics;
import io.micrometer.core.instrument.binder.kafka.KafkaStreamsMetrics;
import io.quarkus.runtime.ShutdownEvent;

/**
 * Observer to create and register KafkaStreamsMetrics.
 *
 * Must be separated from KafkaEventObserver, because they use different dependencies and if only "kafka-client" is used, the
 * classes from "kafka-streams" aren't loaded.
 */
@ApplicationScoped
public class KafkaStreamsEventObserver {
    private static final Logger log = Logger.getLogger(KafkaStreamsEventObserver.class);

    final MeterRegistry registry = Metrics.globalRegistry;
    KafkaStreamsMetrics kafkaStreamsMetrics;

    /**
     * Manage bind/close of KafkaStreamsMetrics for the specified KafkaStreams client.
     * If the kafkaStreams has not been seen before, it will be bound to the
     * Micrometer registry and instrumented using a Kafka MeterBinder.
     * If the kafkaStreams has been seen before, the MeterBinder will be closed.
     *
     * @param kafkaStreams Observed KafkaStreams instance
     */
    public synchronized void kafkaStreamsCreated(@Observes KafkaStreams kafkaStreams) {
        if (kafkaStreamsMetrics == null) {
            kafkaStreamsMetrics = new KafkaStreamsMetrics(kafkaStreams);
            try {
                kafkaStreamsMetrics.bindTo(registry);
            } catch (Throwable t) {
                log.warnf(t, "Unable to register metrics for KafkaStreams %s", kafkaStreams);
                tryToClose(kafkaStreamsMetrics);
            }
        } else {
            tryToClose(kafkaStreamsMetrics);
        }
    }

    void onStop(@Observes ShutdownEvent event) {
        tryToClose(kafkaStreamsMetrics);
    }

    void tryToClose(AutoCloseable c) {
        try {
            c.close();
        } catch (Exception e) {
            // intentionally empty
        }
    }

}
