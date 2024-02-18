package io.quarkus.jfr.runtime;

import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;

import io.opentelemetry.api.trace.Span;

@RequestScoped
public class OTelIdProducer implements IdProducer {

    @Inject
    Span span;

    @Override
    public String getTraceId() {
        return span.getSpanContext().getTraceId();
    }

    @Override
    public String getSpanId() {
        return span.getSpanContext().getSpanId();
    }
}
