package io.quarkus.opentelemetry.deployment.exporter.otlp;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Collection;

import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.opentelemetry.sdk.common.CompletableResultCode;
import io.opentelemetry.sdk.metrics.InstrumentType;
import io.opentelemetry.sdk.metrics.data.AggregationTemporality;
import io.opentelemetry.sdk.metrics.data.MetricData;
import io.opentelemetry.sdk.metrics.export.MetricExporter;
import io.quarkus.test.QuarkusExtensionTest;

/**
 * Two custom {@link MetricExporter} beans must not cause an {@code AmbiguousResolutionException}
 * when the SDK asks the CDI provider for the metric exporter. The provider composites them, so the
 * application boots and both remain resolvable.
 */
public class OtlpMultipleMetricExportersTest {

    @RegisterExtension
    static final QuarkusExtensionTest config = new QuarkusExtensionTest()
            .withApplicationRoot((jar) -> jar.addClasses(CustomMetricExporterA.class, CustomMetricExporterB.class))
            .overrideConfigKey("quarkus.otel.metrics.enabled", "true");

    @Inject
    Instance<MetricExporter> metricExporters;

    @Test
    void bothCustomMetricExportersBootAndResolve() {
        assertThat(metricExporters.stream()).hasSizeGreaterThanOrEqualTo(2);
        assertThat(metricExporters.stream()).anyMatch(CustomMetricExporterA.class::isInstance);
        assertThat(metricExporters.stream()).anyMatch(CustomMetricExporterB.class::isInstance);
    }

    abstract static class AbstractCustomMetricExporter implements MetricExporter {
        @Override
        public CompletableResultCode export(Collection<MetricData> metrics) {
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

        @Override
        public AggregationTemporality getAggregationTemporality(InstrumentType instrumentType) {
            return AggregationTemporality.CUMULATIVE;
        }
    }

    @Singleton
    public static class CustomMetricExporterA extends AbstractCustomMetricExporter {
    }

    @Singleton
    public static class CustomMetricExporterB extends AbstractCustomMetricExporter {
    }
}
