package io.quarkus.micrometer.runtime.binder.redis;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Typed;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Metrics;
import io.micrometer.core.instrument.Tag;
import io.micrometer.core.instrument.Tags;
import io.micrometer.core.instrument.Timer;
import io.quarkus.redis.runtime.client.ObservableRedisMetrics;

@ApplicationScoped
@Typed(ObservableRedisMetrics.class)
public class RedisMetricsBean implements ObservableRedisMetrics {

    final MeterRegistry registry = Metrics.globalRegistry;

    final Map<String, RedisMetrics> reportedMetrics = new ConcurrentHashMap<>();

    @Override
    public void report(String name, long durationInNs, boolean succeeded) {
        reportedMetrics.computeIfAbsent(name, n -> new RedisMetrics(registry, n)).report(name, durationInNs, succeeded);
    }

    private class RedisMetrics implements ObservableRedisMetrics {
        private final Tags tags;
        private final Counter operationCounter;
        private final Counter successCounter;

        private final Counter failureCounter;
        private final Timer timer;
        private String name;

        private RedisMetrics(MeterRegistry registry, String name) {
            this.name = name;
            this.tags = Tags.of(Tag.of("client-name", name));
            this.operationCounter = Counter.builder("redis.commands.count")
                    .description("The number of operations (commands or batches) executed").tags(tags)
                    .register(registry);
            this.successCounter = Counter.builder("redis.commands.success")
                    .description("The number of operations (commands or batches) that have been executed successfully")
                    .tags(tags).register(registry);
            this.failureCounter = Counter.builder("redis.commands.failure")
                    .description("The number of operations (commands or batches) that have been failed").tags(tags)
                    .register(registry);
            this.timer = Timer.builder("redis.commands.duration")
                    .description("The duration of the operations (commands of batches").tags(tags).register(registry);
        }

        @Override
        public void report(String name, long durationInNs, boolean succeeded) {
            operationCounter.increment();
            if (succeeded) {
                this.successCounter.increment();
            } else {
                this.failureCounter.increment();
            }
            timer.record(durationInNs, TimeUnit.NANOSECONDS);
        }
    }
}
