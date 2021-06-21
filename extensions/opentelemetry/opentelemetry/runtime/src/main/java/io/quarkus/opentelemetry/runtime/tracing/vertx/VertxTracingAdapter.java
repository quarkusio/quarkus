package io.quarkus.opentelemetry.runtime.tracing.vertx;

import static io.opentelemetry.context.Context.current;
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
import io.opentelemetry.api.trace.SpanBuilder;
import io.opentelemetry.api.trace.StatusCode;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.context.Scope;
import io.opentelemetry.context.propagation.TextMapGetter;
import io.opentelemetry.context.propagation.TextMapPropagator;
import io.opentelemetry.context.propagation.TextMapSetter;
import io.opentelemetry.semconv.trace.attributes.SemanticAttributes;
import io.vertx.core.Context;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.core.http.HttpVersion;
import io.vertx.core.impl.ContextInternal;
import io.vertx.core.spi.VertxTracerFactory;
import io.vertx.core.spi.tracing.SpanKind;
import io.vertx.core.spi.tracing.TagExtractor;
import io.vertx.core.spi.tracing.VertxTracer;
import io.vertx.core.tracing.TracingOptions;
import io.vertx.core.tracing.TracingPolicy;

public class VertxTracingAdapter extends TracingOptions implements VertxTracer<Span, Span>, VertxTracerFactory {
    private static final String SCOPE_KEY = VertxTracingAdapter.class.getName() + ".scope";
    private static final String SPAN_KEY = VertxTracingAdapter.class.getName() + ".activeSpan";
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

        ((ContextInternal) context).dispatch(() -> {
            io.opentelemetry.context.Context currentContext = current();

            // Retrieve any incoming Span
            io.opentelemetry.context.Context propagatedContext = TEXT_MAP_PROPAGATOR.extract(currentContext, headers, GETTER);

            SpanBuilder builder;

            // Create new span
            builder = tracer.spanBuilder(operationName(request, operation))
                    .setParent(propagatedContext)
                    .setSpanKind(SpanKind.RPC.equals(kind) ? io.opentelemetry.api.trace.SpanKind.SERVER
                            : io.opentelemetry.api.trace.SpanKind.CONSUMER);

            //TODO - Figure out how to handle span name in a better way.
            if (request instanceof HttpServerRequest) {
                HttpServerRequest httpServerRequest = (HttpServerRequest) request;

                // Add attributes
                builder.setAttribute(HTTP_FLAVOR, convertHttpVersion(httpServerRequest.version()));
                builder.setAttribute(HTTP_METHOD, httpServerRequest.method().name());
                builder.setAttribute(HTTP_TARGET, httpServerRequest.path());
                builder.setAttribute(HTTP_SCHEME, httpServerRequest.scheme());
                builder.setAttribute(HTTP_HOST, httpServerRequest.host());
                builder.setAttribute(HTTP_CLIENT_IP, extractClientIP(httpServerRequest));
                builder.setAttribute(HTTP_USER_AGENT, httpServerRequest.getHeader(USER_AGENT));

                String contentLength = httpServerRequest.getHeader(CONTENT_LENGTH);
                if (contentLength != null && contentLength.length() > 0 && Long.parseLong(contentLength) > 0) {
                    builder.setAttribute(SemanticAttributes.HTTP_REQUEST_CONTENT_LENGTH, Long.valueOf(contentLength));
                } else {
                    builder.setAttribute(SemanticAttributes.HTTP_REQUEST_CONTENT_LENGTH, httpServerRequest.bytesRead());
                }
            }

            final Span currentSpan = builder.startSpan();
            context.putLocal(SPAN_KEY, currentSpan);
            context.putLocal(SCOPE_KEY, currentSpan.makeCurrent());
        });

        return context.getLocal(SPAN_KEY);
    }

    private <R> String operationName(R request, String operationName) {
        if (request instanceof HttpServerRequest) {
            return ((HttpServerRequest) request).uri().substring(1);
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

        context.removeLocal(SPAN_KEY);

        if (span == null) {
            return;
        }

        // Update Span name if parameterized path present
        String pathTemplate = context.getLocal("UrlPathTemplate");
        if (pathTemplate != null && !pathTemplate.isEmpty()) {
            span.updateName(pathTemplate.substring(1));
            span.setAttribute(HTTP_ROUTE, pathTemplate);
        }

        ((ContextInternal) context).dispatch(() -> {
            if (failure != null) {
                span.setStatus(StatusCode.ERROR);
                span.recordException(failure);
            }

            if (response != null) {
                if (response instanceof HttpServerResponse) {
                    HttpServerResponse httpServerResponse = (HttpServerResponse) response;
                    span.setAttribute(HTTP_STATUS_CODE, httpServerResponse.getStatusCode());
                }
            }

            span.end();

            Scope spanScope = context.getLocal(SCOPE_KEY);
            if (spanScope != null) {
                spanScope.close();
                context.removeLocal(SCOPE_KEY);
            }
        });
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

        if (policy.equals(TracingPolicy.IGNORE)) {
            /*
             * SmallRye Reactive Messaging with Kafka is responsible for creating spans for outgoing messages.
             * In this SPI call there is no way to know whether it has happened.
             * The current approach to prevent duplicate spans is for the Kafka client in SmallRye Reactive Messaging
             * to set TracingPolicy.IGNORE
             * Disabling tracing in Quarkus will not be done with TracingPolicy.IGNORE,
             * allowing it to be used for this purpose.
             */
            return null;
        }

        ((ContextInternal) context).dispatch(() -> {
            // Create new span
            SpanBuilder builder = tracer.spanBuilder(operationName(request, operation))
                    .setSpanKind(SpanKind.RPC.equals(kind)
                            ? io.opentelemetry.api.trace.SpanKind.CLIENT
                            : io.opentelemetry.api.trace.SpanKind.PRODUCER);

            if (request instanceof HttpServerRequest) {
                HttpServerRequest httpServerRequest = (HttpServerRequest) request;

                // Add attributes
                builder.setAttribute(HTTP_METHOD, httpServerRequest.method().name());
                builder.setAttribute(HTTP_URL, httpServerRequest.uri());
            }

            final Span outgoingSpan = builder.startSpan();
            TEXT_MAP_PROPAGATOR.inject(io.opentelemetry.context.Context.current().with(outgoingSpan), headers, SETTER);
            context.putLocal(SPAN_KEY, outgoingSpan);
        });

        return context.getLocal(SPAN_KEY);
    }

    @Override
    public <R> void receiveResponse(
            final Context context,
            final R response,
            final Span span,
            final Throwable failure,
            final TagExtractor<R> tagExtractor) {

        context.removeLocal(SPAN_KEY);

        if (span == null) {
            return;
        }

        ((ContextInternal) context).dispatch(() -> {
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
        });
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
