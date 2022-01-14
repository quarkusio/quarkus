package io.quarkus.opentelemetry.runtime.tracing.vertx;

import static io.opentelemetry.semconv.trace.attributes.SemanticAttributes.HTTP_CLIENT_IP;
import static io.opentelemetry.semconv.trace.attributes.SemanticAttributes.HTTP_ROUTE;
import static io.quarkus.opentelemetry.runtime.OpenTelemetryConfig.INSTRUMENTATION_NAME;
import static io.quarkus.opentelemetry.runtime.QuarkusContextStorage.ACTIVE_CONTEXT;

import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;

import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.common.AttributesBuilder;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.context.Scope;
import io.opentelemetry.context.propagation.TextMapGetter;
import io.opentelemetry.context.propagation.TextMapSetter;
import io.opentelemetry.instrumentation.api.instrumenter.AttributesExtractor;
import io.opentelemetry.instrumentation.api.instrumenter.Instrumenter;
import io.opentelemetry.instrumentation.api.instrumenter.InstrumenterBuilder;
import io.opentelemetry.instrumentation.api.instrumenter.SpanNameExtractor;
import io.opentelemetry.instrumentation.api.instrumenter.http.HttpClientAttributesExtractor;
import io.opentelemetry.instrumentation.api.instrumenter.http.HttpServerAttributesExtractor;
import io.opentelemetry.instrumentation.api.instrumenter.http.HttpSpanNameExtractor;
import io.opentelemetry.instrumentation.api.instrumenter.http.HttpSpanStatusExtractor;
import io.quarkus.opentelemetry.runtime.QuarkusContextStorage;
import io.vertx.core.Context;
import io.vertx.core.MultiMap;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.http.impl.headers.HeadersAdaptor;
import io.vertx.core.http.impl.headers.HeadersMultiMap;
import io.vertx.core.net.SocketAddress;
import io.vertx.core.spi.observability.HttpRequest;
import io.vertx.core.spi.observability.HttpResponse;
import io.vertx.core.spi.tracing.SpanKind;
import io.vertx.core.spi.tracing.TagExtractor;
import io.vertx.core.spi.tracing.VertxTracer;
import io.vertx.core.tracing.TracingPolicy;

public class OpenTelemetryVertxTracer
        implements VertxTracer<OpenTelemetryVertxTracer.SpanOperation, OpenTelemetryVertxTracer.SpanOperation> {
    private final Instrumenter<HttpRequest, HttpResponse> serverInstrumenter;
    private final Instrumenter<HttpRequest, HttpResponse> clientInstrumenter;

    public OpenTelemetryVertxTracer(OpenTelemetry openTelemetry) {
        ServerAttributesExtractor serverAttributesExtractor = new ServerAttributesExtractor();

        InstrumenterBuilder<HttpRequest, HttpResponse> serverBuilder = Instrumenter.builder(
                openTelemetry,
                INSTRUMENTATION_NAME,
                ServerSpanNameExtractor.create(serverAttributesExtractor));

        this.serverInstrumenter = serverBuilder
                .setSpanStatusExtractor(HttpSpanStatusExtractor.create(serverAttributesExtractor))
                .addAttributesExtractor(serverAttributesExtractor)
                .addAttributesExtractor(new AdditionalServerAttributesExtractor())
                .newServerInstrumenter(new HttpRequestTextMapGetter());

        ClientAttributesExtractor clientAttributesExtractor = new ClientAttributesExtractor();

        InstrumenterBuilder<HttpRequest, HttpResponse> clientBuilder = Instrumenter.builder(
                openTelemetry,
                INSTRUMENTATION_NAME,
                HttpSpanNameExtractor.create(clientAttributesExtractor));

        this.clientInstrumenter = clientBuilder
                .setSpanStatusExtractor(HttpSpanStatusExtractor.create(serverAttributesExtractor))
                .addAttributesExtractor(clientAttributesExtractor)
                .newClientInstrumenter(new HttpRequestTextMapSetter());
    }

    @Override
    public <R> SpanOperation receiveRequest(
            final Context context,
            final SpanKind kind,
            final TracingPolicy policy,
            final R request,
            final String operation,
            final Iterable<Map.Entry<String, String>> headers,
            final TagExtractor<R> tagExtractor) {

        if (!(request instanceof HttpRequest)) {
            return null;
        }

        io.opentelemetry.context.Context parentContext = context.getLocal(ACTIVE_CONTEXT);
        if (parentContext == null) {
            parentContext = io.opentelemetry.context.Context.root();
        }

        if (serverInstrumenter.shouldStart(parentContext, (HttpRequest) request)) {
            io.opentelemetry.context.Context spanContext = serverInstrumenter.start(parentContext, (HttpRequest) request);
            Scope scope = QuarkusContextStorage.INSTANCE.attach(context, spanContext);
            return SpanOperation.span(context, (HttpRequest) request, spanContext, scope);
        }

        return null;
    }

    @Override
    public <R> void sendResponse(
            final Context context,
            final R response,
            final SpanOperation spanOperation,
            final Throwable failure,
            final TagExtractor<R> tagExtractor) {

        if (spanOperation == null) {
            return;
        }

        if (!(response instanceof HttpResponse)) {
            return;
        }

        Scope scope = spanOperation.getScope();
        if (scope == null) {
            return;
        }

        HttpRequest httpRequest = SpanRequest.request(spanOperation);
        HttpResponse httpResponse = (HttpResponse) response;

        try (scope) {
            serverInstrumenter.end(spanOperation.getSpanContext(), httpRequest, httpResponse, failure);
        }
    }

    @Override
    public <R> SpanOperation sendRequest(
            final Context context,
            final SpanKind kind,
            final TracingPolicy policy,
            final R request,
            final String operation,
            final BiConsumer<String, String> headers,
            final TagExtractor<R> tagExtractor) {

        if (!(request instanceof HttpRequest)) {
            return null;
        }

        io.opentelemetry.context.Context parentContext = context.getLocal(ACTIVE_CONTEXT);
        if (parentContext == null) {
            parentContext = io.opentelemetry.context.Context.root();
        }

        if (clientInstrumenter.shouldStart(parentContext, (HttpRequest) request)) {
            io.opentelemetry.context.Context spanContext = clientInstrumenter.start(parentContext,
                    WriteHeadersHttpRequest.request((HttpRequest) request, headers));
            Scope scope = QuarkusContextStorage.INSTANCE.attach(context, spanContext);
            return SpanOperation.span(context, (HttpRequest) request, spanContext, scope);
        }

        return null;
    }

    @Override
    public <R> void receiveResponse(
            final Context context,
            final R response,
            final SpanOperation spanOperation,
            final Throwable failure,
            final TagExtractor<R> tagExtractor) {

        if (spanOperation == null) {
            return;
        }

        if (!(response instanceof HttpResponse)) {
            return;
        }

        Scope scope = spanOperation.getScope();
        if (scope == null) {
            return;
        }

        HttpRequest httpRequest = SpanRequest.request(spanOperation);
        HttpResponse httpResponse = (HttpResponse) response;

        try (scope) {
            clientInstrumenter.end(spanOperation.getSpanContext(), httpRequest, httpResponse, failure);
        }
    }

    static class SpanOperation {
        private final Context context;
        private final HttpRequest request;
        private final io.opentelemetry.context.Context spanContext;
        private final Scope scope;

        public SpanOperation(Context context, HttpRequest request, io.opentelemetry.context.Context spanContext, Scope scope) {
            this.context = context;
            this.request = request;
            this.spanContext = spanContext;
            this.scope = scope;
        }

        public Context getContext() {
            return context;
        }

        public HttpRequest getRequest() {
            return request;
        }

        public io.opentelemetry.context.Context getSpanContext() {
            return spanContext;
        }

        public Scope getScope() {
            return scope;
        }

        static SpanOperation span(Context context, HttpRequest request, io.opentelemetry.context.Context spanContext,
                Scope scope) {
            return new SpanOperation(context, request, spanContext, scope);
        }
    }

    static class SpanRequest implements HttpRequest {
        private final SpanOperation spanOperation;

        SpanRequest(final SpanOperation spanOperation) {
            this.spanOperation = spanOperation;
        }

        public SpanOperation getSpanOperation() {
            return spanOperation;
        }

        @Override
        public int id() {
            return spanOperation.getRequest().id();
        }

        @Override
        public String uri() {
            return spanOperation.getRequest().uri();
        }

        @Override
        public String absoluteURI() {
            return spanOperation.getRequest().absoluteURI();
        }

        @Override
        public HttpMethod method() {
            return spanOperation.getRequest().method();
        }

        @Override
        public MultiMap headers() {
            return spanOperation.getRequest().headers();
        }

        @Override
        public SocketAddress remoteAddress() {
            return spanOperation.getRequest().remoteAddress();
        }

        static SpanRequest request(SpanOperation spanOperation) {
            return new SpanRequest(spanOperation);
        }
    }

    private static class HttpRequestTextMapGetter implements TextMapGetter<HttpRequest> {
        @Override
        public Iterable<String> keys(final HttpRequest carrier) {
            return carrier.headers().names();
        }

        @Override
        public String get(final HttpRequest carrier, final String key) {
            if (carrier == null) {
                return null;
            }

            return carrier.headers().get(key);
        }
    }

    // TODO - Ideally this should use HttpSpanNameExtractor, but to keep the name without the slash we use our own.
    private static class ServerSpanNameExtractor implements SpanNameExtractor<HttpRequest> {
        private final HttpServerAttributesExtractor<HttpRequest, HttpResponse> serverAttributesExtractor;

        private ServerSpanNameExtractor(
                final HttpServerAttributesExtractor<HttpRequest, HttpResponse> serverAttributesExtractor) {
            this.serverAttributesExtractor = serverAttributesExtractor;
        }

        @Override
        public String extract(final HttpRequest httpRequest) {
            if (httpRequest instanceof HttpServerRequest) {
                String path = URI.create(httpRequest.uri()).getPath();
                if (path != null && path.length() > 1) {
                    return path.substring(1);
                } else {
                    return "HTTP " + httpRequest.method();
                }
            }
            return null;
        }

        static SpanNameExtractor<HttpRequest> create(
                HttpServerAttributesExtractor<HttpRequest, HttpResponse> serverAttributesExtractor) {
            return new ServerSpanNameExtractor(serverAttributesExtractor);
        }
    }

    private static class ServerAttributesExtractor extends HttpServerAttributesExtractor<HttpRequest, HttpResponse> {
        @Override
        protected String flavor(final HttpRequest request) {
            if (request instanceof HttpServerRequest) {
                HttpServerRequest serverRequest = (HttpServerRequest) request;
                switch (serverRequest.version()) {
                    case HTTP_1_0:
                        return "1.0";
                    case HTTP_1_1:
                        return "1.1";
                    case HTTP_2:
                        return "2.0";
                    default:
                        return null;
                }
            }
            return null;
        }

        @Override
        protected String target(final HttpRequest request) {
            return request.uri();
        }

        @Override
        protected String route(final HttpRequest request) {
            return request.uri().length() > 1 ? request.uri() : null;
        }

        @Override
        protected String scheme(final HttpRequest request) {
            if (request instanceof HttpServerRequest) {
                return ((HttpServerRequest) request).scheme();
            }
            return null;
        }

        @Override
        protected String serverName(final HttpRequest request, final HttpResponse response) {
            return request.remoteAddress().hostName();
        }

        @Override
        protected String method(final HttpRequest request) {
            return request.method().name();
        }

        @Override
        protected List<String> requestHeader(final HttpRequest request, final String name) {
            return request.headers().getAll(name);
        }

        @Override
        protected Long requestContentLength(final HttpRequest request, final HttpResponse response) {
            return null;
        }

        @Override
        protected Long requestContentLengthUncompressed(final HttpRequest request, final HttpResponse response) {
            return null;
        }

        @Override
        protected Integer statusCode(final HttpRequest request, final HttpResponse response) {
            return response.statusCode();
        }

        @Override
        protected Long responseContentLength(final HttpRequest request, final HttpResponse response) {
            return null;
        }

        @Override
        protected Long responseContentLengthUncompressed(final HttpRequest request, final HttpResponse response) {
            return null;
        }

        @Override
        protected List<String> responseHeader(final HttpRequest request, final HttpResponse response, final String name) {
            return response.headers().getAll(name);
        }
    }

    private static class AdditionalServerAttributesExtractor implements AttributesExtractor<HttpRequest, HttpResponse> {
        @Override
        public void onStart(
                final AttributesBuilder attributes,
                final HttpRequest httpRequest) {

            if (httpRequest instanceof HttpServerRequest) {
                set(attributes, HTTP_CLIENT_IP, VertxUtil.extractClientIP((HttpServerRequest) httpRequest));
            }
        }

        @Override
        public void onEnd(
                final AttributesBuilder attributes,
                final HttpRequest httpRequest,
                final HttpResponse httpResponse,
                final Throwable error) {

            // The UrlPathTemplate is only added to the Vert.x context after the instrumenter start
            if (httpRequest instanceof SpanRequest) {
                SpanRequest spanRequest = (SpanRequest) httpRequest;
                // RESTEasy
                String route = spanRequest.getSpanOperation().getContext().getLocal("UrlPathTemplate");
                if (route == null) {
                    // Vert.x
                    route = spanRequest.getSpanOperation().getContext().getLocal("VertxRoute");
                }

                if (route != null && route.length() > 1) {
                    set(attributes, HTTP_ROUTE, route);
                    Span span = Span.fromContext(spanRequest.getSpanOperation().getSpanContext());
                    span.updateName(route.substring(1));
                }
            }
        }
    }

    private static class HttpRequestTextMapSetter implements TextMapSetter<HttpRequest> {
        @Override
        public void set(final HttpRequest carrier, final String key, final String value) {
            if (carrier != null) {
                carrier.headers().set(key, value);
            }
        }
    }

    static class WriteHeadersHttpRequest implements HttpRequest {
        private final HttpRequest httpRequest;
        private final BiConsumer<String, String> headers;

        WriteHeadersHttpRequest(final HttpRequest httpRequest, final BiConsumer<String, String> headers) {
            this.httpRequest = httpRequest;
            this.headers = headers;
        }

        @Override
        public int id() {
            return httpRequest.id();
        }

        @Override
        public String uri() {
            return httpRequest.uri();
        }

        @Override
        public String absoluteURI() {
            return httpRequest.absoluteURI();
        }

        @Override
        public HttpMethod method() {
            return httpRequest.method();
        }

        @Override
        public MultiMap headers() {
            HeadersAdaptor headers = new HeadersAdaptor(new HeadersMultiMap()) {
                @Override
                public MultiMap set(final String name, final String value) {
                    MultiMap result = super.set(name, value);
                    WriteHeadersHttpRequest.this.headers.accept(name, value);
                    return result;
                }
            };
            return headers.addAll(httpRequest.headers());
        }

        @Override
        public SocketAddress remoteAddress() {
            return httpRequest.remoteAddress();
        }

        static WriteHeadersHttpRequest request(HttpRequest httpRequest, BiConsumer<String, String> headers) {
            return new WriteHeadersHttpRequest(httpRequest, headers);
        }
    }

    private static class ClientAttributesExtractor extends HttpClientAttributesExtractor<HttpRequest, HttpResponse> {
        @Override
        protected String url(final HttpRequest request) {
            return request.absoluteURI();
        }

        @Override
        protected String flavor(final HttpRequest request, final HttpResponse response) {
            return null;
        }

        @Override
        protected String method(final HttpRequest request) {
            return request.method().name();
        }

        @Override
        protected List<String> requestHeader(final HttpRequest request, final String name) {
            return request.headers().getAll(name);
        }

        @Override
        protected Long requestContentLength(final HttpRequest request, final HttpResponse response) {
            return null;
        }

        @Override
        protected Long requestContentLengthUncompressed(final HttpRequest request, final HttpResponse response) {
            return null;
        }

        @Override
        protected Integer statusCode(final HttpRequest request, final HttpResponse response) {
            return response.statusCode();
        }

        @Override
        protected Long responseContentLength(final HttpRequest request, final HttpResponse response) {
            return null;
        }

        @Override
        protected Long responseContentLengthUncompressed(
                final HttpRequest request, final HttpResponse response) {
            return null;
        }

        @Override
        protected List<String> responseHeader(final HttpRequest request, final HttpResponse response, final String name) {
            return response.headers().getAll(name);
        }
    }
}
