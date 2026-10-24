package io.quarkus.opentelemetry.deployment.instrumentation;

import static io.opentelemetry.api.common.AttributeKey.stringKey;
import static io.opentelemetry.api.trace.SpanKind.CONSUMER;
import static io.opentelemetry.api.trace.SpanKind.PRODUCER;
import static io.opentelemetry.api.trace.SpanKind.SERVER;
import static java.net.HttpURLConnection.HTTP_OK;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.sdk.trace.data.SpanData;
import io.quarkus.opentelemetry.deployment.common.TestUtil;
import io.quarkus.opentelemetry.deployment.common.exporter.TestSpanExporter;
import io.quarkus.opentelemetry.deployment.common.exporter.TestSpanExporterProvider;
import io.quarkus.test.QuarkusExtensionTest;
import io.quarkus.vertx.ConsumeEvent;
import io.restassured.RestAssured;
import io.vertx.core.eventbus.DeliveryOptions;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.tracing.TracingPolicy;
import io.vertx.ext.web.Router;

/**
 * The event bus tracer must honor the Vert.x {@link TracingPolicy}. The event bus carries intra-application
 * messages, so it defaults to {@code PROPAGATE}: a message sent while no trace is active must not open a trace
 * of its own, while one sent inside a trace must join it.
 *
 * @see <a href="https://github.com/quarkusio/quarkus/issues/25417">#25417</a>
 */
public class VertxEventBusTracingPolicyTest {

    private static final String PROPAGATE_ADDRESS = "propagate";
    private static final String ALWAYS_ADDRESS = "always";
    private static final String IGNORE_ADDRESS = "ignore";

    @RegisterExtension
    static final QuarkusExtensionTest unitTest = new QuarkusExtensionTest()
            .withApplicationRoot(root -> root
                    .addClasses(Events.class, TestUtil.class, TestSpanExporter.class, TestSpanExporterProvider.class)
                    .addAsResource(new StringAsset(TestSpanExporterProvider.class.getCanonicalName()),
                            "META-INF/services/io.opentelemetry.sdk.autoconfigure.spi.traces.ConfigurableSpanExporterProvider"))
            .overrideConfigKey("quarkus.otel.traces.exporter", "test-span-exporter")
            .overrideConfigKey("quarkus.otel.traces.sampler.arg", "1.0d")
            .overrideConfigKey("quarkus.otel.metrics.enabled", "false")
            .overrideConfigKey("quarkus.otel.logs.enabled", "false")
            .overrideConfigKey("quarkus.otel.bsp.schedule.delay", "200")
            .overrideConfigKey("quarkus.datasource.devservices.enabled", "false");

    @Inject
    TestSpanExporter spanExporter;

    @Inject
    EventBus eventBus;

    @AfterEach
    void tearDown() {
        spanExporter.reset();
    }

    /**
     * The regression from #25417: a message published outside any trace used to produce an orphan root trace.
     * An {@code ALWAYS} message is sent alongside as a sentinel, so the test waits on a deterministic span
     * count instead of asserting on the absence of spans.
     */
    @Test
    void propagateWithoutActiveTraceIsNotTraced() throws Exception {
        request(PROPAGATE_ADDRESS, TracingPolicy.PROPAGATE);
        request(ALWAYS_ADDRESS, TracingPolicy.ALWAYS);

        List<SpanData> spans = spanExporter.getFinishedSpanItems(2);

        assertThat(spans).extracting(VertxEventBusTracingPolicyTest::destination).containsOnly(ALWAYS_ADDRESS);
        assertThat(spans).extracting(SpanData::getKind).containsExactlyInAnyOrder(PRODUCER, CONSUMER);
    }

    /**
     * A message sent from inside a trace still has to be traced, and has to join the ongoing trace rather than
     * start a new one.
     */
    @Test
    void propagateWithinActiveTraceIsTraced() {
        RestAssured.when().get("/send/" + PROPAGATE_ADDRESS).then().statusCode(HTTP_OK);

        // SERVER for the HTTP request, plus the event bus PRODUCER and CONSUMER.
        List<SpanData> spans = spanExporter.getFinishedSpanItems(3);

        SpanData server = single(spans, SERVER);
        SpanData producer = single(spans, PRODUCER);
        SpanData consumer = single(spans, CONSUMER);

        assertThat(destination(producer)).isEqualTo(PROPAGATE_ADDRESS);
        assertThat(producer.getTraceId()).isEqualTo(server.getTraceId());
        assertThat(producer.getParentSpanId()).isEqualTo(server.getSpanId());

        assertThat(destination(consumer)).isEqualTo(PROPAGATE_ADDRESS);
        assertThat(consumer.getTraceId()).isEqualTo(server.getTraceId());
        assertThat(consumer.getParentSpanId()).isEqualTo(producer.getSpanId());
    }

    /**
     * An explicit {@code IGNORE} stays silent even inside an active trace.
     */
    @Test
    void ignoreWithinActiveTraceIsNotTraced() {
        RestAssured.when().get("/send/" + IGNORE_ADDRESS).then().statusCode(HTTP_OK);

        List<SpanData> spans = spanExporter.getFinishedSpanItems(1);

        assertThat(spans).extracting(SpanData::getKind).containsExactly(SERVER);
    }

    private void request(String address, TracingPolicy policy) throws Exception {
        eventBus.request(address, "hello", new DeliveryOptions().setTracingPolicy(policy))
                .toCompletionStage().toCompletableFuture().get(10, SECONDS);
    }

    private static SpanData single(List<SpanData> spans, SpanKind kind) {
        List<SpanData> matching = spans.stream().filter(span -> span.getKind() == kind).toList();
        assertThat(matching).as("exactly one %s span in %s", kind, spans).hasSize(1);
        return matching.get(0);
    }

    private static String destination(SpanData span) {
        return span.getAttributes().get(stringKey("messaging.destination.name"));
    }

    @Singleton
    public static class Events {

        @ConsumeEvent(PROPAGATE_ADDRESS)
        String propagate(String body) {
            return body;
        }

        @ConsumeEvent(ALWAYS_ADDRESS)
        String always(String body) {
            return body;
        }

        @ConsumeEvent(IGNORE_ADDRESS)
        String ignore(String body) {
            return body;
        }

        /**
         * Sends from inside the HTTP request, so an active trace is in place when the message goes out.
         */
        void registerRoutes(@Observes Router router, EventBus eventBus) {
            router.get("/send/:address").handler(rc -> {
                String address = rc.pathParam("address");
                TracingPolicy policy = IGNORE_ADDRESS.equals(address) ? TracingPolicy.IGNORE : TracingPolicy.PROPAGATE;
                eventBus.request(address, "hello", new DeliveryOptions().setTracingPolicy(policy))
                        .onComplete(reply -> rc.end("sent"));
            });
        }
    }
}
