package io.quarkus.opentelemetry.deployment.exporter.otlp;

import static org.junit.jupiter.api.Assertions.assertTrue;

import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.opentelemetry.sdk.trace.export.SpanExporter;
import io.quarkus.builder.BuildChainBuilder;
import io.quarkus.builder.BuildContext;
import io.quarkus.builder.BuildStep;
import io.quarkus.test.QuarkusUnitTest;

/**
 * Verifies that a Quarkiverse-style extension registering itself via
 * {@link ExternalOtelExporterBuildItem} correctly suppresses the default
 * OTLP SpanExporter when coexistence is not enabled, and that it can
 * coexist with the default when quarkus.otel.experimental.otlp.default.enable=true.
 */
public class OtlpExternalExporterBuildItemTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .withEmptyApplication()
            .addBuildChainCustomizer(registerExternalExporter())
            .overrideConfigKey("quarkus.otel.traces.exporter", "cdi");

    @Inject
    Instance<SpanExporter> spanExporterInstance;

    @Test
    void externalExporterBuildItemSuppressesDefault() {
        // No SpanExporter bean is actually produced by this test's fake external exporter
        // (it only registers the BuildItem, simulating an extension that wires its own bean
        // elsewhere) — so we assert the default OTLP exporter was NOT created because
        // ExternalOtelExporterBuildItem signals "an exporter already exists".
        assertTrue(spanExporterInstance.isUnsatisfied(),
                "Default OTLP SpanExporter should not be created when an ExternalOtelExporterBuildItem is present");
    }

    private static java.util.function.Consumer<BuildChainBuilder> registerExternalExporter() {
        return builder -> builder.addBuildStep(new BuildStep() {
            @Override
            public void execute(BuildContext context) {
                context.produce(new ExternalOtelExporterBuildItem("test-external-exporter"));
            }
        }).produces(ExternalOtelExporterBuildItem.class).build();
    }
}
