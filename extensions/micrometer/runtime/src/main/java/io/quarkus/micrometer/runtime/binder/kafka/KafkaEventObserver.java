package io.quarkus.micrometer.runtime.binder.kafka;

import java.util.HashMap;
import java.util.Map;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;

import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.producer.Producer;
import org.jboss.logging.Logger;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Metrics;
import io.micrometer.core.instrument.binder.kafka.KafkaClientMetrics;

@ApplicationScoped
public class KafkaEventObserver {
    private static final Logger log = Logger.getLogger(KafkaEventObserver.class);

    MeterRegistry registry = Metrics.globalRegistry;
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

    void tryToClose(AutoCloseable c) {
        try {
            c.close();
        } catch (Exception e) {
            // intentionally empty
        }
    }
}
