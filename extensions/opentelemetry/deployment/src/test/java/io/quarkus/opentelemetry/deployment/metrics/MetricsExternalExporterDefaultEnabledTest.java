package io.quarkus.opentelemetry.deployment.metrics;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.function.Consumer;

import jakarta.inject.Inject;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.opentelemetry.sdk.metrics.export.MetricExporter;
import io.quarkus.arc.InjectableInstance;
import io.quarkus.builder.BuildChainBuilder;
import io.quarkus.builder.BuildContext;
import io.quarkus.builder.BuildStep;
import io.quarkus.opentelemetry.deployment.exporter.otlp.ExternalOtelExporterBuildItem;
import io.quarkus.opentelemetry.runtime.exporter.otlp.metrics.VertxGrpcMetricExporter;
import io.quarkus.test.QuarkusUnitTest;

/**
 * Verify that the default metrics exporter is active when explicitly enabled.
 */
public class MetricsExternalExporterDefaultEnabledTest {

    @RegisterExtension
    static final QuarkusUnitTest TEST = new QuarkusUnitTest()
            .withEmptyApplication()
            .overrideConfigKey("quarkus.otel.metrics.enabled", "true")
            .overrideConfigKey("quarkus.otel.exporter.otlp.metrics.default-exporter-enabled", "true")
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
    InjectableInstance<MetricExporter> metricExporterInstance;

    @Test
    void defaultExporterResolvable() {
        assertThat(metricExporterInstance.listActive().isEmpty()).isFalse();
        assertThat(metricExporterInstance.listActive().get(0)).isInstanceOf(VertxGrpcMetricExporter.class);
    }
}
