package io.quarkus.opentelemetry.deployment.traces;

import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.context.Context;
import io.opentelemetry.context.Scope;
import io.opentelemetry.context.propagation.TextMapGetter;
import io.opentelemetry.context.propagation.TextMapPropagator;
import io.opentelemetry.context.propagation.TextMapSetter;
import io.opentelemetry.sdk.trace.data.SpanData;
import io.quarkus.logging.Log;
import io.quarkus.opentelemetry.deployment.common.exporter.TestSpanExporter;
import io.quarkus.opentelemetry.deployment.common.exporter.TestSpanExporterProvider;
import io.quarkus.opentelemetry.runtime.propagation.TextMapPropagatorCustomizer;
import io.quarkus.test.QuarkusUnitTest;
import io.quarkus.vertx.http.runtime.CurrentRequestProducer;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.jetbrains.annotations.Nullable;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static java.util.Collections.emptyList;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * Class testing some of the examples used in the docs
 */
public class HelloBeanTest {
    @RegisterExtension
    static final QuarkusUnitTest TEST = new QuarkusUnitTest()
            .setArchiveProducer(
                    () -> ShrinkWrap.create(JavaArchive.class)
                            .addClass(HelloBean.class)
                            .addClasses(TestSpanExporter.class, TestSpanExporterProvider.class)
                            .addAsResource(new StringAsset(TestSpanExporterProvider.class.getCanonicalName()),
                                    "META-INF/services/io.opentelemetry.sdk.autoconfigure.spi.traces.ConfigurableSpanExporterProvider"))
            .withConfigurationResource("resource-config/application-no-metrics.properties");

    @Inject
    TestSpanExporter spanExporter;

    @Inject
    HelloBean helloBean;
    @Inject
    private CurrentRequestProducer currentRequestProducer;

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
    void helloTestPropagationTest() {
        String parentContext = helloBean.createParentContext();
        assertNotNull(parentContext);
        helloBean.receiveAndUseContext(parentContext);

        List<SpanData> spans = spanExporter.getFinishedSpanItems(2);
        assertNotNull(spans);
        assertEquals(2, spans.size());
        assertEquals("parent-span", spans.get(0).getName());
        assertEquals("0000000000000000", spans.get(0).getParentSpanId());
        assertEquals("child-span", spans.get(1).getName());
        assertEquals(spans.get(0).getSpanId(), spans.get(1).getParentSpanId());
    }

    @ApplicationScoped
    public static class HelloBean {

        private static final TextMapSetter<Map<String, String>> MAP_SETTER = new TextMapSetter<Map<String, String>>() {
            @Override
            public void set(@Nullable Map<String, String> carrier, String key, String value) {
                carrier.put(key, value);
            }
        };

        private static final TextMapGetter<Map<String, String>> MAP_GETTER =
                new TextMapGetter<>() {

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
        public HelloBean(Tracer tracer, OpenTelemetry openTelemetry) {
            this.openTelemetry = openTelemetry;
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

        public String createParentContext() {
            Span parentSpan = tracer.spanBuilder("parent-span").startSpan();

            try (Scope ignored = parentSpan.makeCurrent()) {
                // inject into a temporary map and return the traceparent header value
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
            // Rebuild a tempCarrier map that contains the "traceparent" header
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
                Log.infov("Child span started. Extracted parent valid? {0}", hasParent);
            } finally {
                child.end();
            }
        }
    }

    @Singleton
    public static class TestTextMapPropagatorCustomizer implements TextMapPropagatorCustomizer {
        @Inject
        OpenTelemetry openTelemetry;

        @Override
        public TextMapPropagator customize(Context propagationContext) {
            propagationContext.propagator().inject(otelContext, carrier, setter));
        }
    }
}
