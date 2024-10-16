package io.quarkus.opentelemetry.runtime.tracing.cdi;

import static io.quarkus.opentelemetry.runtime.config.build.OTelBuildConfig.INSTRUMENTATION_NAME;

import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.function.BiConsumer;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.context.RequestScoped;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Singleton;

import io.opentelemetry.api.GlobalOpenTelemetry;
import io.opentelemetry.api.baggage.Baggage;
import io.opentelemetry.api.baggage.BaggageBuilder;
import io.opentelemetry.api.baggage.BaggageEntry;
import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanContext;
import io.opentelemetry.api.trace.StatusCode;
import io.opentelemetry.api.trace.Tracer;
import io.quarkus.arc.DefaultBean;

@Singleton
public class TracerProducer {

    @Produces
    @ApplicationScoped
    @DefaultBean
    public Tracer getTracer() {
        return GlobalOpenTelemetry.getTracer(INSTRUMENTATION_NAME);
    }

    @Produces
    @RequestScoped
    @DefaultBean
    public Span getSpan() {
        return new Span() {
            @Override
            public <T> Span setAttribute(final AttributeKey<T> key, final T value) {
                return Span.current().setAttribute(key, value);
            }

            @Override
            public Span addEvent(final String name, final Attributes attributes) {
                return Span.current().addEvent(name, attributes);
            }

            @Override

            public Span addEvent(
                    final String name,
                    final Attributes attributes,
                    final long timestamp,
                    final TimeUnit unit) {
                return Span.current().addEvent(name, attributes, timestamp, unit);
            }

            @Override
            public Span setStatus(final StatusCode statusCode, final String description) {
                return Span.current().setStatus(statusCode, description);
            }

            @Override
            public Span recordException(final Throwable exception, final Attributes additionalAttributes) {
                return Span.current().recordException(exception, additionalAttributes);
            }

            @Override
            public Span updateName(final String name) {
                return Span.current().updateName(name);
            }

            @Override
            public void end() {
                Span.current().end();
            }

            @Override
            public void end(final long timestamp, final TimeUnit unit) {
                Span.current().end(timestamp, unit);
            }

            @Override
            public SpanContext getSpanContext() {
                return Span.current().getSpanContext();
            }

            @Override
            public boolean isRecording() {
                return Span.current().isRecording();
            }
        };
    }

    @Produces
    @RequestScoped
    @DefaultBean
    public Baggage getBaggage() {
        return new Baggage() {
            @Override
            public int size() {
                return Baggage.current().size();
            }

            @Override
            public void forEach(final BiConsumer<? super String, ? super BaggageEntry> consumer) {
                Baggage.current().forEach(consumer);
            }

            @Override
            public Map<String, BaggageEntry> asMap() {
                return Baggage.current().asMap();
            }

            @Override
            public String getEntryValue(final String entryKey) {
                return Baggage.current().getEntryValue(entryKey);
            }

            @Override
            public BaggageBuilder toBuilder() {
                return Baggage.current().toBuilder();
            }
        };
    }
}
