package io.quarkus.micrometer.runtime.binder.vertx;

import java.util.concurrent.atomic.LongAdder;

import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.LongTaskTimer;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tag;
import io.micrometer.core.instrument.Tags;
import io.vertx.core.spi.metrics.PoolMetrics;

public class VertxPoolMetrics implements PoolMetrics<VertxPoolMetrics.TimeRecorder> {

    final static String QUEUE_TIME = "vertx.pool.queue";
    final static String POOL_USAGE = "vertx.pool.usage";
    final static String IN_USE = "vertx.pool.in.use";
    final static String RATIO = "vertx.pool.ratio";
    final static String COMPLETED = "vertx.pool.completed";

    final LongAdder inUse = new LongAdder();
    final LongTaskTimer queueDelayTimer;
    final LongTaskTimer poolUseTimer;

    /**
     * @param poolName Pool name (used in pools domain)
     * @param poolType Pool type, such as "worker" or "datasource" (used in pools domain)
     */
    public VertxPoolMetrics(MeterRegistry registry, String poolName, String poolType, int maxPoolSize) {

        Tags commonTags = Tags.of(Tag.of("pool_name", poolName), Tag.of("pool_type", poolType));

        this.queueDelayTimer = LongTaskTimer.builder(QUEUE_TIME)
                .description("Time spent in the queue waiting for a resource")
                .tags(commonTags)
                .register(registry);

        this.poolUseTimer = LongTaskTimer.builder(POOL_USAGE)
                .description("Time spent using a resource from the pool")
                .tags(commonTags)
                .register(registry);

        if (maxPoolSize > 0) {
            Gauge.builder(RATIO, inUse, x -> {
                return (inUse.doubleValue() / maxPoolSize);
            })
                    .description("Pool usage ratio (maxPoolSize=" + maxPoolSize + ")")
                    .strongReference(true)
                    .tags(commonTags)
                    .register(registry);
        }
    }

    @Override
    public TimeRecorder submitted() {
        return new TimeRecorder(this)
                .startSubmittedTimer();
    }

    @Override
    public void rejected(TimeRecorder taskRecorder) {
        taskRecorder.stopSubmittedTimer(false);
    }

    @Override
    public TimeRecorder begin(TimeRecorder taskRecorder) {
        return taskRecorder
                .stopSubmittedTimer(true)
                .startRunTimer();
    }

    @Override
    public void end(TimeRecorder taskRecorder, boolean succeeded) {
        taskRecorder.stopRunTimer(succeeded);
    }

    static class TimeRecorder {
        private final VertxPoolMetrics metrics;

        LongTaskTimer.Sample queueActiveTimerSample;
        LongTaskTimer.Sample runTimerSample;

        TimeRecorder(VertxPoolMetrics metrics) {
            this.metrics = metrics;
        }

        public TimeRecorder startSubmittedTimer() {
            queueActiveTimerSample = metrics.queueDelayTimer.start();
            return this;
        }

        protected TimeRecorder stopSubmittedTimer(boolean accepted) {
            queueActiveTimerSample.stop();
            return this;
        }

        public TimeRecorder startRunTimer() {
            metrics.inUse.increment();
            runTimerSample = metrics.poolUseTimer.start();
            return this;
        }

        public void stopRunTimer(boolean succeeded) {
            runTimerSample.stop();
            metrics.inUse.decrement();
        }
    }

}
