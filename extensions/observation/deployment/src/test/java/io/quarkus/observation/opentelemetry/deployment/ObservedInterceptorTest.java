package io.quarkus.observation.opentelemetry.deployment;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import jakarta.annotation.Priority;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Alternative;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.micrometer.core.instrument.Metrics;
import io.micrometer.core.instrument.Timer;
import io.micrometer.core.instrument.observation.DefaultMeterObservationHandler;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import io.micrometer.observation.annotation.Observed;
import io.micrometer.observation.tck.TestObservationRegistry;
import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.trace.StatusCode;
import io.opentelemetry.sdk.trace.data.SpanData;
import io.quarkus.arc.Unremovable;
import io.quarkus.observation.opentelemetry.deployment.common.TestSpanExporter;
import io.quarkus.observation.opentelemetry.deployment.common.TestSpanExporterProvider;
import io.quarkus.observation.opentelemetry.handler.OpenTelemetryObservationHandler;
import io.quarkus.test.QuarkusExtensionTest;
import io.smallrye.mutiny.Uni;

public class ObservedInterceptorTest {

    @RegisterExtension
    static final QuarkusExtensionTest TEST = new QuarkusExtensionTest()
            .setArchiveProducer(
                    () -> ShrinkWrap.create(JavaArchive.class)
                            .addClasses(ObservedBean.class, TestRegistryProducer.class,
                                    TestSpanExporter.class, TestSpanExporterProvider.class)
                            .addAsManifestResource(
                                    "META-INF/services-config/io.opentelemetry.sdk.autoconfigure.spi.traces.ConfigurableSpanExporterProvider",
                                    "services/io.opentelemetry.sdk.autoconfigure.spi.traces.ConfigurableSpanExporterProvider")
                            .addAsResource(new StringAsset(
                                    "quarkus.otel.traces.exporter=test-span-exporter\n" +
                                            "quarkus.otel.bsp.schedule.delay=50ms\n" +
                                            "quarkus.observation.print-out=true\n" +
                                            "quarkus.otel.metrics.exporter=none\n" +
                                            "quarkus.log.category.\"io.quarkus.opentelemetry.runtime.QuarkusContextStorage\".level=DEBUG\n"),
                                    "application.properties"));

    static final SimpleMeterRegistry simpleMeterRegistry = new SimpleMeterRegistry();

    @Inject
    ObservedBean bean;

    @Inject
    TestSpanExporter spanExporter;

    @AfterEach
    void tearDown() {
        spanExporter.reset();
        simpleMeterRegistry.clear();
    }

    @Test
    void syncMethodProducesSpan() {
        String result = bean.syncMethod();
        assertThat(result).isEqualTo("hello");

        List<SpanData> spans = spanExporter.getFinishedSpanItems(1);
        SpanData span = spans.get(0);
        assertThat(span.getName()).contains("syncMethod");
        assertThat(span.getStatus().getStatusCode()).isEqualTo(StatusCode.OK);
        assertThat(span.getAttributes().get(AttributeKey.stringKey("code.function")))
                .isEqualTo("syncMethod");
        assertThat(span.getAttributes().get(AttributeKey.stringKey("code.namespace")))
                .contains("ObservedBean");

        Timer timer = simpleMeterRegistry.find("syncMethod").timer();
        assertThat(timer).isNotNull();
        assertThat(timer.count()).isEqualTo(1);
    }

    @Test
    void syncMethodWithErrorProducesErrorSpan() {
        try {
            bean.syncMethodWithError();
        } catch (RuntimeException expected) {
            // expected
        }

        List<SpanData> spans = spanExporter.getFinishedSpanItems(1);
        SpanData span = spans.get(0);
        assertThat(span.getStatus().getStatusCode()).isEqualTo(StatusCode.ERROR);
        assertThat(span.getEvents()).isNotEmpty();

        Timer timer = simpleMeterRegistry.find("syncMethodWithError").timer();
        assertThat(timer).isNotNull();
        assertThat(timer.count()).isEqualTo(1);
    }

    @Test
    void customNameIsUsed() {
        bean.customNameMethod();

        List<SpanData> spans = spanExporter.getFinishedSpanItems(1);
        SpanData span = spans.get(0);
        assertThat(span.getAttributes().get(AttributeKey.stringKey("code.function")))
                .isEqualTo("customNameMethod");

        Timer timer = simpleMeterRegistry.find("custom.name").timer();
        assertThat(timer).isNotNull();
        assertThat(timer.count()).isEqualTo(1);
    }

    @Test
    void asyncUniProducesSpan() {
        String result = bean.asyncUni().await().indefinitely();
        assertThat(result).isEqualTo("async-hello");

        List<SpanData> spans = spanExporter.getFinishedSpanItems(1);
        SpanData span = spans.get(0);
        assertThat(span.getName()).contains("asyncUni");
        assertThat(span.getStatus().getStatusCode()).isEqualTo(StatusCode.OK);

        Timer timer = simpleMeterRegistry.find("asyncUni").timer();
        assertThat(timer).isNotNull();
        assertThat(timer.count()).isEqualTo(1);
    }

    @Test
    void lowCardinalityKeyValuesFromAnnotation() {
        bean.withKeyValues();

        List<SpanData> spans = spanExporter.getFinishedSpanItems(1);
        SpanData span = spans.get(0);
        assertThat(span.getAttributes().get(AttributeKey.stringKey("env")))
                .isEqualTo("test");

        Timer timer = simpleMeterRegistry.find("withKeyValues")
                .tag("env", "test")
                .timer();
        assertThat(timer).isNotNull();
        assertThat(timer.count()).isEqualTo(1);
    }

    @ApplicationScoped
    public static class ObservedBean {

        @Observed
        public String syncMethod() {
            return "hello";
        }

        @Observed
        public String syncMethodWithError() {
            throw new RuntimeException("test error");
        }

        @Observed(name = "custom.name")
        public String customNameMethod() {
            return "custom";
        }

        @Observed
        public Uni<String> asyncUni() {
            return Uni.createFrom().item("async-hello");
        }

        @Observed(lowCardinalityKeyValues = { "env", "test" })
        public String withKeyValues() {
            return "kv";
        }
    }

    @ApplicationScoped
    public static class TestRegistryProducer {

        @Produces
        @Singleton
        @Alternative
        @Unremovable
        @Priority(Integer.MAX_VALUE)
        TestObservationRegistry testObservationRegistry(OpenTelemetryObservationHandler tracingHandler) {
            Metrics.addRegistry(simpleMeterRegistry);
            TestObservationRegistry registry = TestObservationRegistry.create();
            registry.observationConfig().observationHandler(tracingHandler);
            registry.observationConfig().observationHandler(
                    new DefaultMeterObservationHandler(simpleMeterRegistry));
            return registry;
        }
    }
}
