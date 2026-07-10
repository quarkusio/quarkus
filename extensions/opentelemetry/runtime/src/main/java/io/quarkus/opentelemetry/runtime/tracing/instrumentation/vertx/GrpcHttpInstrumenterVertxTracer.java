package io.quarkus.opentelemetry.runtime.tracing.instrumentation.vertx;

import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiConsumer;

import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.context.Scope;
import io.opentelemetry.context.propagation.TextMapGetter;
import io.opentelemetry.context.propagation.TextMapPropagator;
import io.opentelemetry.context.propagation.TextMapSetter;
import io.opentelemetry.instrumentation.api.instrumenter.Instrumenter;
import io.quarkus.opentelemetry.runtime.QuarkusContextStorage;
import io.smallrye.common.vertx.VertxContext;
import io.vertx.core.Context;
import io.vertx.core.MultiMap;
import io.vertx.core.http.impl.headers.HttpRequestHeaders;
import io.vertx.core.spi.observability.HttpRequest;
import io.vertx.core.spi.observability.HttpResponse;
import io.vertx.core.spi.tracing.SpanKind;
import io.vertx.core.spi.tracing.TagExtractor;
import io.vertx.core.tracing.TracingPolicy;

/**
 * Handles gRPC-over-HTTP requests at the Vert.x HTTP layer.
 * <p>
 * Suppresses HTTP span creation for gRPC requests — the gRPC interceptor creates the only span.
 * Extracts trace propagation context from HTTP headers and stores HTTP-level attributes
 * (scheme, client address, protocol version) in Vert.x context locals so the gRPC interceptor
 * can merge them into the gRPC span.
 * <p>
 * Must be registered before {@link HttpInstrumenterVertxTracer} so it matches first.
 */
public class GrpcHttpInstrumenterVertxTracer implements InstrumenterVertxTracer<HttpRequest, HttpResponse> {

    public static final String GRPC_HTTP_URL_SCHEME = "grpc.http.url.scheme";
    public static final String GRPC_HTTP_CLIENT_ADDRESS = "grpc.http.client.address";
    public static final String GRPC_HTTP_PROTOCOL_VERSION = "grpc.http.protocol.version";
    public static final String GRPC_HTTP_AUTHORITY = "grpc.http.authority";

    private final OpenTelemetry openTelemetry;

    public GrpcHttpInstrumenterVertxTracer(OpenTelemetry openTelemetry) {
        this.openTelemetry = openTelemetry;
    }

    @Override
    public <R> boolean canHandle(R request, TagExtractor<R> tagExtractor) {
        if (request instanceof HttpRequestHeaders multiMap) {
            String contentType = multiMap.get("content-type");
            return contentType != null && contentType.startsWith("application/grpc");
        }
        return false;
    }

    @Override
    public <R> OpenTelemetryVertxTracer.SpanOperation receiveRequest(
            Context context,
            SpanKind kind,
            TracingPolicy policy,
            R request,
            String operation,
            Iterable<java.util.Map.Entry<String, String>> headers,
            TagExtractor<R> tagExtractor) {

        if (TracingPolicy.IGNORE == policy) {
            return null;
        }

        if (!(request instanceof HttpRequestHeaders multiMap)) {
            return null;
        }

        io.opentelemetry.context.Context extractedContext = openTelemetry.getPropagators()
                .getTextMapPropagator()
                .extract(io.opentelemetry.context.Context.root(), multiMap, MultiMapTextMapGetter.INSTANCE);

        Scope scope = QuarkusContextStorage.INSTANCE.attach(context, extractedContext);

        storeHttpAttributes(context, request);

        return OpenTelemetryVertxTracer.SpanOperation.span(context, request, null, extractedContext, scope);
    }

    @Override
    public <R> void sendResponse(
            Context context,
            R response,
            OpenTelemetryVertxTracer.SpanOperation spanOperation,
            Throwable failure,
            TagExtractor<R> tagExtractor) {

        if (spanOperation == null) {
            return;
        }
        Scope scope = spanOperation.getScope();
        if (scope != null) {
            scope.close();
        }
    }

    @Override
    public <R> OpenTelemetryVertxTracer.SpanOperation sendRequest(
            Context context,
            SpanKind kind,
            TracingPolicy policy,
            R request,
            String operation,
            BiConsumer<String, String> headers,
            TagExtractor<R> tagExtractor) {

        if (TracingPolicy.IGNORE == policy) {
            return null;
        }

        io.opentelemetry.context.Context currentContext = QuarkusContextStorage.getOtelContext(context);
        if (currentContext == null) {
            currentContext = io.opentelemetry.context.Context.current();
        }
        if (currentContext != null && headers != null) {
            openTelemetry.getPropagators().getTextMapPropagator()
                    .inject(currentContext, headers, BiConsumerSetter.INSTANCE);
        }
        return null;
    }

    @Override
    public <R> void receiveResponse(
            Context context,
            R response,
            OpenTelemetryVertxTracer.SpanOperation spanOperation,
            Throwable failure,
            TagExtractor<R> tagExtractor) {
    }

    @Override
    public Instrumenter<HttpRequest, HttpResponse> getReceiveRequestInstrumenter() {
        return null;
    }

    @Override
    public Instrumenter<HttpRequest, HttpResponse> getSendResponseInstrumenter() {
        return null;
    }

    @Override
    public Instrumenter<HttpRequest, HttpResponse> getSendRequestInstrumenter() {
        return null;
    }

    @Override
    public Instrumenter<HttpRequest, HttpResponse> getReceiveResponseInstrumenter() {
        return null;
    }

    @Override
    public TextMapPropagator getPropagator() {
        return openTelemetry.getPropagators().getTextMapPropagator();
    }

    private void storeHttpAttributes(Context context, Object request) {
        if (!VertxContext.isDuplicatedContext(context)) {
            return;
        }
        if (request instanceof HttpRequestHeaders requestHeaders) {
            ConcurrentHashMap<String, Object> data = VertxContext.localContextData(context);
            if (requestHeaders.scheme() != null) {
                data.put(GRPC_HTTP_URL_SCHEME, requestHeaders.scheme());
            }
            if (requestHeaders.authority() != null) {
                data.put(GRPC_HTTP_AUTHORITY, requestHeaders.authority().toString());
            }
            data.put(GRPC_HTTP_PROTOCOL_VERSION, "2.0");
        }
    }

    @SuppressWarnings("rawtypes")
    private enum BiConsumerSetter implements TextMapSetter<BiConsumer> {
        INSTANCE;

        @Override
        @SuppressWarnings("unchecked")
        public void set(BiConsumer carrier, String key, String value) {
            if (carrier != null) {
                carrier.accept(key, value);
            }
        }
    }

    private enum MultiMapTextMapGetter implements TextMapGetter<MultiMap> {
        INSTANCE;

        @Override
        public Iterable<String> keys(MultiMap carrier) {
            return carrier.names();
        }

        @Override
        public String get(MultiMap carrier, String key) {
            if (carrier == null) {
                return null;
            }
            return carrier.get(key);
        }
    }

}
