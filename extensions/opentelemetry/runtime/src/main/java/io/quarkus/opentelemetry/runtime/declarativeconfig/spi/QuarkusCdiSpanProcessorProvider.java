package io.quarkus.opentelemetry.runtime.declarativeconfig.spi;

import java.util.ArrayList;
import java.util.List;

import io.opentelemetry.api.incubator.config.DeclarativeConfigProperties;
import io.opentelemetry.sdk.autoconfigure.spi.internal.ComponentProvider;
import io.opentelemetry.sdk.trace.SpanProcessor;
import io.quarkus.opentelemetry.runtime.declarativeconfig.QuarkusOTelModelBuilder;

/**
 * Resolves the {@link SpanProcessor} component named {@link QuarkusOTelModelBuilder#QUARKUS_CDI} to one processor
 * that calls the application's {@code SpanProcessor} beans in their iteration order. The node's configuration is not
 * used.
 */
public final class QuarkusCdiSpanProcessorProvider implements ComponentProvider {

    private final Iterable<SpanProcessor> spanProcessors;

    /**
     * Creates the provider.
     *
     * @param spanProcessors the beans, iterated only by {@link #create(DeclarativeConfigProperties)}, not
     *        {@code null}
     */
    public QuarkusCdiSpanProcessorProvider(Iterable<SpanProcessor> spanProcessors) {
        this.spanProcessors = spanProcessors;
    }

    @Override
    public Class<?> getType() {
        return SpanProcessor.class;
    }

    @Override
    public String getName() {
        return QuarkusOTelModelBuilder.QUARKUS_CDI;
    }

    @Override
    public Object create(DeclarativeConfigProperties config) {
        List<SpanProcessor> processors = new ArrayList<>();
        for (SpanProcessor processor : spanProcessors) {
            processors.add(processor);
        }
        return SpanProcessor.composite(processors);
    }
}
