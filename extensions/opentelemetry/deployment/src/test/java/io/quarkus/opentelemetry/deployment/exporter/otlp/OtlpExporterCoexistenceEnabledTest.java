package io.quarkus.opentelemetry.deployment.exporter.otlp;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Collection;

import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.opentelemetry.sdk.common.CompletableResultCode;
import io.opentelemetry.sdk.trace.data.SpanData;
import io.opentelemetry.sdk.trace.export.SpanExporter;
import io.quarkus.opentelemetry.runtime.config.build.exporter.OtlpExporterBuildConfig;
import io.quarkus.test.QuarkusExtensionTest;

/**
 * When coexistence is enabled, the built-in OTLP exporter is created in addition to a custom
 * CDI exporter, so both are present for the same signal.
 */
public class OtlpExporterCoexistenceEnabledTest {

    @RegisterExtension
    static final QuarkusExtensionTest config = new QuarkusExtensionTest()
            .withApplicationRoot((jar) -> jar.addClass(CustomSpanExporter.class))
            .overrideConfigKey("quarkus.otel.exporter.otlp.experimental.default-enabled", "true");

    @Inject
    Instance<SpanExporter> spanExporters;

    @Inject
    OtlpExporterBuildConfig otlpExporterBuildConfig;

    @Test
    void bothBuiltInAndCustomExportersArePresent() {
        assertThat(otlpExporterBuildConfig.experimental().defaultEnabled()).isTrue();
        assertThat(spanExporters.stream()).hasSizeGreaterThanOrEqualTo(2);
        assertThat(spanExporters.stream()).anyMatch(CustomSpanExporter.class::isInstance);
    }

    @Singleton
    public static class CustomSpanExporter implements SpanExporter {
        @Override
        public CompletableResultCode export(Collection<SpanData> spans) {
            return CompletableResultCode.ofSuccess();
        }

        @Override
        public CompletableResultCode flush() {
            return CompletableResultCode.ofSuccess();
        }

        @Override
        public CompletableResultCode shutdown() {
            return CompletableResultCode.ofSuccess();
        }
    }
}
