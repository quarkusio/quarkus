package io.quarkus.opentelemetry.runtime.config.runtime.exporter;

import java.util.Optional;

import io.quarkus.runtime.annotations.ConfigGroup;
import io.smallrye.config.WithDefault;

@ConfigGroup
public interface OtlpExporterMetricsConfig extends OtlpExporterConfig {

    /**
     * The preferred output aggregation temporality. Options include DELTA, LOWMEMORY, and CUMULATIVE.
     * <p>
     * If CUMULATIVE, all instruments will have cumulative temporality.
     * If DELTA, counter (sync and async) and histograms will be delta, up down counters (sync and async) will be cumulative.
     * If LOWMEMORY, sync counter and histograms will be delta, async counter and up down counters (sync and async) will be
     * cumulative.
     * <p>
     * Default is CUMULATIVE.
     */
    @WithDefault("cumulative")
    Optional<String> temporalityPreference();

    /**
     * The preferred default histogram aggregation.
     * <p>
     * Options include BASE2_EXPONENTIAL_BUCKET_HISTOGRAM and EXPLICIT_BUCKET_HISTOGRAM.
     * <p>
     * Default is EXPLICIT_BUCKET_HISTOGRAM.
     */
    @WithDefault("explicit_bucket_histogram")
    Optional<String> defaultHistogramAggregation();
}
