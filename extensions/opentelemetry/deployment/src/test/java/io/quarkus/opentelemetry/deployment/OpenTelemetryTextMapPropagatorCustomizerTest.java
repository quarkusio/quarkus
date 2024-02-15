package io.quarkus.opentelemetry.deployment;

import static io.quarkus.opentelemetry.deployment.common.TestSpanExporter.getSpanByKindAndParentId;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.Response;

import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.opentelemetry.api.baggage.propagation.W3CBaggagePropagator;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.api.trace.propagation.W3CTraceContextPropagator;
import io.opentelemetry.context.propagation.TextMapPropagator;
import io.opentelemetry.sdk.trace.data.SpanData;
import io.quarkus.opentelemetry.deployment.common.TestSpanExporter;
import io.quarkus.opentelemetry.deployment.common.TestSpanExporterProvider;
import io.quarkus.opentelemetry.runtime.propagation.TextMapPropagatorCustomizer;
import io.quarkus.test.QuarkusUnitTest;

public class OpenTelemetryTextMapPropagatorCustomizerTest {

    @RegisterExtension
    static final QuarkusUnitTest TEST = new QuarkusUnitTest().setArchiveProducer(
            () -> ShrinkWrap.create(JavaArchive.class)
                    .addClass(TestSpanExporter.class)
                    .addClass(TestSpanExporterProvider.class)
                    .addClass(TestTextMapPropagatorCustomizer.class)
                    .addAsResource("resource-config/application.properties", "application.properties")
                    .addAsResource(
                            "META-INF/services-config/io.opentelemetry.sdk.autoconfigure.spi.traces.ConfigurableSpanExporterProvider",
                            "META-INF/services/io.opentelemetry.sdk.autoconfigure.spi.traces.ConfigurableSpanExporterProvider"));
    @Inject
    TestSpanExporter spanExporter;

    @Inject
    @RestClient
    HelloRestClient client;

    @Test
    void testPropagatorCustomizer_NoPropagation() {
        Response response = client.hello();

        List<SpanData> spans = spanExporter.getFinishedSpanItems(2);
        SpanData clientSpan = getSpanByKindAndParentId(spans, SpanKind.CLIENT, "0000000000000000");
        assertEquals("GET", clientSpan.getName());

        // There is a parent id, therefore propagation is working.
        SpanData serverSpan = getSpanByKindAndParentId(spans, SpanKind.SERVER, clientSpan.getSpanId());
        assertEquals("GET /hello", serverSpan.getName());

        assertThat(TestTextMapPropagatorCustomizer.PROPAGATORS_USED).containsOnly(W3CTraceContextPropagator.class.getName());

        assertThat(TestTextMapPropagatorCustomizer.PROPAGATORS_DISCARDED).containsOnly(W3CBaggagePropagator.class.getName());

    }

    @RegisterRestClient(configKey = "client")
    @Path("/hello")
    public interface HelloRestClient {
        @GET
        Response hello();
    }

    @Path("/hello")
    public static class HelloResource {
        @GET
        public String hello() {
            return "hello";
        }
    }

    @Singleton
    public static class TestTextMapPropagatorCustomizer implements TextMapPropagatorCustomizer {
        // just to understand this was actually called.
        public static final Set<String> PROPAGATORS_USED = ConcurrentHashMap.newKeySet();
        public static final Set<String> PROPAGATORS_DISCARDED = ConcurrentHashMap.newKeySet();

        @Override
        public TextMapPropagator customize(Context context) {
            TextMapPropagator propagator = context.propagator();
            if (propagator instanceof W3CBaggagePropagator) {
                PROPAGATORS_DISCARDED.add(propagator.getClass().getName());
                return TextMapPropagator.noop();
            }
            PROPAGATORS_USED.add(propagator.getClass().getName());
            return propagator;
        }
    }
}
