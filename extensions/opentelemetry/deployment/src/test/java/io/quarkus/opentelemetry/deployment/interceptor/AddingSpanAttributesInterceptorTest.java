package io.quarkus.opentelemetry.deployment.interceptor;

import static io.opentelemetry.api.trace.SpanKind.INTERNAL;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.context.Scope;
import io.opentelemetry.instrumentation.annotations.AddingSpanAttributes;
import io.opentelemetry.instrumentation.annotations.SpanAttribute;
import io.opentelemetry.instrumentation.annotations.WithSpan;
import io.opentelemetry.sdk.trace.data.SpanData;
import io.quarkus.opentelemetry.deployment.common.exporter.TestSpanExporter;
import io.quarkus.opentelemetry.deployment.common.exporter.TestSpanExporterProvider;
import io.quarkus.test.QuarkusUnitTest;

public class AddingSpanAttributesInterceptorTest {
    @RegisterExtension
    static final QuarkusUnitTest TEST = new QuarkusUnitTest()
            .setArchiveProducer(
                    () -> ShrinkWrap.create(JavaArchive.class)
                            .addClass(HelloRouter.class)
                            .addClasses(TestSpanExporter.class, TestSpanExporterProvider.class)
                            .addAsManifestResource(
                                    "META-INF/services-config/io.opentelemetry.sdk.autoconfigure.spi.traces.ConfigurableSpanExporterProvider",
                                    "services/io.opentelemetry.sdk.autoconfigure.spi.traces.ConfigurableSpanExporterProvider")
                            .addAsResource("resource-config/application-no-metrics.properties", "application.properties"));

    @Inject
    HelloRouter helloRouter;
    @Inject
    Tracer tracer;
    @Inject
    TestSpanExporter spanExporter;

    @AfterEach
    void tearDown() {
        spanExporter.reset();
    }

    @Test
    void withSpanAttributesTest_existingSpan() {
        Span span = tracer.spanBuilder("withSpanAttributesTest").startSpan();
        String result;
        try (Scope scope = span.makeCurrent()) {
            result = helloRouter.withSpanAttributes(
                    "implicit", "explicit", null, "ignore");
        } finally {
            span.end();
        }
        assertEquals("hello!", result);
        List<SpanData> spanItems = spanExporter.getFinishedSpanItems(1);
        SpanData spanDataOut = spanItems.get(0);
        assertEquals("withSpanAttributesTest", spanDataOut.getName());
        assertEquals(INTERNAL, spanDataOut.getKind());
        assertFalse(spanDataOut.getAttributes().isEmpty(), "No attributes found");
        assertEquals("implicit", getAttribute(spanDataOut, "implicitName"));
        assertEquals("explicit", getAttribute(spanDataOut, "explicitName"));
    }

    @Test
    void withSpanAttributesTest_noActiveSpan() {
        String resultWithoutSpan = helloRouter.withSpanAttributes(
                "implicit", "explicit", null, "ignore");
        assertEquals("hello!", resultWithoutSpan);

        spanExporter.getFinishedSpanItems(0);
        // No span created

        String resultWithSpan = helloRouter.withSpanTakesPrecedence(
                "implicit", "explicit", null, "ignore");
        assertEquals("hello!", resultWithSpan);

        // we need 1 span to make sure we don't get a false positive.
        // The previous call to getFinishedSpanItems might return too early.

        List<SpanData> spanItems = spanExporter.getFinishedSpanItems(1);
        assertEquals(1, spanItems.size());
        SpanData spanDataOut = spanItems.get(0);
        assertEquals("HelloRouter.withSpanTakesPrecedence", spanDataOut.getName());
    }

    @Test
    void withSpanAttributesTest_newSpan() {
        String result = helloRouter.withSpanTakesPrecedence(
                "implicit", "explicit", null, "ignore");

        assertEquals("hello!", result);
        List<SpanData> spanItems = spanExporter.getFinishedSpanItems(1);
        SpanData spanDataOut = spanItems.get(0);
        assertEquals("HelloRouter.withSpanTakesPrecedence", spanDataOut.getName());
        assertEquals(INTERNAL, spanDataOut.getKind());
        assertEquals(2, spanDataOut.getAttributes().size());
        assertEquals("implicit", getAttribute(spanDataOut, "implicitName"));
        assertEquals("explicit", getAttribute(spanDataOut, "explicitName"));
    }

    @Test
    void noAttributesAdded() {
        Span span = tracer.spanBuilder("noAttributesAdded").startSpan();
        String result;
        try (Scope scope = span.makeCurrent()) {
            result = helloRouter.noAttributesAdded(
                    "implicit", "explicit", null, "ignore");
        } finally {
            span.end();
        }
        assertEquals("hello!", result);
        List<SpanData> spanItems = spanExporter.getFinishedSpanItems(1);
        SpanData spanDataOut = spanItems.get(0);
        assertEquals("noAttributesAdded", spanDataOut.getName());
        assertEquals(INTERNAL, spanDataOut.getKind());
        assertTrue(spanDataOut.getAttributes().isEmpty(), "No attributes must be present");
    }

    private static Object getAttribute(SpanData spanDataOut, String attributeName) {
        return spanDataOut.getAttributes().asMap().get(AttributeKey.stringKey(attributeName));
    }

    @ApplicationScoped
    public static class HelloRouter {
        // mast have already an active span
        @AddingSpanAttributes
        public String withSpanAttributes(
                @SpanAttribute String implicitName,
                @SpanAttribute("explicitName") String parameter,
                @SpanAttribute("nullAttribute") String nullAttribute,
                String notTraced) {

            return "hello!";
        }

        @WithSpan
        @AddingSpanAttributes
        public String withSpanTakesPrecedence(
                @SpanAttribute String implicitName,
                @SpanAttribute("explicitName") String parameter,
                @SpanAttribute("nullAttribute") String nullAttribute,
                String notTraced) {

            return "hello!";
        }

        public String noAttributesAdded(
                @SpanAttribute String implicitName,
                @SpanAttribute("explicitName") String parameter,
                @SpanAttribute("nullAttribute") String nullAttribute,
                String notTraced) {

            return "hello!";
        }
    }
}
