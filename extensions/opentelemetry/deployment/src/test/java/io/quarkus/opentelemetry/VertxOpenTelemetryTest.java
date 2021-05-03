package io.quarkus.opentelemetry;

import static io.opentelemetry.api.common.AttributeKey.stringKey;
import static io.opentelemetry.semconv.trace.attributes.SemanticAttributes.HTTP_CLIENT_IP;
import static io.opentelemetry.semconv.trace.attributes.SemanticAttributes.HTTP_FLAVOR;
import static io.opentelemetry.semconv.trace.attributes.SemanticAttributes.HTTP_HOST;
import static io.opentelemetry.semconv.trace.attributes.SemanticAttributes.HTTP_SCHEME;
import static io.opentelemetry.semconv.trace.attributes.SemanticAttributes.HTTP_STATUS_CODE;
import static io.opentelemetry.semconv.trace.attributes.SemanticAttributes.HTTP_TARGET;
import static io.opentelemetry.semconv.trace.attributes.SemanticAttributes.HTTP_USER_AGENT;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;
import javax.inject.Inject;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.sdk.common.CompletableResultCode;
import io.opentelemetry.sdk.trace.data.SpanData;
import io.opentelemetry.sdk.trace.export.SpanExporter;
import io.quarkus.runtime.StartupEvent;
import io.quarkus.test.QuarkusUnitTest;
import io.restassured.RestAssured;
import io.vertx.ext.web.Router;

public class VertxOpenTelemetryTest {
    @RegisterExtension
    static final QuarkusUnitTest unitTest = new QuarkusUnitTest()
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class));

    @Inject
    MyExporter myExporter;

    @Test
    void trace() {
        RestAssured.when().get("/tracer").then()
                .statusCode(200)
                .body(is("Hello Tracer!"));

        List<SpanData> spans = myExporter.getFinishedSpanItems();

        assertEquals(2, spans.size());
        assertEquals("io.quarkus.vertx.opentelemetry", spans.get(0).getName());
        assertEquals("hello!", spans.get(0).getAttributes().get(stringKey("test.message")));
        assertEquals(200, spans.get(1).getAttributes().get(HTTP_STATUS_CODE));
        assertEquals("1.1", spans.get(1).getAttributes().get(HTTP_FLAVOR));
        assertEquals("/tracer", spans.get(1).getAttributes().get(HTTP_TARGET));
        assertEquals("http", spans.get(1).getAttributes().get(HTTP_SCHEME));
        assertEquals("localhost:8081", spans.get(1).getAttributes().get(HTTP_HOST));
        assertEquals("127.0.0.1", spans.get(1).getAttributes().get(HTTP_CLIENT_IP));
        assertNotNull(spans.get(1).getAttributes().get(HTTP_USER_AGENT));
    }

    @ApplicationScoped
    public static class TracerRouter {
        @Inject
        Router router;
        @Inject
        Tracer tracer;

        public void register(@Observes StartupEvent ev) {
            router.get("/tracer").handler(rc -> {
                tracer.spanBuilder("io.quarkus.vertx.opentelemetry").startSpan()
                        .setAttribute("test.message", "hello!")
                        .end();
                rc.response().end("Hello Tracer!");
            });
        }
    }

    @ApplicationScoped
    public static class MyExporter implements SpanExporter {
        private final List<SpanData> finishedSpanItems = new ArrayList<>();
        private boolean isStopped = false;

        public List<SpanData> getFinishedSpanItems() {
            synchronized (this) {
                return Collections.unmodifiableList(new ArrayList<>(finishedSpanItems));
            }
        }

        public void reset() {
            synchronized (this) {
                finishedSpanItems.clear();
            }
        }

        @Override
        public CompletableResultCode export(Collection<SpanData> spans) {
            synchronized (this) {
                if (isStopped) {
                    return CompletableResultCode.ofFailure();
                }
                finishedSpanItems.addAll(spans);
            }
            return CompletableResultCode.ofSuccess();
        }

        @Override
        public CompletableResultCode flush() {
            return CompletableResultCode.ofSuccess();
        }

        @Override
        public CompletableResultCode shutdown() {
            synchronized (this) {
                finishedSpanItems.clear();
                isStopped = true;
            }
            return CompletableResultCode.ofSuccess();
        }
    }
}
