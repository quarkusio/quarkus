package io.quarkus.jfr.runtime;

import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.HttpHeaders;

import io.opentelemetry.api.trace.Span;

@RequestScoped
public class TracingRequestIdProducer implements RequestIdProducer {

    @Context
    HttpHeaders headers;

    @Inject
    Span span;

    public TracingRequestId create() {
        String requestId = headers.getHeaderString(REQUEST_ID_HEADER);
        return new TracingRequestId(requestId, span.getSpanContext().getTraceId(), span.getSpanContext().getSpanId());
    }
}
