package io.quarkus.opentelemetry.deployment.instrumentation;

import static io.opentelemetry.api.common.AttributeKey.stringKey;
import static io.opentelemetry.api.trace.SpanKind.INTERNAL;
import static io.quarkus.opentelemetry.deployment.common.TestSpanExporter.getSpanByKindAndParentId;
import static java.net.HttpURLConnection.HTTP_OK;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;

import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.sdk.trace.data.SpanData;
import io.quarkus.opentelemetry.deployment.common.TestSpanExporter;
import io.quarkus.opentelemetry.deployment.common.TestSpanExporterProvider;
import io.quarkus.opentelemetry.deployment.common.TestUtil;
import io.quarkus.test.QuarkusUnitTest;
import io.quarkus.vertx.ConsumeEvent;
import io.restassured.RestAssured;
import io.vertx.core.eventbus.EventBus;
import io.vertx.ext.web.Router;

public class VertxEventBusInstrumentationDisabledTest {

    @RegisterExtension
    static final QuarkusUnitTest unitTest = new QuarkusUnitTest()
            .withApplicationRoot(root -> root
                    .addClasses(Events.class, TestUtil.class, TestSpanExporter.class, TestSpanExporterProvider.class)
                    .addAsResource(new StringAsset(TestSpanExporterProvider.class.getCanonicalName()),
                            "META-INF/services/io.opentelemetry.sdk.autoconfigure.spi.traces.ConfigurableSpanExporterProvider"))
            .overrideConfigKey("quarkus.otel.traces.exporter", "test-span-exporter")
            .overrideConfigKey("quarkus.otel.metrics.exporter", "none")
            .overrideConfigKey("quarkus.otel.logs.exporter", "none")
            .overrideConfigKey("quarkus.otel.bsp.schedule.delay", "200")
            .overrideConfigKey("quarkus.otel.instrument.vertx-event-bus", "false");

    @Inject
    TestSpanExporter spanExporter;

    @AfterEach
    void tearDown() {
        spanExporter.reset();
    }

    @Test
    void testTracingDisabled() throws Exception {

        RestAssured.when().get("/hello/event")
                .then()
                .statusCode(HTTP_OK)
                .body(equalTo("BAR"));

        // http request and dummy
        List<SpanData> spans = spanExporter.getFinishedSpanItems(2);
        assertEquals(2, spans.size());

        SpanData internal = getSpanByKindAndParentId(spans, INTERNAL, "0000000000000000");
        assertEquals("io.quarkus.vertx.opentelemetry", internal.getName());
        assertEquals("dummy", internal.getAttributes().get(stringKey("test.message")));
    }

    @Singleton
    public static class Events {

        @Inject
        Tracer tracer;

        @ConsumeEvent("foo")
        String echo(String foo) {
            tracer.spanBuilder("io.quarkus.vertx.opentelemetry").startSpan()
                    .setAttribute("test.message", "dummy")
                    .end();
            return foo.toUpperCase();
        }

        void registerRoutes(@Observes Router router, EventBus eventBus) {
            router.get("/hello/event").handler(rc -> {
                eventBus.request("foo", "bar").onComplete(r -> rc.end(r.result().body().toString()));
            });
        }
    }

}
