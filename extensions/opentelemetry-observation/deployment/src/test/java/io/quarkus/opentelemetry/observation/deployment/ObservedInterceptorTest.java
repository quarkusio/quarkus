package io.quarkus.opentelemetry.observation.deployment;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.micrometer.observation.annotation.Observed;
import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.trace.StatusCode;
import io.opentelemetry.sdk.trace.data.SpanData;
import io.quarkus.opentelemetry.observation.deployment.common.TestSpanExporter;
import io.quarkus.opentelemetry.observation.deployment.common.TestSpanExporterProvider;
import io.quarkus.test.QuarkusExtensionTest;
import io.smallrye.mutiny.Uni;

public class ObservedInterceptorTest {

    @RegisterExtension
    static final QuarkusExtensionTest TEST = new QuarkusExtensionTest()
            .setArchiveProducer(
                    () -> ShrinkWrap.create(JavaArchive.class)
                            .addClasses(ObservedBean.class,
                                    TestSpanExporter.class, TestSpanExporterProvider.class)
                            .addAsManifestResource(
                                    "META-INF/services-config/io.opentelemetry.sdk.autoconfigure.spi.traces.ConfigurableSpanExporterProvider",
                                    "services/io.opentelemetry.sdk.autoconfigure.spi.traces.ConfigurableSpanExporterProvider")
                            .addAsResource(new StringAsset(
                                    "quarkus.otel.traces.exporter=test-span-exporter\n" +
                                            "quarkus.otel.bsp.schedule.delay=50ms\n" +
                                            "quarkus.otel.metrics.exporter=none\n"),
                                    "application.properties"));

    @Inject
    ObservedBean bean;

    @Inject
    TestSpanExporter spanExporter;

    @AfterEach
    void tearDown() {
        spanExporter.reset();
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
    }

    @Test
    void customNameIsUsed() {
        bean.customNameMethod();

        List<SpanData> spans = spanExporter.getFinishedSpanItems(1);
        SpanData span = spans.get(0);
        assertThat(span.getAttributes().get(AttributeKey.stringKey("code.function")))
                .isEqualTo("customNameMethod");
    }

    @Test
    void asyncUniProducesSpan() {
        String result = bean.asyncUni().await().indefinitely();
        assertThat(result).isEqualTo("async-hello");

        List<SpanData> spans = spanExporter.getFinishedSpanItems(1);
        SpanData span = spans.get(0);
        assertThat(span.getName()).contains("asyncUni");
        assertThat(span.getStatus().getStatusCode()).isEqualTo(StatusCode.OK);
    }

    @Test
    void lowCardinalityKeyValuesFromAnnotation() {
        bean.withKeyValues();

        List<SpanData> spans = spanExporter.getFinishedSpanItems(1);
        SpanData span = spans.get(0);
        assertThat(span.getAttributes().get(AttributeKey.stringKey("env")))
                .isEqualTo("test");
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
}
