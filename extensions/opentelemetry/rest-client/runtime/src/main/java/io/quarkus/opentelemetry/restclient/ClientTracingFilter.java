package io.quarkus.opentelemetry.restclient;

import java.io.IOException;

import javax.annotation.Priority;
import javax.ws.rs.Priorities;
import javax.ws.rs.client.ClientRequestContext;
import javax.ws.rs.client.ClientRequestFilter;
import javax.ws.rs.client.ClientResponseContext;
import javax.ws.rs.client.ClientResponseFilter;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;

import org.jboss.resteasy.client.jaxrs.internal.ClientRequestContextImpl;

import io.opentelemetry.api.GlobalOpenTelemetry;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanBuilder;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.api.trace.StatusCode;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.context.Context;
import io.opentelemetry.context.propagation.TextMapPropagator;
import io.opentelemetry.context.propagation.TextMapSetter;
import io.opentelemetry.semconv.trace.attributes.SemanticAttributes;

@Priority(Priorities.HEADER_DECORATOR)
public class ClientTracingFilter implements ClientRequestFilter, ClientResponseFilter {
    private static final TextMapPropagator TEXT_MAP_PROPAGATOR = GlobalOpenTelemetry.getPropagators().getTextMapPropagator();

    private Tracer tracer;
    private Span clientSpan;

    public ClientTracingFilter(Tracer tracer) {
        this.tracer = tracer;
    }

    @Override
    public void filter(ClientRequestContext requestContext) throws IOException {
        // Create new span
        SpanBuilder builder = tracer.spanBuilder(requestContext.getUri().getPath().substring(1))
                .setSpanKind(SpanKind.CLIENT);

        // Add attributes
        builder.setAttribute(SemanticAttributes.HTTP_METHOD,
                ((ClientRequestContextImpl) requestContext).getInvocation().getMethod());
        builder.setAttribute(SemanticAttributes.HTTP_URL, requestContext.getUri().toString());

        clientSpan = builder.startSpan();
        TEXT_MAP_PROPAGATOR.inject(Context.current().with(clientSpan), requestContext.getHeaders(), SETTER);
    }

    @Override
    public void filter(ClientRequestContext requestContext, ClientResponseContext responseContext) throws IOException {
        if (clientSpan != null) {
            String pathTemplate = (String) requestContext.getProperty("UrlPathTemplate");
            if (pathTemplate != null && !pathTemplate.isEmpty()) {
                clientSpan.updateName(pathTemplate.substring(1));
            }

            clientSpan.setAttribute(SemanticAttributes.HTTP_STATUS_CODE, responseContext.getStatus());

            if (!responseContext.getStatusInfo().getFamily().equals(Response.Status.Family.SUCCESSFUL)) {
                // TODO Is this ok as we don't have an exception to call recordException() with?
                clientSpan.setStatus(StatusCode.ERROR, responseContext.getStatusInfo().getReasonPhrase());
            }

            clientSpan.end();
        }
    }

    private static final TextMapSetter<MultivaluedMap<String, Object>> SETTER = new TextMapSetter<MultivaluedMap<String, Object>>() {
        @Override
        public void set(MultivaluedMap<String, Object> carrier, String key, String value) {
            carrier.add(key, value);
        }
    };
}
