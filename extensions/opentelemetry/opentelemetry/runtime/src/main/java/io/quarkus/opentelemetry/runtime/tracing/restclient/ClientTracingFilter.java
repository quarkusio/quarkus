package io.quarkus.opentelemetry.runtime.tracing.restclient;

import java.io.IOException;
import java.net.URI;

import javax.annotation.Priority;
import javax.ws.rs.Priorities;
import javax.ws.rs.client.ClientRequestContext;
import javax.ws.rs.client.ClientRequestFilter;
import javax.ws.rs.client.ClientResponseContext;
import javax.ws.rs.client.ClientResponseFilter;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;

import io.opentelemetry.api.GlobalOpenTelemetry;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanBuilder;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.api.trace.StatusCode;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.context.Context;
import io.opentelemetry.context.Scope;
import io.opentelemetry.context.propagation.TextMapPropagator;
import io.opentelemetry.context.propagation.TextMapSetter;
import io.opentelemetry.semconv.trace.attributes.SemanticAttributes;

@Priority(Priorities.HEADER_DECORATOR)
public class ClientTracingFilter implements ClientRequestFilter, ClientResponseFilter {
    private static final TextMapPropagator TEXT_MAP_PROPAGATOR = GlobalOpenTelemetry.getPropagators().getTextMapPropagator();

    private static final String SCOPE_KEY = ClientTracingFilter.class.getName() + ".scope";
    private static final String SPAN_KEY = ClientTracingFilter.class.getName() + ".span";

    private final Tracer tracer;

    public ClientTracingFilter(Tracer tracer) {
        this.tracer = tracer;
    }

    @Override
    public void filter(ClientRequestContext requestContext) throws IOException {
        // Create new span
        SpanBuilder builder = tracer.spanBuilder(getSpanName(requestContext))
                .setSpanKind(SpanKind.CLIENT);

        // Add attributes
        builder.setAttribute(SemanticAttributes.HTTP_METHOD, requestContext.getMethod());
        builder.setAttribute(SemanticAttributes.HTTP_URL, filterUserInfo(requestContext.getUri().toString()));

        final Span clientSpan = builder.startSpan();

        requestContext.setProperty(SPAN_KEY, clientSpan);
        requestContext.setProperty(SCOPE_KEY, clientSpan.makeCurrent());

        TEXT_MAP_PROPAGATOR.inject(Context.current(), requestContext.getHeaders(), SETTER);
    }

    String filterUserInfo(String httpUrl) {
        if (httpUrl.contains("@")) {
            URI uri = URI.create(httpUrl);
            httpUrl = httpUrl.replace(uri.getUserInfo() + "@", "");
        }
        return httpUrl;
    }

    private String getSpanName(ClientRequestContext requestContext) {
        final String uriPath = requestContext.getUri().getPath();
        if (uriPath.length() > 1) {
            return uriPath.substring(1);
        } else {
            // Generate span name as we have empty or "/" on @Path
            return "HTTP " + requestContext.getMethod();
        }
    }

    @Override
    public void filter(ClientRequestContext requestContext, ClientResponseContext responseContext) throws IOException {
        final Span clientSpan = (Span) requestContext.getProperty(SPAN_KEY);
        if (clientSpan != null) {
            String pathTemplate = (String) requestContext.getProperty("UrlPathTemplate");
            if (pathTemplate != null && pathTemplate.length() > 1) {
                clientSpan.updateName(pathTemplate.substring(1));
            }

            clientSpan.setAttribute(SemanticAttributes.HTTP_STATUS_CODE, responseContext.getStatus());
            clientSpan.setAttribute(SemanticAttributes.HTTP_RESPONSE_CONTENT_LENGTH, responseContext.getLength());

            String requestContentLength = requestContext.getHeaderString(HttpHeaders.CONTENT_LENGTH);

            if (requestContentLength != null && requestContentLength.length() > 0 && Long.parseLong(requestContentLength) > 0) {
                clientSpan.setAttribute(SemanticAttributes.HTTP_REQUEST_CONTENT_LENGTH, Long.valueOf(requestContentLength));
            }

            if (!responseContext.getStatusInfo().getFamily().equals(Response.Status.Family.SUCCESSFUL)) {
                // TODO Is this ok as we don't have an exception to call recordException() with?
                clientSpan.setStatus(StatusCode.ERROR, responseContext.getStatusInfo().getReasonPhrase());
            }

            clientSpan.end();

            final Scope spanScope = (Scope) requestContext.getProperty(SCOPE_KEY);
            if (spanScope != null) {
                spanScope.close();
            }
        }
    }

    private static final TextMapSetter<MultivaluedMap<String, Object>> SETTER = new TextMapSetter<MultivaluedMap<String, Object>>() {
        @Override
        public void set(MultivaluedMap<String, Object> carrier, String key, String value) {
            carrier.add(key, value);
        }
    };
}
