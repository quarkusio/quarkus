package io.quarkus.opentelemetry.deployment.logs;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.function.Consumer;

import jakarta.inject.Inject;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.opentelemetry.sdk.logs.export.LogRecordExporter;
import io.quarkus.arc.InjectableInstance;
import io.quarkus.builder.BuildChainBuilder;
import io.quarkus.builder.BuildContext;
import io.quarkus.builder.BuildStep;
import io.quarkus.opentelemetry.deployment.exporter.otlp.ExternalOtelExporterBuildItem;
import io.quarkus.opentelemetry.runtime.exporter.otlp.logs.VertxGrpcLogRecordExporter;
import io.quarkus.test.QuarkusUnitTest;

/**
 * Verify that the default log exporter is active when explicitly enabled.
 */
public class OtelLogsExternalExporterDefaultEnabledTest {

    @RegisterExtension
    static final QuarkusUnitTest TEST = new QuarkusUnitTest()
            .withEmptyApplication()
            .overrideConfigKey("quarkus.otel.logs.enabled", "true")
            .overrideConfigKey("quarkus.otel.exporter.otlp.logs.default-exporter-enabled", "true")
            .addBuildChainCustomizer(new Consumer<>() {
                @Override
                public void accept(BuildChainBuilder buildChainBuilder) {
                    buildChainBuilder.addBuildStep().produces(ExternalOtelExporterBuildItem.class)
                            .setBuildStep(new BuildStep() {
                                @Override
                                public void execute(BuildContext context) {
                                    context.produce(new ExternalOtelExporterBuildItem("external-exporter"));
                                }
                            }).build();
                }
            });

    @Inject
    InjectableInstance<LogRecordExporter> logRecordExporter;

    @Test
    void testOpenTelemetryButNoSpanProcessor() {
        assertThat(logRecordExporter.listActive().isEmpty()).isFalse();
        assertThat(logRecordExporter.listActive().get(0)).isInstanceOf(VertxGrpcLogRecordExporter.class);
    }
}
