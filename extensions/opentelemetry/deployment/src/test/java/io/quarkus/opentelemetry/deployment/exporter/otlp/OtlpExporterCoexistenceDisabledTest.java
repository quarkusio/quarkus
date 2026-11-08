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
 * With coexistence disabled (the default), a custom CDI exporter suppresses the built-in OTLP
 * exporter, so only the custom one is present.
 */
public class OtlpExporterCoexistenceDisabledTest {

    @RegisterExtension
    static final QuarkusExtensionTest config = new QuarkusExtensionTest()
            .withApplicationRoot((jar) -> jar.addClass(CustomSpanExporter.class));

    @Inject
    Instance<SpanExporter> spanExporters;

    @Inject
    OtlpExporterBuildConfig otlpExporterBuildConfig;

    @Test
    void onlyTheCustomExporterIsPresent() {
        assertThat(otlpExporterBuildConfig.experimental().defaultEnabled()).isFalse();
        assertThat(spanExporters.stream()).hasSize(1);
        assertThat(spanExporters.get()).isInstanceOf(CustomSpanExporter.class);
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
