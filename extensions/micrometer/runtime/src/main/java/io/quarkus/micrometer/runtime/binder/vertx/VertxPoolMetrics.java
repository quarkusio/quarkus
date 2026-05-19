package io.quarkus.micrometer.runtime.binder.vertx;

import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.atomic.LongAdder;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tag;
import io.micrometer.core.instrument.Tags;
import io.micrometer.core.instrument.Timer;
import io.quarkus.micrometer.runtime.meters.Gauges;
import io.vertx.core.spi.metrics.PoolMetrics;

/**
 * Adaptation of the Vert.x Pool Metrics implementation for Quarkus Micrometer.
 */
public class VertxPoolMetrics implements PoolMetrics<EventTiming> {

    private final String poolType;
    private final int maxPoolSize;

    private final Timer usage;
    private final AtomicReference<Double> ratio;
    private final LongAdder current;
    private final LongAdder idle;
    private final LongAdder queue;
    private final Counter completed;
    private final Counter rejected;
    private final Timer queueDelay;

    VertxPoolMetrics(MeterRegistry registry, String poolType, String poolName, int maxPoolSize,
            Gauges<LongAdder> longAdderGauges, Gauges<AtomicReference<Double>> doubleGauges) {
        this.poolType = poolType;
        this.maxPoolSize = maxPoolSize;

        Tags tags = Tags.of(Tag.of("pool.type", poolType), Tag.of("pool.name", poolName));

        queueDelay = Timer.builder(name("queue.delay"))
                .description("Time spent in the waiting queue before being processed")
                .tags(tags)
                .register(registry);

        usage = Timer.builder(name("usage"))
                .description("Time spent using resources from the pool")
                .tags(tags)
                .register(registry);

        queue = longAdderGauges.builder(name("queue.size"), LongAdder::doubleValue)
                .description("Number of pending elements in the waiting queue")
                .tags(tags)
                .register(registry);

        current = longAdderGauges.builder(name("active"), LongAdder::doubleValue)
                .description("The number of resources from the pool currently used")
                .tags(tags)
                .register(registry);

        if (maxPoolSize > 0) {
            idle = longAdderGauges.builder(name("idle"), LongAdder::doubleValue)
                    .description("The number of resources from the pool currently idle")
                    .tags(tags)
                    .register(registry);
            idle.add(maxPoolSize);

            ratio = doubleGauges.builder(name("ratio"), AtomicReference::get)
                    .description("Pool usage ratio")
                    .tags(tags)
                    .register(registry);
        } else {
            idle = null;
            ratio = null;
        }

        completed = Counter.builder(name("completed"))
                .description("Number of times resources from the pool have been acquired")
                .tags(tags)
                .register(registry);

        rejected = Counter.builder(name("rejected"))
                .description("Number of times submissions to the pool have been rejected")
                .tags(tags)
                .register(registry);
    }

    private String name(String suffix) {
        return poolType + ".pool." + suffix;
    }

    @Override
    public EventTiming submitted() {
        queue.increment();
        return new EventTiming(queueDelay);
    }

    @Override
    public void rejected(EventTiming submitted) {
        queue.decrement();
        rejected.increment();
        submitted.end();
    }

    @Override
    public EventTiming begin(EventTiming submitted) {
        queue.decrement();
        submitted.end();
        current.increment();
        if (idle != null) {
            idle.decrement();
        }
        computeRatio(current.longValue());
        return new EventTiming(usage);
    }

    @Override
    public void end(EventTiming timer, boolean succeeded) {
        current.decrement();
        if (idle != null) {
            idle.increment();
        }
        computeRatio(current.longValue());
        timer.end();

        completed.increment();
    }

    @Override
    public void close() {
    }

    private void computeRatio(long inUse) {
        if (ratio != null && maxPoolSize > 0) {
            ratio.set((double) inUse / maxPoolSize);
        }
    }

}
