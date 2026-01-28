package io.quarkus.opentelemetry.runtime.metrics.spi;

import static io.quarkus.opentelemetry.runtime.config.build.ExporterType.Constants.CDI_VALUE;

import jakarta.enterprise.inject.Any;

import org.jboss.logging.Logger;

import io.opentelemetry.sdk.autoconfigure.spi.ConfigProperties;
import io.opentelemetry.sdk.autoconfigure.spi.metrics.ConfigurableMetricExporterProvider;
import io.opentelemetry.sdk.metrics.export.MetricExporter;
import io.quarkus.arc.Arc;
import io.quarkus.arc.InjectableInstance;
import io.quarkus.opentelemetry.runtime.exporter.otlp.metrics.NoopMetricExporter;

public class MetricsExporterCDIProvider implements ConfigurableMetricExporterProvider {

    private static final Logger LOG = Logger.getLogger(MetricsExporterCDIProvider.class.getName());

    @Override
    public MetricExporter createExporter(ConfigProperties configProperties) {
        InjectableInstance<MetricExporter> exporters = Arc.container().select(MetricExporter.class, Any.Literal.INSTANCE);
        if (LOG.isDebugEnabled()) {
            LOG.debugf("available exporters: %s", exporters.stream()
                    .map(e -> e.getClass().getName())
                    .reduce((a, b) -> a + ", " + b)
                    .orElse("none"));
        }
        if (exporters.isUnsatisfied() || exporters.listActive().isEmpty()) {
            return NoopMetricExporter.INSTANCE;
        } else {
            return exporters.get();
        }
    }

    @Override
    public String getName() {
        return CDI_VALUE;
    }
}
