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
import io.opentelemetry.instrumentation.api.instrumenter.http.HttpClientAttributesGetter;
import io.opentelemetry.instrumentation.api.instrumenter.http.HttpSpanStatusExtractor;
import io.quarkus.arc.Unremovable;
import io.quarkus.opentelemetry.runtime.QuarkusContextStorage;

/**
 * A client filter for the JAX-RS Client and MicroProfile REST Client that records OpenTelemetry data.
 */
@Unremovable
@Provider
public class OpenTelemetryClientFilter implements ClientRequestFilter, ClientResponseFilter {
    public static final String REST_CLIENT_OTEL_SPAN_CLIENT_CONTEXT = "otel.span.client.context";
    public static final String REST_CLIENT_OTEL_SPAN_CLIENT_PARENT_CONTEXT = "otel.span.client.parentContext";
    public static final String REST_CLIENT_OTEL_SPAN_CLIENT_SCOPE = "otel.span.client.scope";

    /**
     * Property stored in the Client Request context to retrieve the captured Vert.x context.
     * This context is captured and stored by the Reactive REST Client.
     *
     * We use this property to avoid having to depend on the Reactive REST Client explicitly.
     */
    private static final String VERTX_CONTEXT_PROPERTY = "__context";

    private final Instrumenter<ClientRequestContext, ClientResponseContext> instrumenter;

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
                .addAttributesExtractor(HttpClientAttributesExtractor.create(clientAttributesExtractor))
                .newClientInstrumenter(new ClientRequestContextTextMapSetter());
    }

    @Override
    public void filter(final ClientRequestContext request) {
        Context parentContext = Context.current();
        if (instrumenter.shouldStart(parentContext, request)) {
            Context spanContext = instrumenter.start(parentContext, request);
            Scope scope = QuarkusContextStorage.INSTANCE.attach(getVertxContext(request), spanContext);
            request.setProperty(REST_CLIENT_OTEL_SPAN_CLIENT_CONTEXT, spanContext);
            request.setProperty(REST_CLIENT_OTEL_SPAN_CLIENT_PARENT_CONTEXT, parentContext);
            request.setProperty(REST_CLIENT_OTEL_SPAN_CLIENT_SCOPE, scope);
        }
    }

    private static io.vertx.core.Context getVertxContext(final ClientRequestContext request) {
        io.vertx.core.Context vertxContext = (io.vertx.core.Context) request.getProperty(VERTX_CONTEXT_PROPERTY);
        if (vertxContext == null) {
            return QuarkusContextStorage.getVertxContext();
        } else {
            return vertxContext;
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

    static boolean isReactiveClient(final ClientRequestContext request) {
        return request.getProperty(VERTX_CONTEXT_PROPERTY) != null;
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
                return pathTemplate;
            }

            String uriPath = request.getUri().getPath();
            if (uriPath != null && uriPath.length() > 1) {
                return uriPath;
            }

            return "HTTP " + request.getMethod();
        }
    }

    private static class ClientAttributesExtractor
            implements HttpClientAttributesGetter<ClientRequestContext, ClientResponseContext> {

        @Override
        public String url(final ClientRequestContext request) {
            URI uri = request.getUri();
            if (uri.getUserInfo() != null) {
                return UriBuilder.fromUri(uri).userInfo(null).build().toString();
            }
            return uri.toString();
        }

        @Override
        public String flavor(final ClientRequestContext request, final ClientResponseContext response) {
            return null;
        }

        @Override
        public String method(final ClientRequestContext request) {
            return request.getMethod();
        }

        @Override
        public List<String> requestHeader(final ClientRequestContext request, final String name) {
            return request.getStringHeaders().getOrDefault(name, emptyList());
        }

        @Override
        public Long requestContentLength(final ClientRequestContext request, final ClientResponseContext response) {
            return null;
        }

        @Override
        public Long requestContentLengthUncompressed(final ClientRequestContext request,
                final ClientResponseContext response) {
            return null;
        }

        @Override
        public Integer statusCode(final ClientRequestContext request, final ClientResponseContext response) {
            return response.getStatus();
        }

        @Override
        public Long responseContentLength(final ClientRequestContext request, final ClientResponseContext response) {
            return null;
        }

        @Override
        public Long responseContentLengthUncompressed(final ClientRequestContext request,
                final ClientResponseContext response) {
            return null;
        }

        @Override
        public List<String> responseHeader(final ClientRequestContext request, final ClientResponseContext response,
                final String name) {
            return response.getHeaders().getOrDefault(name, emptyList());
        }
    }
}
