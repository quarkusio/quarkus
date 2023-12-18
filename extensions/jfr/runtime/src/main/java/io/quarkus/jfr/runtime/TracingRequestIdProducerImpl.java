package io.quarkus.jfr.runtime;

import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.HttpHeaders;

import org.eclipse.microprofile.config.ConfigProvider;

import io.opentelemetry.api.trace.Span;

@RequestScoped
public class TracingRequestIdProducerImpl implements RequestIdProducer {

    private final static String REQUEST_ID_HEADER = ConfigProvider.getConfig()
            .getOptionalValue("quarkus.jfr.request-id-header", String.class).orElse("X-Request-ID");

    @Context
    HttpHeaders headers;

    @Inject
    Span span;

    public TracingRequestId create() {
        String requestId = headers.getHeaderString(REQUEST_ID_HEADER);
        return new TracingRequestId(requestId, span.getSpanContext().getTraceId(), span.getSpanContext().getSpanId());
    }
}
