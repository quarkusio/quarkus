package io.quarkus.opentelemetry.runtime.tracing.restclient;

import static io.quarkus.opentelemetry.runtime.OpenTelemetryConfig.INSTRUMENTATION_NAME;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;

import java.net.URI;
import java.util.List;

import javax.inject.Inject;
import javax.ws.rs.client.ClientRequestContext;
import javax.ws.rs.client.ClientRequestFilter;
import javax.ws.rs.client.ClientResponseContext;
import javax.ws.rs.client.ClientResponseFilter;
import javax.ws.rs.core.UriBuilder;
import javax.ws.rs.ext.Provider;

import io.opentelemetry.api.GlobalOpenTelemetry;
import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.context.Context;
import io.opentelemetry.context.Scope;
import io.opentelemetry.context.propagation.TextMapSetter;
import io.opentelemetry.instrumentation.api.instrumenter.Instrumenter;
import io.opentelemetry.instrumentation.api.instrumenter.InstrumenterBuilder;
import io.opentelemetry.instrumentation.api.instrumenter.SpanNameExtractor;
import io.opentelemetry.instrumentation.api.instrumenter.http.HttpClientAttributesExtractor;
import io.opentelemetry.instrumentation.api.instrumenter.http.HttpSpanStatusExtractor;
import io.quarkus.arc.Unremovable;

@Unremovable
@Provider
public class OpenTelemetryClientFilter implements ClientRequestFilter, ClientResponseFilter {
    public static final String REST_CLIENT_OTEL_SPAN_CLIENT_CONTEXT = "otel.span.client.context";
    public static final String REST_CLIENT_OTEL_SPAN_CLIENT_PARENT_CONTEXT = "otel.span.client.parentContext";
    public static final String REST_CLIENT_OTEL_SPAN_CLIENT_SCOPE = "otel.span.client.scope";

    private Instrumenter<ClientRequestContext, ClientResponseContext> instrumenter;

    // RESTEasy requires no-arg constructor for CDI injection: https://issues.redhat.com/browse/RESTEASY-1538
    // In Reactive Rest Client this is the constructor called. In the classic is the next one with injection.
    public OpenTelemetryClientFilter() {
        this(GlobalOpenTelemetry.get());
    }

    @Inject
    public OpenTelemetryClientFilter(final OpenTelemetry openTelemetry) {
        ClientAttributesExtractor clientAttributesExtractor = new ClientAttributesExtractor();

        InstrumenterBuilder<ClientRequestContext, ClientResponseContext> builder = Instrumenter.builder(
                openTelemetry,
                INSTRUMENTATION_NAME,
                new ClientSpanNameExtractor());

        this.instrumenter = builder
                .setSpanStatusExtractor(HttpSpanStatusExtractor.create(clientAttributesExtractor))
                .addAttributesExtractor(clientAttributesExtractor)
                .newClientInstrumenter(new ClientRequestContextTextMapSetter());
    }

    @Override
    public void filter(final ClientRequestContext request) {
        Context parentContext = Context.current();
        if (instrumenter.shouldStart(parentContext, request)) {
            Context spanContext = instrumenter.start(parentContext, request);
            Scope scope = spanContext.makeCurrent();
            request.setProperty(REST_CLIENT_OTEL_SPAN_CLIENT_CONTEXT, spanContext);
            request.setProperty(REST_CLIENT_OTEL_SPAN_CLIENT_PARENT_CONTEXT, parentContext);
            request.setProperty(REST_CLIENT_OTEL_SPAN_CLIENT_SCOPE, scope);
        }
    }

    @Override
    public void filter(final ClientRequestContext request, final ClientResponseContext response) {
        Scope scope = (Scope) request.getProperty(REST_CLIENT_OTEL_SPAN_CLIENT_SCOPE);
        if (scope == null) {
            return;
        }

        Context spanContext = (Context) request.getProperty(REST_CLIENT_OTEL_SPAN_CLIENT_CONTEXT);
        try {
            instrumenter.end(spanContext, request, response, null);
        } finally {
            scope.close();

            request.removeProperty(REST_CLIENT_OTEL_SPAN_CLIENT_CONTEXT);
            request.removeProperty(REST_CLIENT_OTEL_SPAN_CLIENT_PARENT_CONTEXT);
            request.removeProperty(REST_CLIENT_OTEL_SPAN_CLIENT_SCOPE);
        }
    }

    private static class ClientRequestContextTextMapSetter implements TextMapSetter<ClientRequestContext> {
        @Override
        public void set(final ClientRequestContext carrier, final String key, final String value) {
            if (carrier != null) {
                carrier.getHeaders().put(key, singletonList(value));
            }
        }
    }

    private static class ClientSpanNameExtractor implements SpanNameExtractor<ClientRequestContext> {
        @Override
        public String extract(final ClientRequestContext request) {
            String pathTemplate = (String) request.getProperty("UrlPathTemplate");
            if (pathTemplate != null && pathTemplate.length() > 1) {
                return pathTemplate.substring(1);
            }

            String uriPath = request.getUri().getPath();
            if (uriPath.length() > 1) {
                return uriPath.substring(1);
            }

            return "HTTP " + request.getMethod();
        }
    }

    private static class ClientAttributesExtractor
            extends HttpClientAttributesExtractor<ClientRequestContext, ClientResponseContext> {

        @Override
        protected String url(final ClientRequestContext request) {
            URI uri = request.getUri();
            if (uri.getUserInfo() != null) {
                return UriBuilder.fromUri(uri).userInfo(null).build().toString();
            }
            return uri.toString();
        }

        @Override
        protected String flavor(final ClientRequestContext request, final ClientResponseContext response) {
            return null;
        }

        @Override
        protected String method(final ClientRequestContext request) {
            return request.getMethod();
        }

        @Override
        protected List<String> requestHeader(final ClientRequestContext request, final String name) {
            return request.getStringHeaders().getOrDefault(name, emptyList());
        }

        @Override
        protected Long requestContentLength(final ClientRequestContext request, final ClientResponseContext response) {
            return null;
        }

        @Override
        protected Long requestContentLengthUncompressed(final ClientRequestContext request,
                final ClientResponseContext response) {
            return null;
        }

        @Override
        protected Integer statusCode(final ClientRequestContext request, final ClientResponseContext response) {
            return response.getStatus();
        }

        @Override
        protected Long responseContentLength(final ClientRequestContext request, final ClientResponseContext response) {
            return null;
        }

        @Override
        protected Long responseContentLengthUncompressed(final ClientRequestContext request,
                final ClientResponseContext response) {
            return null;
        }

        @Override
        protected List<String> responseHeader(final ClientRequestContext request, final ClientResponseContext response,
                final String name) {
            return response.getHeaders().getOrDefault(name, emptyList());
        }
    }
}
