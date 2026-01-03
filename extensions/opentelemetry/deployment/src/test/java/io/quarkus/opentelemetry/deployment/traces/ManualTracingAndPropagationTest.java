package io.quarkus.opentelemetry.deployment.traces;

import static java.util.Collections.emptyList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import org.jboss.logging.Logger;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.jetbrains.annotations.Nullable;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.baggage.Baggage;
import io.opentelemetry.api.baggage.BaggageEntry;
import io.opentelemetry.api.baggage.BaggageEntryMetadata;
import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.context.Context;
import io.opentelemetry.context.Scope;
import io.opentelemetry.context.propagation.TextMapGetter;
import io.opentelemetry.context.propagation.TextMapSetter;
import io.opentelemetry.sdk.trace.data.SpanData;
import io.quarkus.opentelemetry.deployment.common.exporter.TestSpanExporter;
import io.quarkus.opentelemetry.deployment.common.exporter.TestSpanExporterProvider;
import io.quarkus.test.QuarkusUnitTest;

/**
 * Class testing some of the examples used in the docs
 */
public class ManualTracingAndPropagationTest {

    private static final Logger logger = Logger.getLogger(ManualTracingAndPropagationTest.class);

    @RegisterExtension
    static final QuarkusUnitTest TEST = new QuarkusUnitTest()
            .setArchiveProducer(
                    () -> ShrinkWrap.create(JavaArchive.class)
                            .addClass(HelloSpan.class)
                            .addClasses(TestSpanExporter.class, TestSpanExporterProvider.class)
                            .addAsResource(new StringAsset(TestSpanExporterProvider.class.getCanonicalName()),
                                    "META-INF/services/io.opentelemetry.sdk.autoconfigure.spi.traces.ConfigurableSpanExporterProvider"))
            .withConfigurationResource("resource-config/application-no-metrics.properties");

    @Inject
    TestSpanExporter spanExporter;

    @Inject
    HelloSpan helloBean;

    @Inject
    HelloPropagation helloPropagation;

    @AfterEach
    void tearDown() {
        spanExporter.reset();
    }

    /**
     * This is a test for the HelloBean class used in the docs
     */
    @Test
    void helloManualSpanTest() {
        assertEquals("Hello from Quarkus REST", helloBean.helloManualSpan());
        List<SpanData> spans = spanExporter.getFinishedSpanItems(1);
        assertNotNull(spans);
        assertEquals(1, spans.size());
        assertEquals("HelloBean.helloManualSpan", spans.get(0).getName());
        assertEquals("myValue", spans.get(0)
                .getAttributes().asMap()
                .get(AttributeKey.stringKey("myAttributeName")));
    }

    @Test
    void helloManualContextPropagationTest() {
        String parentContext = helloPropagation.createParentContext();
        assertNotNull(parentContext);
        helloPropagation.receiveAndUseContext(parentContext);

        List<SpanData> spans = spanExporter.getFinishedSpanItems(2);
        assertNotNull(spans);
        assertEquals(2, spans.size());

        Optional<SpanData> parentSpan = spans.stream()
                .filter(span -> span.getName().equals("parent-span"))
                .findFirst();
        assertTrue(parentSpan.isPresent());
        assertEquals("0000000000000000", parentSpan.get().getParentSpanId());

        Optional<SpanData> childSpan = spans.stream()
                .filter(span -> span.getName().equals("child-span"))
                .findFirst();
        assertTrue(childSpan.isPresent());
        assertEquals(parentSpan.get().getSpanId(), childSpan.get().getParentSpanId());
    }

    @Test
    void helloBaggagePropagationTest() {
        Map<String, String> headers = helloPropagation.createBaggage("baggage_value");
        // will only contain the baggage
        assertThat(headers).hasSize(1);
        assertEquals("baggage_value", helloPropagation.receiveAndUseBaggage(headers));
    }

    @ApplicationScoped
    public static class HelloSpan {

        private final Tracer tracer;

        // Instead of using @Inject on the tracer attribute
        public HelloSpan(Tracer tracer) {
            // The same as openTelemetry.getTracer("io.quarkus.opentelemetry");
            this.tracer = tracer;
        }

        public String helloManualSpan() {
            // Create a new span
            Span span = tracer.spanBuilder("HelloBean.helloManualSpan").startSpan();
            // Make sure span scope is closed
            try (Scope scope = span.makeCurrent()) {
                // Add an attribute
                span.setAttribute("myAttributeName", "myValue");
                // Execute logic...
                return "Hello from Quarkus REST";
            } catch (Exception ignored) {
                // Store potential exceptions.
                span.recordException(ignored);
            } finally {
                // Whatever happens above, the span will be closed.
                span.end();
            }
            return "failover message";
        }
    }

    @ApplicationScoped
    public static class HelloPropagation {

        /**
         * How data is stored
         */
        private static final TextMapSetter<Map<String, String>> MAP_SETTER = new TextMapSetter<Map<String, String>>() {
            @Override
            public void set(@Nullable Map<String, String> carrier, String key, String value) {
                carrier.put(key, value);
            }
        };

        /**
         * How data is retrieved
         */
        private static final TextMapGetter<Map<String, String>> MAP_GETTER = new TextMapGetter<>() {

            @Override
            public Iterable<String> keys(Map<String, String> carrier) {
                return carrier == null ? emptyList() : carrier.keySet();
            }

            @Override
            public @Nullable String get(@Nullable Map<String, String> carrier, String key) {
                return carrier == null && key != null ? null : carrier.get(key);
            }
        };

        private final Tracer tracer;
        private final OpenTelemetry openTelemetry;

        // Instead of using @Inject on the tracer attribute
        public HelloPropagation(Tracer tracer, OpenTelemetry openTelemetry) {
            this.openTelemetry = openTelemetry;
            // The same as openTelemetry.getTracer("io.quarkus.opentelemetry");
            this.tracer = tracer;
        }

        public String createParentContext() {
            // Create the first manual span
            Span parentSpan = tracer.spanBuilder("parent-span").startSpan();

            try (Scope ignored = parentSpan.makeCurrent()) {
                // inject into a temporary map and return the traceparent header value
                // This can be stored in any data structure containing metadata in the payload to ship.
                // As an example, for HTTP the headers, for JMS a message attribute
                Map<String, String> tempCarrier = new HashMap<>();

                // We will use The MAP_SETTER to place the header containing the OTel Context in the tempCarrier
                openTelemetry.getPropagators()
                        .getTextMapPropagator()
                        .inject(Context.current(), tempCarrier, MAP_SETTER);

                // W3C traceparent header key is "traceparent"
                return tempCarrier.get("traceparent");
            } finally {
                parentSpan.end();
            }
        }

        public void receiveAndUseContext(String traceparent) {
            // Rebuild a tempCarrier map that contains the "traceparent" header data
            Map<String, String> tempCarrier = new HashMap<>();
            tempCarrier.put("traceparent", traceparent);

            // Extract context from the tempCarrier
            Context extracted = openTelemetry.getPropagators()
                    .getTextMapPropagator()
                    .extract(Context.current(), tempCarrier, MAP_GETTER);

            // Optionally check whether a parent span was found:
            boolean hasParent = Span.fromContext(extracted).getSpanContext().isValid();

            // Start a child span with the extracted context as explicit parent
            Span child = tracer.spanBuilder("child-span")
                    .setParent(extracted)
                    .startSpan();

            try (Scope scope = child.makeCurrent()) {
                // Simulate work under child span
                logger.infov("Child span started. Extracted parent valid? {0}", hasParent);
            } finally {
                child.end();
            }
        }

        public Map<String, String> createBaggage(String withValue) {
            // Baggage can be used to send Application specific data using the OpenTelemetry propagation mechanism
            Baggage baggage = Baggage.builder()
                    .put("baggage_key", withValue, BaggageEntryMetadata.empty())
                    .build();
            Context context = baggage.storeInContext(Context.current());

            // We use this to store the header data to ship out.
            Map<String, String> tempCarrier = new HashMap<>();

            // We will use The MAP_SETTER to place the header containing the OTel Context in the tempCarrier
            try (Scope ignored = context.makeCurrent()) {
                openTelemetry.getPropagators()
                        .getTextMapPropagator()
                        .inject(Context.current(), tempCarrier, MAP_SETTER);
            }
            return tempCarrier;
        }

        public String receiveAndUseBaggage(Map<String, String> headers) {
            // Extract context from the tempCarrier
            Context extracted = openTelemetry.getPropagators()
                    .getTextMapPropagator()
                    .extract(Context.current(), headers, MAP_GETTER);
            // Retrieve the baggage contents
            Baggage baggage = Baggage.fromContext(extracted);
            BaggageEntry baggageKey = baggage.asMap().get("baggage_key");
            return baggageKey != null ? baggageKey.getValue() : null;
        }
    }

}
