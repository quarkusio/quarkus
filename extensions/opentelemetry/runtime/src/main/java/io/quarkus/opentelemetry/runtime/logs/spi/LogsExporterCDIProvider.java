package io.quarkus.opentelemetry.runtime.logs.spi;

import static io.quarkus.opentelemetry.runtime.config.build.ExporterType.Constants.CDI_VALUE;

import jakarta.enterprise.inject.Any;
import jakarta.enterprise.inject.Instance;
import jakarta.enterprise.inject.spi.CDI;

import org.jboss.logging.Logger;

import io.opentelemetry.api.incubator.config.DeclarativeConfigProperties;
import io.opentelemetry.sdk.autoconfigure.spi.ConfigProperties;
import io.opentelemetry.sdk.autoconfigure.spi.internal.ComponentProvider;
import io.opentelemetry.sdk.autoconfigure.spi.logs.ConfigurableLogRecordExporterProvider;
import io.opentelemetry.sdk.logs.export.LogRecordExporter;
import io.quarkus.opentelemetry.runtime.exporter.otlp.logs.NoopLogRecordExporter;

/**
 * Implements both {@link ConfigurableLogRecordExporterProvider} (the legacy, property-based autoconfigure SPI,
 * resolved by name "cdi") and {@link ComponentProvider} (the declarative-config SPI, resolved by the same name) so
 * the same CDI lookup backs both SDK initialization paths.
 */
public class LogsExporterCDIProvider implements ConfigurableLogRecordExporterProvider, ComponentProvider {

    Logger log = Logger.getLogger(LogsExporterCDIProvider.class.getName());

    @Override
    public LogRecordExporter createExporter(ConfigProperties configProperties) {
        return createExporter();
    }

    @Override
    public Class<?> getType() {
        return LogRecordExporter.class;
    }

    @Override
    public Object create(DeclarativeConfigProperties config) {
        return createExporter();
    }

    @Override
    public String getName() {
        return CDI_VALUE;
    }

    private LogRecordExporter createExporter() {
        Instance<LogRecordExporter> exporters = CDI.current().select(LogRecordExporter.class, Any.Literal.INSTANCE);
        if (log.isDebugEnabled()) {
            log.debug("available exporters: " + exporters.stream()
                    .map(e -> e.getClass().getName())
                    .reduce((a, b) -> a + ", " + b)
                    .orElse("none"));
        }
        if (exporters.isUnsatisfied()) {
            return NoopLogRecordExporter.INSTANCE;
        } else {
            log.debugf("using exporter: %s", exporters.get().getClass().getName());
            return exporters.get();
        }
    }
}
