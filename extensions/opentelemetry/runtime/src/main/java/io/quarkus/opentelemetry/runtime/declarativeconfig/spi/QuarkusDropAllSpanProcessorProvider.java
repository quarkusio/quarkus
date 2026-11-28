package io.quarkus.opentelemetry.runtime.declarativeconfig.spi;

import io.opentelemetry.api.incubator.config.DeclarativeConfigProperties;
import io.opentelemetry.sdk.autoconfigure.spi.internal.ComponentProvider;
import io.opentelemetry.sdk.trace.SpanProcessor;
import io.quarkus.opentelemetry.runtime.declarativeconfig.QuarkusOTelModelBuilder;
import io.quarkus.opentelemetry.runtime.tracing.DropAllSpanProcessor;

/**
 * Resolves the {@link SpanProcessor} component named {@link QuarkusOTelModelBuilder#QUARKUS_DROP_ALL} to a new
 * {@link DropAllSpanProcessor}. The node's configuration is not used.
 */
public final class QuarkusDropAllSpanProcessorProvider implements ComponentProvider {

    @Override
    public Class<?> getType() {
        return SpanProcessor.class;
    }

    @Override
    public String getName() {
        return QuarkusOTelModelBuilder.QUARKUS_DROP_ALL;
    }

    @Override
    public Object create(DeclarativeConfigProperties config) {
        return new DropAllSpanProcessor();
    }
}
