package io.quarkus.opentelemetry.runtime.metrics.spi;

import static io.quarkus.opentelemetry.runtime.config.build.ExporterType.Constants.CDI_VALUE;

import jakarta.enterprise.inject.Any;
import jakarta.enterprise.inject.Instance;
import jakarta.enterprise.inject.spi.CDI;

import org.jboss.logging.Logger;

import io.opentelemetry.api.incubator.config.DeclarativeConfigProperties;
import io.opentelemetry.sdk.autoconfigure.spi.ConfigProperties;
import io.opentelemetry.sdk.autoconfigure.spi.internal.ComponentProvider;
import io.opentelemetry.sdk.autoconfigure.spi.metrics.ConfigurableMetricExporterProvider;
import io.opentelemetry.sdk.metrics.export.MetricExporter;
import io.quarkus.opentelemetry.runtime.exporter.otlp.metrics.NoopMetricExporter;

/**
 * Implements both {@link ConfigurableMetricExporterProvider} (the legacy, property-based autoconfigure SPI,
 * resolved by name "cdi") and {@link ComponentProvider} (the declarative-config SPI, resolved by the same name) so
 * the same CDI lookup backs both SDK initialization paths.
 */
public class MetricsExporterCDIProvider implements ConfigurableMetricExporterProvider, ComponentProvider {

    private static final Logger LOG = Logger.getLogger(MetricsExporterCDIProvider.class.getName());

    @Override
    public MetricExporter createExporter(ConfigProperties configProperties) {
        return createExporter();
    }

    @Override
    public Class<?> getType() {
        return MetricExporter.class;
    }

    @Override
    public Object create(DeclarativeConfigProperties config) {
        return createExporter();
    }

    @Override
    public String getName() {
        return CDI_VALUE;
    }

    private MetricExporter createExporter() {
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
}
