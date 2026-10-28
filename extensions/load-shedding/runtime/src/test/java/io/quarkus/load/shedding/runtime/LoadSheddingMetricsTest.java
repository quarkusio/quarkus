package io.quarkus.load.shedding.runtime;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;

public class LoadSheddingMetricsTest {
    private static final int LIMIT = 10;

    // the number of concurrent requests at which the overloaded state is left
    private static final int CLEAR_AT = (int) (OverloadDetector.HYSTERESIS_FACTOR * LIMIT);

    // strictly decreasing, so that every sample establishes a new minimum and the limit
    // never adapts; all assertions below are stated against `LIMIT`
    private long nextRequestTime = 1_000_000;

    private final LoadSheddingRuntimeConfig config = new TestConfig.Builder()
            .withMinLimit(1)
            .withInitialLimit(LIMIT)
            .withProbeFactor(Double.MAX_VALUE)
            .build();
    private final SimpleMeterRegistry registry = new SimpleMeterRegistry();
    private final OverloadDetector detector = new OverloadDetector(config);
    private final LoadSheddingMetrics metrics = new LoadSheddingMetrics(detector, config);

    @BeforeEach
    public void bind() {
        // deliberately bound before anything happens, so that the tests also show that
        // the meters read the current state at collection time and not the state at binding time
        metrics.bindTo(registry);
    }

    @Test
    public void allMetersAreRegistered() {
        assertThat(registry.getMeters())
                .extracting(meter -> meter.getId().getName())
                .containsExactlyInAnyOrder(
                        LoadSheddingMetrics.SHED_REQUESTS,
                        LoadSheddingMetrics.CURRENT_REQUESTS,
                        LoadSheddingMetrics.CURRENT_LIMIT,
                        LoadSheddingMetrics.OVERLOAD_STATUS,
                        LoadSheddingMetrics.OVERLOAD_DEPTH,
                        LoadSheddingMetrics.OVERLOAD_DURATION);
    }

    // nothing ever counts a request, adapts the limit or signals an overload when load
    // shedding is disabled, so the meters would only report the initial values forever
    @Test
    public void noMetersAreRegisteredWhenLoadSheddingIsDisabled() {
        SimpleMeterRegistry other = new SimpleMeterRegistry();

        new LoadSheddingMetrics(detector, new TestConfig.Builder().withEnabled(false).build()).bindTo(other);

        assertThat(other.getMeters()).isEmpty();
    }

    @Test
    public void shedRequestsAreCounted() {
        assertThat(shedRequests()).isZero();

        metrics.requestShed();
        metrics.requestShed();

        assertThat(shedRequests()).isEqualTo(2.0);
    }

    @Test
    public void shedRequestsAreNotCountedAsCurrent() {
        // a rejected request is answered and forgotten; it never enters the detector
        metrics.requestShed();

        assertThat(currentRequests()).isZero();
    }

    @Test
    public void currentRequestsFollowTheDetector() {
        begin(LIMIT);
        assertThat(currentRequests()).isEqualTo(LIMIT);

        end(LIMIT);
        assertThat(currentRequests()).isZero();
    }

    @Test
    public void currentLimitIsThePointWhereSheddingStarts() {
        assertThat(currentLimit()).isEqualTo(LIMIT);

        begin(LIMIT - 1);
        assertThat(overloaded()).isZero();

        // the very request that reaches the limit is the one that signals the overload
        begin(1);
        assertThat(currentRequests()).isEqualTo(currentLimit());
        assertThat(overloaded()).isEqualTo(1.0);
    }

    @Test
    public void overloadedReflectsTheHysteresis() {
        // the state cannot be derived from the two gauges above,
        // which is why it is published on its own
        begin(LIMIT);
        assertThat(overloaded()).isEqualTo(1.0);

        end(LIMIT - CLEAR_AT);
        assertThat(currentRequests()).isEqualTo(CLEAR_AT);
        assertThat(overloaded()).isEqualTo(1.0);

        end(1);
        assertThat(overloaded()).isZero();
    }

    @Test
    public void overloadFactorGrowsPastTheLimit() {
        begin(LIMIT);
        assertThat(overloadDepth()).isZero();

        begin(1);
        assertThat(overloadDepth()).isPositive();
    }

    @Test
    public void overloadDurationIsZeroWhenNotOverloaded() throws InterruptedException {
        assertThat(overloadDurationSeconds()).isZero();

        begin(LIMIT - 1);
        assertThat(overloadDurationSeconds()).isZero();

        begin(1);
        Thread.sleep(10);
        assertThat(overloadDurationSeconds()).isPositive();
    }

    @Test
    public void overloadDurationIsResetWhenTheOverloadEnds() throws InterruptedException {
        begin(LIMIT);

        Thread.sleep(10);
        assertThat(overloadDurationSeconds()).isPositive();

        end(LIMIT);
        assertThat(overloadDurationSeconds()).isZero();
    }

    private double shedRequests() {
        return registry.get(LoadSheddingMetrics.SHED_REQUESTS).functionCounter().count();
    }

    private double currentRequests() {
        return registry.get(LoadSheddingMetrics.CURRENT_REQUESTS).gauge().value();
    }

    private double currentLimit() {
        return registry.get(LoadSheddingMetrics.CURRENT_LIMIT).gauge().value();
    }

    private double overloaded() {
        return registry.get(LoadSheddingMetrics.OVERLOAD_STATUS).gauge().value();
    }

    private double overloadDepth() {
        return registry.get(LoadSheddingMetrics.OVERLOAD_DEPTH).gauge().value();
    }

    private double overloadDurationSeconds() {
        return registry.get(LoadSheddingMetrics.OVERLOAD_DURATION).timeGauge().value(TimeUnit.SECONDS);
    }

    private void begin(int count) {
        for (int i = 0; i < count; i++) {
            detector.requestBegin();
        }
    }

    private void end(int count) {
        for (int i = 0; i < count; i++) {
            detector.requestEnd(nextRequestTime--);
        }
    }
}
