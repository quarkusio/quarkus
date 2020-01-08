package io.quarkus.stackdriver.runtime;

import javax.ws.rs.client.ClientRequestContext;
import javax.ws.rs.client.ClientRequestFilter;
import javax.ws.rs.ext.Provider;

import io.opencensus.trace.Span;
import io.opencensus.trace.Tracing;

@Provider
public class SpanPropagationClientRequestFilter implements ClientRequestFilter {

    @Override
    public void filter(ClientRequestContext requestContext) {
        Span span = Tracing.getTracer().getCurrentSpan();
        requestContext.getHeaders().add("X-SpanID", span.getContext().getSpanId().toLowerBase16());
        requestContext.getHeaders().add("X-TraceID", span.getContext().getTraceId().toLowerBase16());
    }
}
