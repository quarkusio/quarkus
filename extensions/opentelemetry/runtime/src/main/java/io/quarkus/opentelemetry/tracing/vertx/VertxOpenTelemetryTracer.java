package io.quarkus.opentelemetry.tracing.vertx;

import static io.opentelemetry.context.Context.current;
import static io.opentelemetry.semconv.trace.attributes.SemanticAttributes.HTTP_CLIENT_IP;
import static io.opentelemetry.semconv.trace.attributes.SemanticAttributes.HTTP_FLAVOR;
import static io.opentelemetry.semconv.trace.attributes.SemanticAttributes.HTTP_HOST;
import static io.opentelemetry.semconv.trace.attributes.SemanticAttributes.HTTP_SCHEME;
import static io.opentelemetry.semconv.trace.attributes.SemanticAttributes.HTTP_STATUS_CODE;
import static io.opentelemetry.semconv.trace.attributes.SemanticAttributes.HTTP_TARGET;
import static io.opentelemetry.semconv.trace.attributes.SemanticAttributes.HTTP_USER_AGENT;
import static io.vertx.core.http.HttpHeaders.CONTENT_LENGTH;
import static io.vertx.core.http.HttpHeaders.USER_AGENT;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.BiConsumer;

import io.opentelemetry.api.GlobalOpenTelemetry;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanBuilder;
import io.opentelemetry.api.trace.StatusCode;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.context.propagation.TextMapGetter;
import io.opentelemetry.context.propagation.TextMapPropagator;
import io.opentelemetry.semconv.trace.attributes.SemanticAttributes;
import io.vertx.core.Context;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.core.http.HttpVersion;
import io.vertx.core.spi.tracing.SpanKind;
import io.vertx.core.spi.tracing.TagExtractor;
import io.vertx.core.spi.tracing.VertxTracer;
import io.vertx.core.tracing.TracingPolicy;

public class VertxOpenTelemetryTracer implements VertxTracer<Span, Span> {
    private final Tracer tracer;
    private final TextMapPropagator textMapPropagator;

    public VertxOpenTelemetryTracer() {
        tracer = GlobalOpenTelemetry.getTracer("io.quarkus.vertx.opentelemetry");
        textMapPropagator = GlobalOpenTelemetry.getPropagators().getTextMapPropagator();
        // eager init for ServiceLoader
        current();
    }

    @Override
    public <R> Span receiveRequest(
            final Context context,
            final SpanKind kind,
            final TracingPolicy policy,
            final R request,
            final String operation,
            final Iterable<Map.Entry<String, String>> headers,
            final TagExtractor<R> tagExtractor) {

        io.opentelemetry.context.Context parentContext = textMapPropagator.extract(current(), headers, GETTER);

        SpanBuilder builder;

        // TODO - Figure out how to handle span name in a better way.
        if (request instanceof HttpServerRequest) {
            HttpServerRequest httpServerRequest = (HttpServerRequest) request;
            builder = tracer.spanBuilder(httpServerRequest.uri().substring(1))
                    .setParent(parentContext)
                    .setSpanKind(io.opentelemetry.api.trace.SpanKind.SERVER);

            builder.setAttribute(HTTP_FLAVOR, convertHttpVersion(httpServerRequest.version()));
            builder.setAttribute(HTTP_TARGET, httpServerRequest.path());
            builder.setAttribute(HTTP_SCHEME, httpServerRequest.scheme());
            builder.setAttribute(HTTP_HOST, httpServerRequest.host());
            builder.setAttribute(HTTP_CLIENT_IP, httpServerRequest.remoteAddress().host());
            builder.setAttribute(HTTP_USER_AGENT, httpServerRequest.getHeader(USER_AGENT));

            String contentLength = httpServerRequest.getHeader(CONTENT_LENGTH);
            if (contentLength != null && contentLength.length() > 0 && Long.parseLong(contentLength) > 0) {
                builder.setAttribute(SemanticAttributes.HTTP_REQUEST_CONTENT_LENGTH, Long.valueOf(contentLength));
            } else {
                builder.setAttribute(SemanticAttributes.HTTP_REQUEST_CONTENT_LENGTH, httpServerRequest.bytesRead());
            }
        } else {
            builder = tracer.spanBuilder(operation)
                    .setParent(parentContext)
                    .setSpanKind(io.opentelemetry.api.trace.SpanKind.SERVER);
        }

        tagExtractor.extractTo(request, builder::setAttribute);

        return builder.startSpan();
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

        // Check io.vertx.core.http.impl.HttpUtils. Only includes http.status_code (as String)
        tagExtractor.extractTo(response, span::setAttribute);
        // TODO - Add to Vert.x Extractor?
        if (response instanceof HttpServerResponse) {
            HttpServerResponse httpServerResponse = (HttpServerResponse) response;
            // Override to use Number
            span.setAttribute(HTTP_STATUS_CODE, httpServerResponse.getStatusCode());
        }

        span.end();
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

        throw new UnsupportedOperationException();
    }

    @Override
    public <R> void receiveResponse(
            final Context context,
            final R response,
            final Span payload,
            final Throwable failure,
            final TagExtractor<R> tagExtractor) {

        throw new UnsupportedOperationException();
    }

    @Override
    public void close() {

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
                    return entry.getKey();
                }
            }

            return null;
        }
    };

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
}
