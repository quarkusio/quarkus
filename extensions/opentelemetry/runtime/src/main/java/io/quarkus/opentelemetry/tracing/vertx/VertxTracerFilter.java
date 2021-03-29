package io.quarkus.opentelemetry.tracing.vertx;

import java.util.Iterator;

import javax.enterprise.inject.Instance;
import javax.enterprise.inject.spi.CDI;

import io.opentelemetry.api.GlobalOpenTelemetry;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanBuilder;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.api.trace.StatusCode;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.context.Context;
import io.opentelemetry.context.Scope;
import io.opentelemetry.context.propagation.TextMapGetter;
import io.opentelemetry.context.propagation.TextMapPropagator;
import io.opentelemetry.semconv.trace.attributes.SemanticAttributes;
import io.quarkus.opentelemetry.QuarkusContextStorage;
import io.vertx.core.Handler;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.core.http.HttpVersion;
import io.vertx.ext.web.RoutingContext;

public class VertxTracerFilter implements Handler<RoutingContext> {
    private static final TextMapPropagator TEXT_MAP_PROPAGATOR = GlobalOpenTelemetry.getPropagators().getTextMapPropagator();

    Tracer tracer;
    boolean initialized = false;

    @Override
    public void handle(RoutingContext routingContext) {
        // Needed because CDI Request Scope not active yet
        QuarkusContextStorage.INSTANCE.setRoutingContext(routingContext);

        if (!initialized) {
            Instance<Tracer> tracerInstance = CDI.current().select(Tracer.class);
            if (tracerInstance.isResolvable()) {
                tracer = tracerInstance.get();
            }
            initialized = true;
        }

        // Don't do anything if there is no Tracer
        if (tracer != null) {
            // Retrieve any incoming Span
            Context parentContext = propagatedContext(routingContext);

            // Create new span
            SpanBuilder builder = tracer.spanBuilder(routingContext.request().uri().substring(1))
                    .setParent(parentContext)
                    .setSpanKind(SpanKind.SERVER);

            // Add attributes
            builder.setAttribute(SemanticAttributes.HTTP_FLAVOR, convertHttpVersion(routingContext.request().version()));
            builder.setAttribute(SemanticAttributes.HTTP_METHOD, routingContext.request().method().name());
            builder.setAttribute(SemanticAttributes.HTTP_TARGET, routingContext.request().path());
            builder.setAttribute(SemanticAttributes.HTTP_SCHEME, routingContext.request().scheme());
            builder.setAttribute(SemanticAttributes.HTTP_HOST, routingContext.request().host());
            builder.setAttribute(SemanticAttributes.HTTP_CLIENT_IP, routingContext.request().remoteAddress().host());
            builder.setAttribute(SemanticAttributes.HTTP_USER_AGENT, routingContext.request().getHeader("User-Agent"));

            String contentLength = routingContext.request().getHeader("Content-Length");
            if (contentLength != null && contentLength.length() > 0 && Long.parseLong(contentLength) > 0) {
                builder.setAttribute(SemanticAttributes.HTTP_REQUEST_CONTENT_LENGTH, Long.valueOf(contentLength));
            } else {
                builder.setAttribute(SemanticAttributes.HTTP_REQUEST_CONTENT_LENGTH, routingContext.request().bytesRead());
            }

            Span currentSpan = builder.startSpan();
            Scope spanScope = currentSpan.makeCurrent();

            // Add Handler to finish a Span
            HttpServerResponse response = routingContext.response();
            routingContext.addHeadersEndHandler(new Handler<Void>() {
                @Override
                public void handle(Void event) {
                    currentSpan.setAttribute(SemanticAttributes.HTTP_STATUS_CODE, response.getStatusCode());

                    if (routingContext.failed()) {
                        // Add exception to span
                        currentSpan.setStatus(StatusCode.ERROR);
                        currentSpan.recordException(routingContext.failure());
                    }

                    currentSpan.end();
                    spanScope.close();

                    QuarkusContextStorage.INSTANCE.clearRoutingContext(routingContext);
                }
            });
        }

        // Call next Handler
        routingContext.next();
    }

    private String convertHttpVersion(HttpVersion version) {
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

    private Context propagatedContext(RoutingContext routingContext) {
        // Extract trace from incoming request
        return TEXT_MAP_PROPAGATOR.extract(Context.current(), routingContext.request(), GETTER);
    }

    private static final TextMapGetter<HttpServerRequest> GETTER = new TextMapGetter<HttpServerRequest>() {
        @Override
        public Iterable<String> keys(HttpServerRequest carrier) {
            return new Iterable<String>() {
                @Override
                public Iterator<String> iterator() {
                    return carrier.headers().names().iterator();
                }
            };
        }

        @Override
        public String get(HttpServerRequest carrier, String key) {
            if (carrier != null) {
                return carrier.getHeader(key);
            }
            return null;
        }
    };
}
