package io.quarkus.micrometer.runtime.binder.kafka;

import java.util.HashMap;
import java.util.Map;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;

import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.producer.Producer;
import org.jboss.logging.Logger;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Metrics;
import io.micrometer.core.instrument.binder.kafka.KafkaClientMetrics;
import io.quarkus.runtime.ShutdownEvent;

/**
 * Observer to create and register KafkaClientMetrics.
 *
 * This observer uses only classes from "kafka-clients" and none from "kafka-streams".
 *
 * Must be separated from KafkaStreamsEventObserver, because they use different dependencies and if only kafka-client is used,
 * the classes from kafka-streams aren't loaded.
 */
@ApplicationScoped
public class KafkaEventObserver {
    private static final Logger log = Logger.getLogger(KafkaEventObserver.class);

    final MeterRegistry registry = Metrics.globalRegistry;
    Map<Object, KafkaClientMetrics> clientMetrics = new HashMap<>();

    /**
     * Manage bind/close of KafkaClientMetrics for the specified Consumer.
     * If the consumer has not been seen before, it will be bound to the
     * Micrometer registry and instrumented using a Kafka MeterBinder.
     * If the consumer has been seen before, the MeterBinder will be closed.
     *
     * @param consumer Observed Kafka Consumer
     */
    public synchronized void consumerCreated(@Observes Consumer<?, ?> consumer) {
        KafkaClientMetrics metrics = clientMetrics.remove(consumer);
        if (metrics == null) {
            metrics = new KafkaClientMetrics(consumer);
            try {
                metrics.bindTo(registry);
                clientMetrics.put(consumer, metrics);
            } catch (Throwable t) {
                log.warnf(t, "Unable to register metrics for Kafka consumer %s", consumer);
                tryToClose(metrics);
            }
        } else {
            tryToClose(metrics);
        }
    }

    /**
     * Manage bind/close of KafkaClientMetrics for the specified Producer.
     * If the producer has not been seen before, it will be bound to the
     * Micrometer registry and instrumented using a Kafka MeterBinder.
     * If the producer has been seen before, the MeterBinder will be closed.
     *
     * @param producer Observed Kafka Producer
     */
    public synchronized void producerCreated(@Observes Producer<?, ?> producer) {
        KafkaClientMetrics metrics = clientMetrics.remove(producer);
        if (metrics == null) {
            metrics = new KafkaClientMetrics(producer);
            try {
                metrics.bindTo(registry);
                clientMetrics.put(producer, metrics);
            } catch (Throwable t) {
                log.warnf(t, "Unable to register metrics for Kafka producer %s", producer);
                tryToClose(metrics);
            }
        } else {
            tryToClose(metrics);
        }
    }

    void onStop(@Observes ShutdownEvent event) {
        clientMetrics.values().forEach(this::tryToClose);
    }

    void tryToClose(AutoCloseable c) {
        try {
            c.close();
        } catch (Exception e) {
            // intentionally empty
        }
    }
}
