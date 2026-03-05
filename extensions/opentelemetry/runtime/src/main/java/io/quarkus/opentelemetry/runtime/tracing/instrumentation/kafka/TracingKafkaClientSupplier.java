package io.quarkus.opentelemetry.runtime.tracing.instrumentation.kafka;

import java.util.Map;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import org.apache.kafka.clients.admin.Admin;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.streams.KafkaClientSupplier;
import org.apache.kafka.streams.processor.internals.DefaultKafkaClientSupplier;

import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.instrumentation.kafkaclients.v2_6.KafkaTelemetry;

@ApplicationScoped
public class TracingKafkaClientSupplier implements KafkaClientSupplier {

    private final KafkaTelemetry kafkaTelemetry;
    private final KafkaClientSupplier delegate;

    @Inject
    public TracingKafkaClientSupplier(OpenTelemetry openTelemetry) {
        this.kafkaTelemetry = KafkaTelemetry.create(openTelemetry);
        this.delegate = new DefaultKafkaClientSupplier();
    }

    @Override
    public Admin getAdmin(Map<String, Object> config) {
        return delegate.getAdmin(config);
    }

    @Override
    public Producer<byte[], byte[]> getProducer(Map<String, Object> config) {
        return kafkaTelemetry.wrap(delegate.getProducer(config));
    }

    @Override
    public Consumer<byte[], byte[]> getConsumer(Map<String, Object> config) {
        return kafkaTelemetry.wrap(delegate.getConsumer(config));
    }

    @Override
    public Consumer<byte[], byte[]> getRestoreConsumer(Map<String, Object> config) {
        return delegate.getRestoreConsumer(config);
    }

    @Override
    public Consumer<byte[], byte[]> getGlobalConsumer(Map<String, Object> config) {
        return kafkaTelemetry.wrap(delegate.getGlobalConsumer(config));
    }
}
