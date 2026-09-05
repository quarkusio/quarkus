package io.quarkus.opentelemetry.deployment.exporter.otlp;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.opentelemetry.sdk.trace.export.SpanExporter;
import io.quarkus.test.QuarkusExtensionTest;

/**
 * Setting a signal's exporter to {@code none} disables the built-in exporter for that signal even
 * when coexistence is enabled.
 */
public class OtlpExporterCoexistenceNoneWinsTest {

    @RegisterExtension
    static final QuarkusExtensionTest config = new QuarkusExtensionTest()
            .withEmptyApplication()
            .overrideConfigKey("quarkus.otel.traces.exporter", "none")
            .overrideConfigKey("quarkus.otel.exporter.otlp.experimental.default-enabled", "true");

    @Inject
    Instance<SpanExporter> spanExporters;

    @Test
    void noneDisablesTheBuiltInExporterDespiteCoexistence() {
        assertThat(spanExporters.isResolvable()).isFalse();
        assertThat(spanExporters.stream()).isEmpty();
    }
}
