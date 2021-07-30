package io.quarkus.opentelemetry.runtime.tracing.vertx;

import static io.opentelemetry.semconv.trace.attributes.SemanticAttributes.HTTP_CLIENT_IP;
import static io.opentelemetry.semconv.trace.attributes.SemanticAttributes.HTTP_FLAVOR;
import static io.opentelemetry.semconv.trace.attributes.SemanticAttributes.HTTP_HOST;
import static io.opentelemetry.semconv.trace.attributes.SemanticAttributes.HTTP_METHOD;
import static io.opentelemetry.semconv.trace.attributes.SemanticAttributes.HTTP_ROUTE;
import static io.opentelemetry.semconv.trace.attributes.SemanticAttributes.HTTP_SCHEME;
import static io.opentelemetry.semconv.trace.attributes.SemanticAttributes.HTTP_STATUS_CODE;
import static io.opentelemetry.semconv.trace.attributes.SemanticAttributes.HTTP_TARGET;
import static io.opentelemetry.semconv.trace.attributes.SemanticAttributes.HTTP_URL;
import static io.opentelemetry.semconv.trace.attributes.SemanticAttributes.HTTP_USER_AGENT;
import static io.quarkus.opentelemetry.runtime.tracing.vertx.VertxUtil.extractClientIP;
import static io.vertx.core.http.HttpHeaders.CONTENT_LENGTH;
import static io.vertx.core.http.HttpHeaders.USER_AGENT;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.BiConsumer;

import javax.enterprise.inject.spi.CDI;

import io.opentelemetry.api.GlobalOpenTelemetry;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.StatusCode;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.context.Scope;
import io.opentelemetry.context.propagation.TextMapGetter;
import io.opentelemetry.context.propagation.TextMapPropagator;
import io.opentelemetry.context.propagation.TextMapSetter;
import io.opentelemetry.semconv.trace.attributes.SemanticAttributes;
import io.quarkus.opentelemetry.runtime.QuarkusContextStorage;
import io.vertx.core.Context;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.core.http.HttpVersion;
import io.vertx.core.http.impl.HttpRequestHead;
import io.vertx.core.spi.VertxTracerFactory;
import io.vertx.core.spi.tracing.SpanKind;
import io.vertx.core.spi.tracing.TagExtractor;
import io.vertx.core.spi.tracing.VertxTracer;
import io.vertx.core.tracing.TracingOptions;
import io.vertx.core.tracing.TracingPolicy;

public class VertxTracingAdapter extends TracingOptions implements VertxTracer<Span, Span>, VertxTracerFactory {
    private static final String RECEIVE_SCOPE_KEY = VertxTracingAdapter.class.getName() + ".scope.receive";
    private static final String SEND_SCOPE_KEY = VertxTracingAdapter.class.getName() + ".scope.send";
    private static TextMapPropagator TEXT_MAP_PROPAGATOR;

    private Tracer tracer;

    public void init() {
        tracer = CDI.current().select(Tracer.class).get();

        TEXT_MAP_PROPAGATOR = GlobalOpenTelemetry.getPropagators().getTextMapPropagator();
    }

    // TracingOptions overrides
    @Override
    public VertxTracerFactory getFactory() {
        return this;
    }

    // VertxTracerFactory overrides
    @Override
    public VertxTracer<Span, Span> tracer(final TracingOptions options) {
        return this;
    }

    // VertxTracer overrides
    @Override
    public <R> Span receiveRequest(
            final Context context,
            final SpanKind kind,
            final TracingPolicy policy,
            final R request,
            final String operation,
            final Iterable<Map.Entry<String, String>> headers,
            final TagExtractor<R> tagExtractor) {

        io.opentelemetry.context.Context openTelemetryContext = context.getLocal(QuarkusContextStorage.ACTIVE_CONTEXT);
        if (openTelemetryContext == null) {
            openTelemetryContext = io.opentelemetry.context.Context.root();
        }

        // Retrieve any incoming Span
        openTelemetryContext = TEXT_MAP_PROPAGATOR.extract(openTelemetryContext, headers, GETTER);

        // Create new span
        final Span currentSpan = tracer.spanBuilder(operationName(request, operation))
                .setParent(openTelemetryContext)
                .setSpanKind(SpanKind.RPC.equals(kind) ? io.opentelemetry.api.trace.SpanKind.SERVER
                        : io.opentelemetry.api.trace.SpanKind.CONSUMER)
                .startSpan();

        //TODO - Figure out how to handle span name in a better way.
        if (request instanceof HttpServerRequest) {
            HttpServerRequest httpServerRequest = (HttpServerRequest) request;

            // Add attributes
            currentSpan.setAttribute(HTTP_FLAVOR, convertHttpVersion(httpServerRequest.version()));
            currentSpan.setAttribute(HTTP_METHOD, operation);
            currentSpan.setAttribute(HTTP_TARGET, httpServerRequest.path());
            currentSpan.setAttribute(HTTP_SCHEME, httpServerRequest.scheme());
            currentSpan.setAttribute(HTTP_HOST, httpServerRequest.host());
            currentSpan.setAttribute(HTTP_CLIENT_IP, extractClientIP(httpServerRequest));
            currentSpan.setAttribute(HTTP_USER_AGENT, httpServerRequest.getHeader(USER_AGENT));

            String contentLength = httpServerRequest.getHeader(CONTENT_LENGTH);
            if (contentLength != null && contentLength.length() > 0 && Long.parseLong(contentLength) > 0) {
                currentSpan.setAttribute(SemanticAttributes.HTTP_REQUEST_CONTENT_LENGTH, Long.valueOf(contentLength));
            } else {
                currentSpan.setAttribute(SemanticAttributes.HTTP_REQUEST_CONTENT_LENGTH, httpServerRequest.bytesRead());
            }
        }

        openTelemetryContext = openTelemetryContext.with(currentSpan);
        context.putLocal(RECEIVE_SCOPE_KEY, QuarkusContextStorage.INSTANCE.attach(context, openTelemetryContext));

        return currentSpan;
    }

    private <R> String operationName(R request, String operationName) {
        if (request instanceof HttpServerRequest) {
            final String uri = ((HttpServerRequest) request).uri();
            if (uri.length() > 1) {
                return uri.substring(1);
            } else {
                return "HTTP " + operationName;
            }
        }
        return operationName;
    }

    @Override
    public <R> void sendResponse(
            final Context context,
            final R response,
            final Span span,
            final Throwable failure,
            final TagExtractor<R> tagExtractor) {

        if (span == null) {
            return;
        }

        if (failure != null) {
            span.setStatus(StatusCode.ERROR);
            span.recordException(failure);
        }

        if (response != null) {
            if (response instanceof HttpServerResponse) {
                HttpServerResponse httpServerResponse = (HttpServerResponse) response;
                span.setAttribute(HTTP_STATUS_CODE, httpServerResponse.getStatusCode());

                // Update Span name if parameterized path present
                String pathTemplate = context.getLocal("UrlPathTemplate");
                if (pathTemplate != null && pathTemplate.length() > 1) {
                    span.updateName(pathTemplate.substring(1));
                    span.setAttribute(HTTP_ROUTE, pathTemplate);
                }
            }
        }

        span.end();

        final Scope spanScope = context.getLocal(RECEIVE_SCOPE_KEY);
        if (spanScope != null) {
            spanScope.close();
            context.removeLocal(RECEIVE_SCOPE_KEY);
        }
    }

    @Override
    public <R> Span sendRequest(
            final Context context,
            final SpanKind kind,
            final TracingPolicy policy,
            final R request,
            final String operation,
            final BiConsumer<String, String> headers,
            final TagExtractor<R> tagExtractor) {

        io.opentelemetry.context.Context openTelemetryContext = context.getLocal(QuarkusContextStorage.ACTIVE_CONTEXT);
        if (openTelemetryContext == null) {
            openTelemetryContext = io.opentelemetry.context.Context.root();
        }

        if (request instanceof HttpRequestHead) {
            HttpRequestHead requestHead = (HttpRequestHead) request;
            if (requestHead.headers().contains("traceparent")) {
                // Don't create a new span if we've already got one present for the outgoing request
                return null;
            }
        }

        // Create new span
        final Span outgoingSpan = tracer.spanBuilder(operationName(request, operation))
                .setParent(openTelemetryContext)
                .setSpanKind(SpanKind.RPC.equals(kind)
                        ? io.opentelemetry.api.trace.SpanKind.CLIENT
                        : io.opentelemetry.api.trace.SpanKind.PRODUCER)
                .startSpan();

        if (request instanceof HttpServerRequest) {
            HttpServerRequest httpServerRequest = (HttpServerRequest) request;

            // Add attributes
            outgoingSpan.setAttribute(HTTP_METHOD, httpServerRequest.method().name());
            outgoingSpan.setAttribute(HTTP_URL, httpServerRequest.uri());
        }

        openTelemetryContext = openTelemetryContext.with(outgoingSpan);
        TEXT_MAP_PROPAGATOR.inject(openTelemetryContext, headers, SETTER);

        context.putLocal(SEND_SCOPE_KEY, QuarkusContextStorage.INSTANCE.attach(context, openTelemetryContext));

        return outgoingSpan;
    }

    @Override
    public <R> void receiveResponse(
            final Context context,
            final R response,
            final Span span,
            final Throwable failure,
            final TagExtractor<R> tagExtractor) {

        if (span == null) {
            return;
        }

        if (failure != null) {
            span.recordException(failure);
        }

        if (response != null) {
            if (response instanceof HttpServerResponse) {
                HttpServerResponse httpServerResponse = (HttpServerResponse) response;

                // Add attributes
                span.setAttribute(HTTP_STATUS_CODE, httpServerResponse.getStatusCode());
            }
        }

        span.end();

        Scope spanScope = context.getLocal(SEND_SCOPE_KEY);
        if (spanScope != null) {
            spanScope.close();
            context.removeLocal(SEND_SCOPE_KEY);
        }
    }

    private static String convertHttpVersion(HttpVersion version) {
        switch (version) {
            case HTTP_1_0:
                return "1.0";
            case HTTP_1_1:
                return "1.1";
            case HTTP_2:
                return "2.0";
            default:
                return "";
        }
    }

    public static final TextMapGetter<Iterable<Map.Entry<String, String>>> GETTER = new TextMapGetter<Iterable<Map.Entry<String, String>>>() {
        @Override
        public Iterable<String> keys(final Iterable<Map.Entry<String, String>> carrier) {
            final Set<String> keys = new HashSet<>();
            for (Map.Entry<String, String> entry : carrier) {
                keys.add(entry.getKey());
            }
            return keys;
        }

        @Override
        public String get(final Iterable<Map.Entry<String, String>> carrier, final String key) {
            if (carrier == null) {
                return null;
            }

            for (Map.Entry<String, String> entry : carrier) {
                if (entry.getKey().equalsIgnoreCase(key)) {
                    return entry.getValue();
                }
            }

            return null;
        }
    };

    public static final TextMapSetter<BiConsumer<String, String>> SETTER = new TextMapSetter<BiConsumer<String, String>>() {
        @Override
        public void set(BiConsumer<String, String> carrier, String key, String value) {
            if (carrier == null) {
                return;
            }
            carrier.accept(key, value);
        }
    };
}
