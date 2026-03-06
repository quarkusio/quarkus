package io.quarkus.opentelemetry.runtime.tracing.instrumentation.kafka;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.Map;

import org.apache.kafka.clients.admin.Admin;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.streams.KafkaClientSupplier;
import org.junit.jupiter.api.Test;

import io.opentelemetry.instrumentation.kafkaclients.v2_6.KafkaTelemetry;

public class TracingKafkaClientSupplierTest {

    private final Map<String, Object> config = Collections.emptyMap();

    @SuppressWarnings("unchecked")
    @Test
    public void shouldWrapProducerWithTracing() {
        KafkaClientSupplier delegate = mock(KafkaClientSupplier.class);
        KafkaTelemetry telemetry = mock(KafkaTelemetry.class);
        Producer<byte[], byte[]> rawProducer = mock(Producer.class);
        Producer<byte[], byte[]> wrappedProducer = mock(Producer.class);

        when(delegate.getProducer(config)).thenReturn(rawProducer);
        when(telemetry.wrap(rawProducer)).thenReturn(wrappedProducer);

        TracingKafkaClientSupplier supplier = new TracingKafkaClientSupplier(telemetry, delegate);
        Producer<byte[], byte[]> result = supplier.getProducer(config);

        assertThat(result).isSameAs(wrappedProducer);
        verify(telemetry).wrap(rawProducer);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void shouldWrapConsumerWithTracing() {
        KafkaClientSupplier delegate = mock(KafkaClientSupplier.class);
        KafkaTelemetry telemetry = mock(KafkaTelemetry.class);
        Consumer<byte[], byte[]> rawConsumer = mock(Consumer.class);
        Consumer<byte[], byte[]> wrappedConsumer = mock(Consumer.class);

        when(delegate.getConsumer(config)).thenReturn(rawConsumer);
        when(telemetry.wrap(rawConsumer)).thenReturn(wrappedConsumer);

        TracingKafkaClientSupplier supplier = new TracingKafkaClientSupplier(telemetry, delegate);
        Consumer<byte[], byte[]> result = supplier.getConsumer(config);

        assertThat(result).isSameAs(wrappedConsumer);
        verify(telemetry).wrap(rawConsumer);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void shouldWrapGlobalConsumerWithTracing() {
        KafkaClientSupplier delegate = mock(KafkaClientSupplier.class);
        KafkaTelemetry telemetry = mock(KafkaTelemetry.class);
        Consumer<byte[], byte[]> rawConsumer = mock(Consumer.class);
        Consumer<byte[], byte[]> wrappedConsumer = mock(Consumer.class);

        when(delegate.getGlobalConsumer(config)).thenReturn(rawConsumer);
        when(telemetry.wrap(rawConsumer)).thenReturn(wrappedConsumer);

        TracingKafkaClientSupplier supplier = new TracingKafkaClientSupplier(telemetry, delegate);
        Consumer<byte[], byte[]> result = supplier.getGlobalConsumer(config);

        assertThat(result).isSameAs(wrappedConsumer);
        verify(telemetry).wrap(rawConsumer);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void shouldNotWrapRestoreConsumer() {
        KafkaClientSupplier delegate = mock(KafkaClientSupplier.class);
        KafkaTelemetry telemetry = mock(KafkaTelemetry.class);
        Consumer<byte[], byte[]> rawConsumer = mock(Consumer.class);

        when(delegate.getRestoreConsumer(config)).thenReturn(rawConsumer);

        TracingKafkaClientSupplier supplier = new TracingKafkaClientSupplier(telemetry, delegate);
        Consumer<byte[], byte[]> result = supplier.getRestoreConsumer(config);

        assertThat(result).isSameAs(rawConsumer);
        verifyNoInteractions(telemetry);
    }

    @Test
    public void shouldDelegateAdmin() {
        KafkaClientSupplier delegate = mock(KafkaClientSupplier.class);
        KafkaTelemetry telemetry = mock(KafkaTelemetry.class);
        Admin admin = mock(Admin.class);

        when(delegate.getAdmin(config)).thenReturn(admin);

        TracingKafkaClientSupplier supplier = new TracingKafkaClientSupplier(telemetry, delegate);
        Admin result = supplier.getAdmin(config);

        assertThat(result).isSameAs(admin);
        verifyNoInteractions(telemetry);
    }
}
