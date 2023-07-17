package io.quarkus.opentelemetry.runtime.tracing.intrumentation.restclient;

import static io.quarkus.opentelemetry.runtime.config.build.OTelBuildConfig.INSTRUMENTATION_NAME;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;

import java.net.URI;
import java.util.List;

import jakarta.inject.Inject;
import jakarta.ws.rs.client.ClientRequestContext;
import jakarta.ws.rs.client.ClientRequestFilter;
import jakarta.ws.rs.client.ClientResponseContext;
import jakarta.ws.rs.client.ClientResponseFilter;
import jakarta.ws.rs.core.UriBuilder;
import jakarta.ws.rs.ext.Provider;

import io.opentelemetry.api.GlobalOpenTelemetry;
import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.context.Context;
import io.opentelemetry.context.Scope;
import io.opentelemetry.context.propagation.TextMapSetter;
import io.opentelemetry.instrumentation.api.instrumenter.Instrumenter;
import io.opentelemetry.instrumentation.api.instrumenter.InstrumenterBuilder;
import io.opentelemetry.instrumentation.api.instrumenter.http.HttpClientAttributesExtractor;
import io.opentelemetry.instrumentation.api.instrumenter.http.HttpClientAttributesGetter;
import io.opentelemetry.instrumentation.api.instrumenter.http.HttpSpanNameExtractor;
import io.opentelemetry.instrumentation.api.instrumenter.http.HttpSpanStatusExtractor;
import io.opentelemetry.instrumentation.api.instrumenter.net.NetClientAttributesGetter;
import io.opentelemetry.semconv.trace.attributes.SemanticAttributes;
import io.quarkus.arc.Unremovable;
import io.quarkus.opentelemetry.runtime.QuarkusContextStorage;

/**
 * A client filter for the JAX-RS Client and MicroProfile REST Client that records OpenTelemetry data. This is only used
 * by RESTEasy Classic, because the handling implementation is provided by RESTEasy. This is not used by RESTEasy
 * Reactive because tracing is handled by Vert.x.
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
        ClientNetAttributesGetter clientNetAttributesExtractor = new ClientNetAttributesGetter();

        InstrumenterBuilder<ClientRequestContext, ClientResponseContext> builder = Instrumenter.builder(
                openTelemetry,
                INSTRUMENTATION_NAME,
                HttpSpanNameExtractor.create(clientAttributesExtractor));

        this.instrumenter = builder
                .setSpanStatusExtractor(HttpSpanStatusExtractor.create(clientAttributesExtractor))
                .addAttributesExtractor(HttpClientAttributesExtractor.create(
                        clientAttributesExtractor, clientNetAttributesExtractor))
                .buildClientInstrumenter(new ClientRequestContextTextMapSetter());
    }

    @Override
    public void filter(final ClientRequestContext request) {
        io.vertx.core.Context vertxContext = getVertxContext(request);
        io.opentelemetry.context.Context parentContext = QuarkusContextStorage.getContext(vertxContext);
        if (parentContext == null) {
            parentContext = io.opentelemetry.context.Context.current();
        }

        // For each request, we need a new OTel Context from the **current one**
        // the parent context needs to be the one from which the call originates.

        if (instrumenter.shouldStart(parentContext, request)) {
            Context spanContext = instrumenter.start(parentContext, request);
            // Create a new scope with an empty termination callback.
            Scope scope = new Scope() {
                @Override
                public void close() {

                }
            };
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

    private static class ClientAttributesExtractor
            implements HttpClientAttributesGetter<ClientRequestContext, ClientResponseContext> {

        @Override
        public String getUrl(final ClientRequestContext request) {
            URI uri = request.getUri();
            if (uri.getUserInfo() != null) {
                return UriBuilder.fromUri(uri).userInfo(null).build().toString();
            }
            return uri.toString();
        }

        @Override
        public String getFlavor(final ClientRequestContext request, final ClientResponseContext response) {
            return null;
        }

        @Override
        public String getMethod(final ClientRequestContext request) {
            return request.getMethod();
        }

        @Override
        public List<String> getRequestHeader(final ClientRequestContext request, final String name) {
            return request.getStringHeaders().getOrDefault(name, emptyList());
        }

        @Override
        public Integer getStatusCode(ClientRequestContext clientRequestContext,
                ClientResponseContext clientResponseContext, Throwable error) {
            return clientResponseContext.getStatus();
        }

        @Override
        public List<String> getResponseHeader(final ClientRequestContext request, final ClientResponseContext response,
                final String name) {
            return response.getHeaders().getOrDefault(name, emptyList());
        }
    }

    private static class ClientNetAttributesGetter
            implements NetClientAttributesGetter<ClientRequestContext, ClientResponseContext> {

        @Override
        public String getTransport(ClientRequestContext clientRequestContext, ClientResponseContext clientResponseContext) {
            return SemanticAttributes.NetTransportValues.IP_TCP;
        }

        @Override
        public String getPeerName(ClientRequestContext clientRequestContext) {
            return clientRequestContext.getUri().getHost();
        }

        @Override
        public Integer getPeerPort(ClientRequestContext clientRequestContext) {
            return clientRequestContext.getUri().getPort();
        }
    }
}
