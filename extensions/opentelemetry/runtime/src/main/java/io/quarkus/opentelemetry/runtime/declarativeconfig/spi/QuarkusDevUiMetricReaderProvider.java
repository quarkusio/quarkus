package io.quarkus.opentelemetry.runtime.declarativeconfig.spi;

import io.opentelemetry.api.incubator.config.DeclarativeConfigProperties;
import io.opentelemetry.sdk.autoconfigure.spi.internal.ComponentProvider;
import io.opentelemetry.sdk.metrics.export.MetricReader;
import io.quarkus.opentelemetry.runtime.declarativeconfig.QuarkusOTelModelBuilder;

/**
 * Resolves the {@link MetricReader} component named {@link QuarkusOTelModelBuilder#QUARKUS_DEVUI} to the Dev UI's
 * in-memory reader. The node's configuration is not used.
 */
public final class QuarkusDevUiMetricReaderProvider implements ComponentProvider {

    private final MetricReader reader;

    /**
     * Creates the provider.
     *
     * @param reader the reader returned by {@link #create(DeclarativeConfigProperties)}, not {@code null}
     */
    public QuarkusDevUiMetricReaderProvider(MetricReader reader) {
        this.reader = reader;
    }

    @Override
    public Class<?> getType() {
        return MetricReader.class;
    }

    @Override
    public String getName() {
        return QuarkusOTelModelBuilder.QUARKUS_DEVUI;
    }

    @Override
    public Object create(DeclarativeConfigProperties config) {
        return reader;
    }
}
