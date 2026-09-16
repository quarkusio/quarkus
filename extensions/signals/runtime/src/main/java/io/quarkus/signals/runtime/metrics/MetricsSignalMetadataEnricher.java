package io.quarkus.signals.runtime.metrics;

import jakarta.inject.Singleton;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Meter.MeterProvider;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tags;
import io.quarkus.signals.SignalContext;
import io.quarkus.signals.spi.SignalMetadataEnricher;
import io.smallrye.common.annotation.Identifier;

/**
 * Built-in enricher that increments the {@value MetricsSupport#EMISSIONS} counter once per emission.
 * <p>
 * The enricher runs synchronously on the emitting thread, which is invoked exactly once per emission (regardless of the
 * number of matching receivers), so it is the natural place to count emissions. It does not add any metadata.
 * <p>
 * It is only registered when the Micrometer metrics capability is present.
 */
@Identifier(MetricsSignalMetadataEnricher.ID)
@Singleton
public class MetricsSignalMetadataEnricher implements SignalMetadataEnricher {

    public static final String ID = "quarkus.metrics";

    private final MeterProvider<Counter> emissions;

    MetricsSignalMetadataEnricher(MeterRegistry registry) {
        this.emissions = Counter.builder(MetricsSupport.EMISSIONS).withRegistry(registry);
    }

    @Override
    public void enrich(EnrichmentContext context) {
        SignalContext<?> signalContext = context.signalContext();
        String responseType = signalContext.responseType() != null
                ? MetricsSupport.rawTypeName(signalContext.responseType())
                : MetricsSupport.RESPONSE_TYPE_NONE;
        emissions.withTags(Tags.of(
                MetricsSupport.TAG_SIGNAL_TYPE, MetricsSupport.rawTypeName(signalContext.signalType()),
                MetricsSupport.TAG_EMISSION_TYPE, signalContext.emissionType().toString(),
                MetricsSupport.TAG_RESPONSE_TYPE, responseType)).increment();
    }
}
