package io.quarkus.opentelemetry.runtime.tracing.intrumentation.vertx;

import static io.opentelemetry.instrumentation.api.instrumenter.http.HttpServerRouteSource.SERVER_FILTER;
import static io.opentelemetry.semconv.SemanticAttributes.CLIENT_ADDRESS;
import static io.opentelemetry.semconv.SemanticAttributes.HTTP_CLIENT_IP;
import static io.quarkus.opentelemetry.runtime.config.build.OTelBuildConfig.INSTRUMENTATION_NAME;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Collections;
import java.util.List;
import java.util.function.BiConsumer;

import io.netty.handler.codec.http.HttpResponseStatus;
import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.common.AttributesBuilder;
import io.opentelemetry.context.Scope;
import io.opentelemetry.context.propagation.TextMapGetter;
import io.opentelemetry.context.propagation.TextMapSetter;
import io.opentelemetry.instrumentation.api.instrumenter.AttributesExtractor;
import io.opentelemetry.instrumentation.api.instrumenter.Instrumenter;
import io.opentelemetry.instrumentation.api.instrumenter.InstrumenterBuilder;
import io.opentelemetry.instrumentation.api.instrumenter.SpanNameExtractor;
import io.opentelemetry.instrumentation.api.instrumenter.http.HttpClientAttributesGetter;
import io.opentelemetry.instrumentation.api.instrumenter.http.HttpServerAttributesExtractor;
import io.opentelemetry.instrumentation.api.instrumenter.http.HttpServerAttributesGetter;
import io.opentelemetry.instrumentation.api.instrumenter.http.HttpServerRoute;
import io.opentelemetry.instrumentation.api.instrumenter.http.HttpServerRouteBiGetter;
import io.opentelemetry.instrumentation.api.instrumenter.http.HttpSpanNameExtractor;
import io.opentelemetry.instrumentation.api.instrumenter.http.HttpSpanStatusExtractor;
import io.opentelemetry.semconv.SemanticAttributes;
import io.quarkus.opentelemetry.runtime.config.runtime.SemconvStabilityType;
import io.vertx.core.Context;
import io.vertx.core.MultiMap;
import io.vertx.core.http.HttpHeaders;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.http.HttpVersion;
import io.vertx.core.http.impl.HttpRequestHead;
import io.vertx.core.http.impl.headers.HeadersAdaptor;
import io.vertx.core.http.impl.headers.HeadersMultiMap;
import io.vertx.core.net.SocketAddress;
import io.vertx.core.spi.observability.HttpRequest;
import io.vertx.core.spi.observability.HttpResponse;
import io.vertx.core.spi.tracing.TagExtractor;

public class HttpInstrumenterVertxTracer implements InstrumenterVertxTracer<HttpRequest, HttpResponse> {
    private final Instrumenter<HttpRequest, HttpResponse> serverInstrumenter;
    private final Instrumenter<HttpRequest, HttpResponse> clientInstrumenter;

    public HttpInstrumenterVertxTracer(final OpenTelemetry openTelemetry, final SemconvStabilityType semconvStability) {
        serverInstrumenter = getServerInstrumenter(openTelemetry, semconvStability);
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

        HttpServerRoute.update(spanOperation.getSpanContext(), SERVER_FILTER, RouteGetter.ROUTE_GETTER,
                ((HttpRequestSpan) spanOperation.getRequest()), (HttpResponse) response);
        InstrumenterVertxTracer.super.sendResponse(context, response, spanOperation, failure, tagExtractor);
    }

    @Override
    public HttpRequest writableHeaders(
            final HttpRequest request, final BiConsumer<String, String> headers) {
        return WriteHeadersHttpRequest.request(request, headers);
    }

    private static Instrumenter<HttpRequest, HttpResponse> getServerInstrumenter(final OpenTelemetry openTelemetry,
            final SemconvStabilityType semconvStability) {
        ServerAttributesExtractor serverAttributesExtractor = new ServerAttributesExtractor();

        InstrumenterBuilder<HttpRequest, HttpResponse> serverBuilder = Instrumenter.builder(
                openTelemetry,
                INSTRUMENTATION_NAME,
                HttpSpanNameExtractor.create(serverAttributesExtractor));

        return serverBuilder
                .setSpanStatusExtractor(HttpSpanStatusExtractor.create(serverAttributesExtractor))
                .addAttributesExtractor(
                        HttpServerAttributesExtractor.create(serverAttributesExtractor))
                .addAttributesExtractor(new AdditionalServerAttributesExtractor(semconvStability))
                .addContextCustomizer(HttpServerRoute.create(serverAttributesExtractor))
                .buildServerInstrumenter(new HttpRequestTextMapGetter());
    }

    private static Instrumenter<HttpRequest, HttpResponse> getClientInstrumenter(final OpenTelemetry openTelemetry) {
        ServerAttributesExtractor serverAttributesExtractor = new ServerAttributesExtractor();
        HttpClientAttributesExtractor httpClientAttributesExtractor = new HttpClientAttributesExtractor();

        InstrumenterBuilder<HttpRequest, HttpResponse> clientBuilder = Instrumenter.builder(
                openTelemetry,
                INSTRUMENTATION_NAME,
                new ClientSpanNameExtractor(httpClientAttributesExtractor));

        return clientBuilder
                .setSpanStatusExtractor(HttpSpanStatusExtractor.create(serverAttributesExtractor))
                .addAttributesExtractor(
                        io.opentelemetry.instrumentation.api.instrumenter.http.HttpClientAttributesExtractor.create(
                                httpClientAttributesExtractor))
                .buildClientInstrumenter(new HttpRequestTextMapSetter());
    }

    private static class RouteGetter implements HttpServerRouteBiGetter<HttpRequestSpan, HttpResponse> {
        static final RouteGetter ROUTE_GETTER = new RouteGetter();

        @Override
        public String get(final io.opentelemetry.context.Context context, final HttpRequestSpan requestSpan,
                final HttpResponse response) {
            // RESTEasy
            String route = requestSpan.getContext().getLocal("UrlPathTemplate");
            if (route == null) {
                // Vert.x
                route = requestSpan.getContext().getLocal("VertxRoute");
            }

            if (route != null && route.length() >= 1) {
                return route;
            }

            if (response != null && HttpResponseStatus.NOT_FOUND.code() == response.statusCode()) {
                return "/*";
            }

            return null;
        }
    }

    private static class ServerAttributesExtractor implements HttpServerAttributesGetter<HttpRequest, HttpResponse> {
        @Override
        public String getTransport(HttpRequest httpRequest) {
            return null;
        }

        @Override
        public String getServerAddress(HttpRequest httpRequest) {
            if (httpRequest instanceof HttpServerRequest) {
                return VertxUtil.extractRemoteHostname((HttpServerRequest) httpRequest);
            }
            return null;
        }

        @Override
        public Integer getServerPort(HttpRequest httpRequest) {
            if (httpRequest instanceof HttpServerRequest) {
                Long remoteHostPort = VertxUtil.extractRemoteHostPort((HttpServerRequest) httpRequest);
                if (remoteHostPort == null) {
                    return null;
                }
                return remoteHostPort.intValue();
            }
            return null;
        }

        @Override
        public String getNetworkProtocolName(HttpRequest request, HttpResponse response) {
            return "http";
        }

        @Override
        public String getNetworkProtocolVersion(HttpRequest request, HttpResponse response) {
            return getHttpVersion(request);
        }

        @Override
        public String getUrlPath(final HttpRequest request) {
            try {
                URI uri = new URI(request.uri());
                return uri.getPath();
            } catch (URISyntaxException e) {
                return null;
            }
        }

        @Override
        public String getUrlQuery(HttpRequest request) {
            try {
                URI uri = new URI(request.uri());
                return uri.getQuery();
            } catch (URISyntaxException e) {
                return null;
            }
        }

        @Override
        public String getHttpRoute(final HttpRequest request) {
            return null;
        }

        @Override
        public String getUrlScheme(final HttpRequest request) {
            if (request instanceof HttpServerRequest) {
                return ((HttpServerRequest) request).scheme();
            }
            return null;
        }

        @Override
        public String getHttpRequestMethod(final HttpRequest request) {
            return request.method().name();
        }

        @Override
        public List<String> getHttpRequestHeader(final HttpRequest request, final String name) {
            return request.headers().getAll(name);
        }

        @Override
        public Integer getHttpResponseStatusCode(HttpRequest httpRequest, HttpResponse httpResponse, Throwable error) {
            return httpResponse != null ? httpResponse.statusCode() : null;
        }

        @Override
        public List<String> getHttpResponseHeader(final HttpRequest request, final HttpResponse response, final String name) {
            return response != null ? response.headers().getAll(name) : Collections.emptyList();
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
        private final SemconvStabilityType semconvStability;

        public AdditionalServerAttributesExtractor(final SemconvStabilityType semconvStability) {
            this.semconvStability = semconvStability;
        }

        @Override
        public void onStart(
                final AttributesBuilder attributes,
                final io.opentelemetry.context.Context parentContext,
                final HttpRequest httpRequest) {

            if (httpRequest instanceof HttpServerRequest) {
                String clientIp = VertxUtil.extractClientIP((HttpServerRequest) httpRequest);
                if (clientIp != null) {
                    switch (semconvStability) {
                        case HTTP_OLD:
                            attributes.put(HTTP_CLIENT_IP, clientIp);
                            break;
                        case HTTP_DUP:
                            attributes.put(HTTP_CLIENT_IP, clientIp);
                            attributes.put(CLIENT_ADDRESS, clientIp);
                            break;
                        case HTTP:
                            attributes.put(CLIENT_ADDRESS, clientIp);
                            break;
                    }
                }
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

    private static class HttpClientAttributesExtractor implements HttpClientAttributesGetter<HttpRequest, HttpResponse> {
        @Override
        public String getUrlFull(final HttpRequest request) {
            return request.absoluteURI();
        }

        @Override
        public String getHttpRequestMethod(final HttpRequest request) {
            return request.method().name();
        }

        @Override
        public List<String> getHttpRequestHeader(final HttpRequest request, final String name) {
            return request.headers().getAll(name);
        }

        @Override
        public Integer getHttpResponseStatusCode(HttpRequest httpRequest, HttpResponse httpResponse, Throwable error) {
            return httpResponse.statusCode();
        }

        @Override
        public List<String> getHttpResponseHeader(final HttpRequest request, final HttpResponse response, final String name) {
            return response.headers().getAll(name);
        }

        @Override
        public String getTransport(HttpRequest httpClientRequest, HttpResponse httpClientResponse) {
            return SemanticAttributes.NetTransportValues.IP_TCP;
        }

        @Override
        public String getServerAddress(HttpRequest httpRequest) {
            return httpRequest.remoteAddress().hostName();
        }

        @Override
        public Integer getServerPort(HttpRequest httpRequest) {
            return httpRequest.remoteAddress().port();
        }

        @Override
        public String getNetworkProtocolName(final HttpRequest request, final HttpResponse response) {
            return "http";
        }

        @Override
        public String getNetworkProtocolVersion(final HttpRequest request, final HttpResponse response) {
            return getHttpVersion(request);
        }
    }

    private static class ClientSpanNameExtractor implements SpanNameExtractor<HttpRequest> {

        private final SpanNameExtractor<HttpRequest> http;

        ClientSpanNameExtractor(HttpClientAttributesExtractor clientAttributesExtractor) {
            this.http = HttpSpanNameExtractor.create(clientAttributesExtractor);
        }

        @Override
        public String extract(HttpRequest httpRequest) {
            if (httpRequest instanceof HttpRequestHead) {
                HttpRequestHead head = (HttpRequestHead) httpRequest;
                if (head.traceOperation != null) {
                    return head.traceOperation;
                }
            }
            if (httpRequest instanceof WriteHeadersHttpRequest) {
                WriteHeadersHttpRequest writeHeaders = (WriteHeadersHttpRequest) httpRequest;
                String traceOperation = writeHeaders.traceOperation();
                if (traceOperation != null) {
                    return traceOperation;
                }
            }
            return http.extract(httpRequest);
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

        public String traceOperation() {
            if (httpRequest instanceof HttpRequestHead) {
                return ((HttpRequestHead) httpRequest).traceOperation;
            }
            return null;
        }

        static WriteHeadersHttpRequest request(HttpRequest httpRequest, BiConsumer<String, String> headers) {
            return new WriteHeadersHttpRequest(httpRequest, headers);
        }
    }

    private static String getHttpVersion(HttpRequest request) {
        if (request instanceof HttpServerRequest) {
            HttpVersion version = ((HttpServerRequest) request).version();
            if (version != null) {
                switch (version) {
                    case HTTP_1_0:
                        return "1.0";
                    case HTTP_1_1:
                        return "1.1";
                    case HTTP_2:
                        return "2.0";
                    default:
                        // Will be executed once Vert.x supports other versions
                        // At that point version transformation will be needed for OTel semantics
                        return version.alpnName();
                }
            }
        }
        return null;
    }
}
