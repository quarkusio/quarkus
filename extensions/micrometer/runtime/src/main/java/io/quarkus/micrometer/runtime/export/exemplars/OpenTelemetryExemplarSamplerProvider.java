package io.quarkus.micrometer.runtime.export.exemplars;

import java.util.Optional;
import java.util.function.Function;

import jakarta.enterprise.inject.Produces;

import io.opentelemetry.api.trace.Span;
import io.opentelemetry.context.Context;
import io.quarkus.opentelemetry.runtime.QuarkusContextStorage;

public class OpenTelemetryExemplarSamplerProvider {

    @Produces
    public Optional<io.prometheus.metrics.tracer.common.SpanContext> exemplarSampler() {

        return Optional.of(new io.prometheus.metrics.tracer.common.SpanContext() {
            @Override
            public String getCurrentTraceId() {
                return get(io.opentelemetry.api.trace.SpanContext::getTraceId);
            }

            @Override
            public String getCurrentSpanId() {
                return get(io.opentelemetry.api.trace.SpanContext::getSpanId);
            }

            @Override
            public boolean isCurrentSpanSampled() {
                return Boolean.TRUE.equals(get(io.opentelemetry.api.trace.SpanContext::isSampled));
            }

            @Override
            public void markCurrentSpanAsExemplar() {
            }

            private <T> T get(Function<io.opentelemetry.api.trace.SpanContext, T> valueExtractor) {
                // QuarkusContextStorage.current() may return null when there is no Vert.x / fallback
                // context. OpenTelemetry 1.62+ treats Span.fromContextOrNull(null) as API misuse and
                // emits a WARNING on io.opentelemetry.usage (#55855).
                Context context = QuarkusContextStorage.INSTANCE.current();
                if (context == null) {
                    return null;
                }
                return Optional.ofNullable(Span.fromContextOrNull(context))
                        .map(Span::getSpanContext)
                        .map(valueExtractor)
                        .orElse(null);
            }
        });
    }
}
