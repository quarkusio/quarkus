package io.quarkus.redis.deployment.client;

import io.quarkus.builder.item.SimpleBuildItem;
import io.quarkus.redis.runtime.client.ObservableRedisMetrics;
import io.quarkus.runtime.RuntimeValue;

public final class RedisMetricsBuildItem extends SimpleBuildItem {

    private final RuntimeValue<ObservableRedisMetrics> metrics;

    public RedisMetricsBuildItem(RuntimeValue<ObservableRedisMetrics> metrics) {
        this.metrics = metrics;
    }

    public RuntimeValue<ObservableRedisMetrics> get() {
        return metrics;
    }
}
