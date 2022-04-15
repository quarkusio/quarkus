package io.quarkus.opentelemetry.runtime.tracing.vertx;

import static io.opentelemetry.instrumentation.api.instrumenter.http.HttpRouteSource.FILTER;
import static io.opentelemetry.semconv.trace.attributes.SemanticAttributes.HTTP_CLIENT_IP;
import static io.quarkus.opentelemetry.runtime.OpenTelemetryConfig.INSTRUMENTATION_NAME;

import java.util.List;
import java.util.function.BiConsumer;

import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.common.AttributesBuilder;
import io.opentelemetry.context.Scope;
import io.opentelemetry.context.propagation.TextMapGetter;
import io.opentelemetry.context.propagation.TextMapSetter;
import io.opentelemetry.instrumentation.api.instrumenter.AttributesExtractor;
import io.opentelemetry.instrumentation.api.instrumenter.Instrumenter;
import io.opentelemetry.instrumentation.api.instrumenter.InstrumenterBuilder;
import io.opentelemetry.instrumentation.api.instrumenter.http.HttpClientAttributesExtractor;
import io.opentelemetry.instrumentation.api.instrumenter.http.HttpClientAttributesGetter;
import io.opentelemetry.instrumentation.api.instrumenter.http.HttpRouteGetter;
import io.opentelemetry.instrumentation.api.instrumenter.http.HttpRouteHolder;
import io.opentelemetry.instrumentation.api.instrumenter.http.HttpServerAttributesExtractor;
import io.opentelemetry.instrumentation.api.instrumenter.http.HttpServerAttributesGetter;
import io.opentelemetry.instrumentation.api.instrumenter.http.HttpSpanNameExtractor;
import io.opentelemetry.instrumentation.api.instrumenter.http.HttpSpanStatusExtractor;
import io.vertx.core.Context;
import io.vertx.core.MultiMap;
import io.vertx.core.http.HttpHeaders;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.http.impl.headers.HeadersAdaptor;
import io.vertx.core.http.impl.headers.HeadersMultiMap;
import io.vertx.core.net.SocketAddress;
import io.vertx.core.spi.observability.HttpRequest;
import io.vertx.core.spi.observability.HttpResponse;
import io.vertx.core.spi.tracing.TagExtractor;

class HttpInstrumenterVertxTracer implements InstrumenterVertxTracer<HttpRequest, HttpResponse> {
    private final Instrumenter<HttpRequest, HttpResponse> serverInstrumenter;
    private final Instrumenter<HttpRequest, HttpResponse> clientInstrumenter;

    public HttpInstrumenterVertxTracer(final OpenTelemetry openTelemetry) {
        serverInstrumenter = getServerInstrumenter(openTelemetry);
        clientInstrumenter = getClientInstrumenter(openTelemetry);
    }

    @Override
    public <R> boolean canHandle(final R request, final TagExtractor<R> tagExtractor) {
        return request instanceof HttpRequest;
    }

    @Override
    public Instrumenter<HttpRequest, HttpResponse> getReceiveRequestInstrumenter() {
        return serverInstrumenter;
    }

    @Override
    public Instrumenter<HttpRequest, HttpResponse> getSendResponseInstrumenter() {
        return serverInstrumenter;
    }

    @Override
    public Instrumenter<HttpRequest, HttpResponse> getSendRequestInstrumenter() {
        return clientInstrumenter;
    }

    @Override
    public Instrumenter<HttpRequest, HttpResponse> getReceiveResponseInstrumenter() {
        return clientInstrumenter;
    }

    @Override
    public OpenTelemetryVertxTracer.SpanOperation spanOperation(
            final Context context,
            final HttpRequest request,
            final MultiMap headers,
            final io.opentelemetry.context.Context spanContext,
            final Scope scope) {
        HttpRequestSpan requestSpan = HttpRequestSpan.request(request, headers, context, spanContext);
        return OpenTelemetryVertxTracer.SpanOperation.span(context, requestSpan, headers, spanContext, scope);
    }

    @Override
    public <R> void sendResponse(
            final Context context,
            final R response,
            final OpenTelemetryVertxTracer.SpanOperation spanOperation,
            final Throwable failure,
            final TagExtractor<R> tagExtractor) {

        HttpRouteHolder.updateHttpRoute(spanOperation.getSpanContext(), FILTER, RouteGetter.ROUTE_GETTER,
                ((HttpRequestSpan) spanOperation.getRequest()));
        InstrumenterVertxTracer.super.sendResponse(context, response, spanOperation, failure, tagExtractor);
    }

    @Override
    public HttpRequest writableHeaders(
            final HttpRequest request, final BiConsumer<String, String> headers) {
        return WriteHeadersHttpRequest.request(request, headers);
    }

    private static Instrumenter<HttpRequest, HttpResponse> getServerInstrumenter(final OpenTelemetry openTelemetry) {
        ServerAttributesExtractor serverAttributesExtractor = new ServerAttributesExtractor();

        InstrumenterBuilder<HttpRequest, HttpResponse> serverBuilder = Instrumenter.builder(
                openTelemetry,
                INSTRUMENTATION_NAME,
                HttpSpanNameExtractor.create(serverAttributesExtractor));

        return serverBuilder
                .setSpanStatusExtractor(HttpSpanStatusExtractor.create(serverAttributesExtractor))
                .addAttributesExtractor(HttpServerAttributesExtractor.create(serverAttributesExtractor))
                .addAttributesExtractor(new AdditionalServerAttributesExtractor())
                .addContextCustomizer(HttpRouteHolder.get())
                .newServerInstrumenter(new HttpRequestTextMapGetter());
    }

    private static Instrumenter<HttpRequest, HttpResponse> getClientInstrumenter(final OpenTelemetry openTelemetry) {
        ServerAttributesExtractor serverAttributesExtractor = new ServerAttributesExtractor();
        ClientAttributesExtractor clientAttributesExtractor = new ClientAttributesExtractor();

        InstrumenterBuilder<HttpRequest, HttpResponse> clientBuilder = Instrumenter.builder(
                openTelemetry,
                INSTRUMENTATION_NAME,
                HttpSpanNameExtractor.create(clientAttributesExtractor));

        return clientBuilder
                .setSpanStatusExtractor(HttpSpanStatusExtractor.create(serverAttributesExtractor))
                .addAttributesExtractor(HttpClientAttributesExtractor.create(clientAttributesExtractor))
                .newClientInstrumenter(new HttpRequestTextMapSetter());
    }

    private static class RouteGetter implements HttpRouteGetter<HttpRequestSpan> {
        static final RouteGetter ROUTE_GETTER = new RouteGetter();

        @Override
        public String get(final io.opentelemetry.context.Context context, final HttpRequestSpan requestSpan) {
            // RESTEasy
            String route = requestSpan.getContext().getLocal("UrlPathTemplate");
            if (route == null) {
                // Vert.x
                route = requestSpan.getContext().getLocal("VertxRoute");
            }

            if (route != null && route.length() > 1) {
                return route;
            }

            return null;
        }
    }

    private static class ServerAttributesExtractor implements HttpServerAttributesGetter<HttpRequest, HttpResponse> {
        @Override
        public String flavor(final HttpRequest request) {
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
        public String target(final HttpRequest request) {
            return request.uri();
        }

        @Override
        public String route(final HttpRequest request) {
            return request.uri().length() > 1 ? request.uri() : null;
        }

        @Override
        public String scheme(final HttpRequest request) {
            if (request instanceof HttpServerRequest) {
                return ((HttpServerRequest) request).scheme();
            }
            return null;
        }

        @Override
        public String serverName(final HttpRequest request) {
            return request.remoteAddress().hostName();
        }

        @Override
        public String method(final HttpRequest request) {
            return request.method().name();
        }

        @Override
        public List<String> requestHeader(final HttpRequest request, final String name) {
            return request.headers().getAll(name);
        }

        @Override
        public Long requestContentLength(final HttpRequest request, final HttpResponse response) {
            return getContentLength(request.headers());
        }

        @Override
        public Long requestContentLengthUncompressed(final HttpRequest request, final HttpResponse response) {
            return null;
        }

        @Override
        public Integer statusCode(final HttpRequest request, final HttpResponse response) {
            return response.statusCode();
        }

        @Override
        public Long responseContentLength(final HttpRequest request, final HttpResponse response) {
            return getContentLength(response.headers());
        }

        @Override
        public Long responseContentLengthUncompressed(final HttpRequest request, final HttpResponse response) {
            return null;
        }

        @Override
        public List<String> responseHeader(final HttpRequest request, final HttpResponse response, final String name) {
            return response.headers().getAll(name);
        }

        private static Long getContentLength(final MultiMap headers) {
            String contentLength = headers.get(HttpHeaders.CONTENT_LENGTH);
            if (contentLength != null && contentLength.length() > 0) {
                try {
                    return Long.valueOf(contentLength);
                } catch (NumberFormatException e) {
                    return null;
                }
            } else {
                return null;
            }
        }
    }

    private static class AdditionalServerAttributesExtractor implements AttributesExtractor<HttpRequest, HttpResponse> {
        @Override
        public void onStart(
                final AttributesBuilder attributes,
                final io.opentelemetry.context.Context parentContext,
                final HttpRequest httpRequest) {

            if (httpRequest instanceof HttpServerRequest) {
                set(attributes, HTTP_CLIENT_IP, VertxUtil.extractClientIP((HttpServerRequest) httpRequest));
            }
        }

        @Override
        public void onEnd(
                final AttributesBuilder attributes,
                final io.opentelemetry.context.Context context,
                final HttpRequest httpRequest,
                final HttpResponse httpResponse,
                final Throwable error) {

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

    private static class ClientAttributesExtractor implements HttpClientAttributesGetter<HttpRequest, HttpResponse> {
        @Override
        public String url(final HttpRequest request) {
            return request.absoluteURI();
        }

        @Override
        public String flavor(final HttpRequest request, final HttpResponse response) {
            return null;
        }

        @Override
        public String method(final HttpRequest request) {
            return request.method().name();
        }

        @Override
        public List<String> requestHeader(final HttpRequest request, final String name) {
            return request.headers().getAll(name);
        }

        @Override
        public Long requestContentLength(final HttpRequest request, final HttpResponse response) {
            return null;
        }

        @Override
        public Long requestContentLengthUncompressed(final HttpRequest request, final HttpResponse response) {
            return null;
        }

        @Override
        public Integer statusCode(final HttpRequest request, final HttpResponse response) {
            return response.statusCode();
        }

        @Override
        public Long responseContentLength(final HttpRequest request, final HttpResponse response) {
            return null;
        }

        @Override
        public Long responseContentLengthUncompressed(
                final HttpRequest request, final HttpResponse response) {
            return null;
        }

        @Override
        public List<String> responseHeader(final HttpRequest request, final HttpResponse response, final String name) {
            return response.headers().getAll(name);
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

    static class HttpRequestSpan implements HttpRequest {
        private final HttpRequest httpRequest;
        private final MultiMap headers;
        private final Context context;
        private final io.opentelemetry.context.Context spanContext;

        HttpRequestSpan(
                final HttpRequest httpRequest,
                final MultiMap headers,
                final Context context,
                final io.opentelemetry.context.Context spanContext) {

            this.httpRequest = httpRequest;
            this.headers = headers;
            this.context = context;
            this.spanContext = spanContext;
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
            return headers;
        }

        @Override
        public SocketAddress remoteAddress() {
            return httpRequest.remoteAddress();
        }

        public Context getContext() {
            if (context == null) {
                throw new IllegalStateException("The Vert.x Context is not set");
            }
            return context;
        }

        public io.opentelemetry.context.Context getSpanContext() {
            if (spanContext == null) {
                throw new IllegalStateException("The OpenTelemetry Context is not set");
            }
            return spanContext;
        }

        static HttpRequestSpan request(HttpRequest httpRequest, MultiMap headers, Context context,
                io.opentelemetry.context.Context spanContext) {
            return new HttpRequestSpan(httpRequest, headers, context, spanContext);
        }
    }

    private static class WriteHeadersHttpRequest implements HttpRequest {
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
}
