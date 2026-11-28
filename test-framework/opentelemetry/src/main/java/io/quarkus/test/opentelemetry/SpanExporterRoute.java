package io.quarkus.test.opentelemetry;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

import jakarta.annotation.Priority;
import jakarta.enterprise.event.Observes;
import jakarta.enterprise.inject.Instance;
import jakarta.enterprise.inject.Produces;
import jakarta.enterprise.inject.Reserve;
import jakarta.inject.Singleton;

import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.sdk.OpenTelemetrySdk;
import io.opentelemetry.sdk.common.CompletableResultCode;
import io.opentelemetry.sdk.testing.exporter.InMemorySpanExporter;
import io.opentelemetry.sdk.trace.data.SpanData;
import io.quarkus.arc.ClientProxy;
import io.vertx.core.Context;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;

/**
 * Exposes the spans finished by the application under test, so that tests can fetch them with a
 * {@link SpanExporterClient}. This works
 * the same way in JVM mode, in native mode and for a packaged application, because the spans travel over HTTP.
 * <p>
 * {@code GET /test-otel/spans} returns the finished spans and {@code DELETE /test-otel/spans} forgets them. The spans of
 * the requests to these routes are left out.
 * <p>
 * The routes are plain Vert.x routes, so they do not depend on the REST stack the application uses.
 */
@Singleton
class SpanExporterRoute {

    static final String PATH = "/test-otel/spans";

    private static final AttributeKey<String> URL_PATH = AttributeKey.stringKey("url.path");

    @Produces
    @Singleton
    @Reserve
    @Priority(0)
    InMemorySpanExporter inMemorySpanExporter() {
        return InMemorySpanExporter.create();
    }

    void register(@Observes Router router, InMemorySpanExporter exporter, Instance<OpenTelemetry> openTelemetry) {
        register(router, exporter, new Supplier<>() {
            @Override
            public CompletableResultCode get() {
                // resolved for each request, as OpenTelemetry may not be ready when the router is
                if (openTelemetry.isResolvable()
                        && ClientProxy.unwrap(openTelemetry.get()) instanceof OpenTelemetrySdk sdk) {
                    return sdk.getSdkTracerProvider().forceFlush();
                }
                return CompletableResultCode.ofSuccess();
            }
        });
    }

    /**
     * @param flush hands the spans that are finished but still queued in a span processor over to the exporter. Both
     *        routes do it first: without it, a span finished before a reset would only reach the exporter after the
     *        reset, when the batch span processor gets to it, and show up among the spans of the next test.
     */
    static void register(Router router, InMemorySpanExporter exporter, Supplier<CompletableResultCode> flush) {
        router.get(PATH).handler(new Handler<>() {
            @Override
            public void handle(RoutingContext context) {
                afterFlush(flush, new Handler<>() {
                    @Override
                    public void handle(Void ignored) {
                        List<SpanData> spans = new ArrayList<>();
                        for (SpanData span : exporter.getFinishedSpanItems()) {
                            if (!isOwnRequest(span)) {
                                spans.add(span);
                            }
                        }
                        context.response()
                                .putHeader("Content-Type", "application/json")
                                .end(SpanWireFormat.encode(spans).encode());
                    }
                });
            }
        });
        router.delete(PATH).handler(new Handler<>() {
            @Override
            public void handle(RoutingContext context) {
                afterFlush(flush, new Handler<>() {
                    @Override
                    public void handle(Void ignored) {
                        exporter.reset();
                        context.response().setStatusCode(204).end();
                    }
                });
            }
        });
    }

    /**
     * Runs the action once the flush is over, back on the Vert.x context of the request: the flush completes on a
     * thread of the span processor, which is no place to answer a request from.
     */
    private static void afterFlush(Supplier<CompletableResultCode> flush, Handler<Void> action) {
        Context requestContext = Vertx.currentContext();
        flush.get().whenComplete(new Runnable() {
            @Override
            public void run() {
                requestContext.runOnContext(action);
            }
        });
    }

    private static boolean isOwnRequest(SpanData span) {
        if (span.getKind() != SpanKind.SERVER) {
            return false;
        }
        String path = span.getAttributes().get(URL_PATH);
        // the path is prefixed when the application has an HTTP root path
        return path != null && path.endsWith(PATH);
    }
}
