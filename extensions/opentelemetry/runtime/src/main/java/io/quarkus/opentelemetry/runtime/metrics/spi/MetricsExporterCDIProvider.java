package io.quarkus.opentelemetry.runtime.metrics.spi;

import static io.quarkus.opentelemetry.runtime.config.build.ExporterType.Constants.CDI_VALUE;

import java.util.logging.Logger;

import jakarta.enterprise.inject.Any;
import jakarta.enterprise.inject.Instance;
import jakarta.enterprise.inject.spi.CDI;

import io.opentelemetry.sdk.autoconfigure.spi.ConfigProperties;
import io.opentelemetry.sdk.autoconfigure.spi.metrics.ConfigurableMetricExporterProvider;
import io.opentelemetry.sdk.metrics.export.MetricExporter;
import io.quarkus.opentelemetry.runtime.exporter.otlp.metrics.NoopMetricExporter;

public class MetricsExporterCDIProvider implements ConfigurableMetricExporterProvider {

    Logger log = Logger.getLogger(MetricsExporterCDIProvider.class.getName());

    @Override
    public MetricExporter createExporter(ConfigProperties configProperties) {
        Instance<MetricExporter> exporters = CDI.current().select(MetricExporter.class, Any.Literal.INSTANCE);
        log.fine("available exporters: " + exporters.stream()
                .map(e -> e.getClass().getName())
                .reduce((a, b) -> a + ", " + b)
                .orElse("none"));
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
