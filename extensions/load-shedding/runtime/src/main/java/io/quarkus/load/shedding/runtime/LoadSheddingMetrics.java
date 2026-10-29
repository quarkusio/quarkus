package io.quarkus.load.shedding.runtime;

import java.util.concurrent.TimeUnit;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import io.micrometer.core.instrument.FunctionCounter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.TimeGauge;
import io.micrometer.core.instrument.binder.MeterBinder;

/**
 * Exposes the state of load shedding as Micrometer meters.
 * <p>
 * All meters are untagged. The decision to reject a request is made per request priority and
 * per cohort, but there are 640 combinations of the two, and a time series per combination
 * would cost far more than the insight is worth. The overload depth below determines which
 * of them are being shed anyway, see {@link io.quarkus.load.shedding.runtime.PriorityLoadShedding}.
 * <p>
 * This bean is only registered when the Micrometer extension is present and
 * {@code quarkus.load-shedding.metrics.enabled} is {@code true}. Whether load shedding itself
 * is enabled is a runtime decision, so that one is checked in {@link #bindTo(MeterRegistry)}.
 */
@Singleton
public class LoadSheddingMetrics implements MeterBinder {
    public static final String SHED_REQUESTS = "load.shedding.shed.requests";
    public static final String CURRENT_REQUESTS = "load.shedding.current.requests";
    public static final String CURRENT_LIMIT = "load.shedding.current.limit";
    public static final String OVERLOAD_STATUS = "load.shedding.overload.status";
    public static final String OVERLOAD_DEPTH = "load.shedding.overload.depth";
    public static final String OVERLOAD_DURATION = "load.shedding.overload.duration";

    private final OverloadDetector detector;
    private final LoadSheddingCounter counter;
    private final boolean enabled;

    @Inject
    public LoadSheddingMetrics(OverloadDetector detector, LoadSheddingCounter counter, LoadSheddingRuntimeConfig config) {
        this.detector = detector;
        this.counter = counter;
        this.enabled = config.enabled();
    }

    @Override
    public void bindTo(MeterRegistry registry) {
        // nothing ever counts a request, adapts the limit or signals an overload, so the
        // meters would only report the initial values forever
        if (!enabled) {
            return;
        }

        FunctionCounter.builder(SHED_REQUESTS, counter, LoadSheddingCounter::coundShedRequests)
                .description("Number of requests rejected by load shedding")
                .baseUnit("requests")
                .register(registry);

        Gauge.builder(CURRENT_REQUESTS, detector, OverloadDetector::currentRequests)
                .description("Number of requests currently in flight, as counted by the overload detector")
                .baseUnit("requests")
                .register(registry);

        Gauge.builder(CURRENT_LIMIT, detector, OverloadDetector::currentLimit)
                .description("The request limit the overload detector has currently adapted to; "
                        + "reaching it signals an overload")
                .baseUnit("requests")
                .register(registry);

        Gauge.builder(OVERLOAD_STATUS, detector, d -> d.isOverloaded() ? 1.0 : 0.0)
                .description("Whether the application is currently overloaded: 1 if it is, 0 if it is not; "
                        + "the state is hysteretic, so it cannot be derived from the current requests and limit")
                .register(registry);

        Gauge.builder(OVERLOAD_DEPTH, detector, OverloadDetector::overloadDepth)
                .description("How far past the concurrency limit the application currently is, between 0.0 and 1.0; "
                        + "the higher it is, the more request priorities are being rejected")
                .register(registry);

        TimeGauge.builder(OVERLOAD_DURATION, detector, TimeUnit.NANOSECONDS, OverloadDetector::overloadDurationNanos)
                .description("How long the current overload has lasted, or 0 if the application is not overloaded; "
                        + "this is what the Retry-After header of a rejected request is derived from")
                .register(registry);
    }
}
