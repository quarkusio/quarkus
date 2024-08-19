package io.quarkus.opentelemetry.runtime.tracing.intrumentation.resteasy;

import org.jboss.resteasy.reactive.server.ServerResponseFilter;

import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanContext;
import jakarta.ws.rs.container.ContainerResponseContext;

public class TraceResponseFilter {

    @ServerResponseFilter
    public void responseBasicHeaderFilter(ContainerResponseContext responseContext) {
        SpanContext spanContext = Span.current().getSpanContext();
        responseContext
            .getHeaders()
            .putSingle(
                "traceresponse",
                String.format(
                    "00-%s-%s-%s  ",
                    spanContext.getTraceId(), spanContext.getSpanId(), spanContext.getTraceFlags()));
    }
}
