package io.quarkus.opentelemetry.runtime.metrics.spi;

import static io.quarkus.opentelemetry.runtime.config.build.ExporterType.Constants.CDI_VALUE;

import jakarta.enterprise.inject.Any;
import jakarta.enterprise.inject.Instance;
import jakarta.enterprise.inject.spi.CDI;

import org.jboss.logging.Logger;

import io.opentelemetry.sdk.autoconfigure.spi.ConfigProperties;
import io.opentelemetry.sdk.autoconfigure.spi.metrics.ConfigurableMetricExporterProvider;
import io.opentelemetry.sdk.metrics.export.MetricExporter;
import io.quarkus.opentelemetry.runtime.exporter.otlp.metrics.NoopMetricExporter;

public class MetricsExporterCDIProvider implements ConfigurableMetricExporterProvider {

    private static final Logger LOG = Logger.getLogger(MetricsExporterCDIProvider.class.getName());

    @Override
    public MetricExporter createExporter(ConfigProperties configProperties) {
        Instance<MetricExporter> exporters = CDI.current().select(MetricExporter.class, Any.Literal.INSTANCE);
        if (LOG.isDebugEnabled()) {
            LOG.debugf("available exporters: %s", exporters.stream()
                    .map(e -> e.getClass().getName())
                    .reduce((a, b) -> a + ", " + b)
                    .orElse("none"));
        }
        if (exporters.isUnsatisfied()) {
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
