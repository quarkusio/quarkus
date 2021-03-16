package io.quarkus.micrometer.runtime.registry.json;

import java.time.Duration;
import java.util.concurrent.TimeUnit;
import java.util.function.ToDoubleFunction;
import java.util.function.ToLongFunction;

import io.micrometer.core.instrument.Clock;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.DistributionSummary;
import io.micrometer.core.instrument.FunctionCounter;
import io.micrometer.core.instrument.FunctionTimer;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.LongTaskTimer;
import io.micrometer.core.instrument.Measurement;
import io.micrometer.core.instrument.Meter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import io.micrometer.core.instrument.cumulative.CumulativeCounter;
import io.micrometer.core.instrument.cumulative.CumulativeFunctionCounter;
import io.micrometer.core.instrument.cumulative.CumulativeFunctionTimer;
import io.micrometer.core.instrument.distribution.DistributionStatisticConfig;
import io.micrometer.core.instrument.distribution.pause.PauseDetector;
import io.micrometer.core.instrument.internal.DefaultGauge;
import io.micrometer.core.instrument.internal.DefaultMeter;
import io.micrometer.core.instrument.noop.NoopLongTaskTimer;

/**
 * A registry that, when exported, mimics the JSON exporter from MP Metrics 3.0
 * as closely as it is reasonable to attempt to.
 */
public class JsonMeterRegistry extends MeterRegistry {

    private final JsonExporter jsonExporter;
    private final Integer bufferLength;
    private final Duration expiry;

    public JsonMeterRegistry(Clock clock, Integer bufferLength, Duration expiry) {
        super(clock);
        this.jsonExporter = new JsonExporter();
        this.bufferLength = bufferLength;
        this.expiry = expiry;
    }

    @Override
    protected <T> Gauge newGauge(Meter.Id id, T obj, ToDoubleFunction<T> valueFunction) {
        return new DefaultGauge<>(id, obj, valueFunction);
    }

    @Override
    protected Counter newCounter(Meter.Id id) {
        return new CumulativeCounter(id);
    }

    @Override
    protected Timer newTimer(Meter.Id id, DistributionStatisticConfig distributionStatisticConfig,
            PauseDetector pauseDetector) {
        // turn off percentiles and histograms because we don't need them for the JSON export
        distributionStatisticConfig = DistributionStatisticConfig.builder()
                .percentilesHistogram(false)
                .percentiles(new double[0])
                .build()
                .merge(distributionStatisticConfig);
        return new JsonTimer(id, clock, distributionStatisticConfig, pauseDetector, getBaseTimeUnit());
    }

    @Override
    protected DistributionSummary newDistributionSummary(Meter.Id id, DistributionStatisticConfig distributionStatisticConfig,
            double scale) {
        return new JsonDistributionSummary(id, clock, distributionStatisticConfig, scale, false);
    }

    @Override
    protected Meter newMeter(Meter.Id id, Meter.Type type, Iterable<Measurement> measurements) {
        return new DefaultMeter(id, type, measurements);
    }

    @Override
    protected <T> FunctionTimer newFunctionTimer(Meter.Id id, T obj, ToLongFunction<T> countFunction,
            ToDoubleFunction<T> totalTimeFunction, TimeUnit totalTimeFunctionUnit) {
        return new CumulativeFunctionTimer<>(id, obj, countFunction, totalTimeFunction,
                totalTimeFunctionUnit, getBaseTimeUnit());
    }

    @Override
    protected <T> FunctionCounter newFunctionCounter(Meter.Id id, T obj, ToDoubleFunction<T> countFunction) {
        return new CumulativeFunctionCounter<>(id, obj, countFunction);
    }

    @Override
    protected LongTaskTimer newLongTaskTimer(Meter.Id id, DistributionStatisticConfig distributionStatisticConfig) {
        return new NoopLongTaskTimer(id);
    }

    @Override
    protected TimeUnit getBaseTimeUnit() {
        return TimeUnit.MILLISECONDS;
    }

    @Override
    protected DistributionStatisticConfig defaultHistogramConfig() {
        return DistributionStatisticConfig.builder()
                .percentiles(0.5, 0.75, 0.95, 0.98, 0.99, 0.999)
                .percentilePrecision(3)
                .percentilesHistogram(false)
                .minimumExpectedValue(Double.MIN_VALUE)
                .maximumExpectedValue(Double.POSITIVE_INFINITY)
                .bufferLength(bufferLength)
                .expiry(expiry)
                .build();
    }

    public String scrape() {
        return jsonExporter
                .exportEverything(this)
                .toString();
    }
}
