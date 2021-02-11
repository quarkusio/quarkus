package io.quarkus.deployment.metrics;

import java.util.function.Consumer;

import io.quarkus.builder.item.MultiBuildItem;
import io.quarkus.deployment.annotations.ExecutionTime;
import io.quarkus.runtime.metrics.MetricsFactory;

/**
 * A metrics provider will iterate over all MetricsFactory consumers,
 * allowing them to register metrics via bytecode recording
 */
public final class MetricsFactoryConsumerBuildItem extends MultiBuildItem {
    private final Consumer<MetricsFactory> factoryConsumer;
    private final ExecutionTime executionTime;

    public MetricsFactoryConsumerBuildItem(Consumer<MetricsFactory> factoryConsumer) {
        this.factoryConsumer = factoryConsumer;
        this.executionTime = ExecutionTime.RUNTIME_INIT;
    }

    public MetricsFactoryConsumerBuildItem(Consumer<MetricsFactory> factoryConsumer, ExecutionTime executionTime) {
        this.factoryConsumer = factoryConsumer;
        this.executionTime = executionTime;
    }

    public Consumer<MetricsFactory> getConsumer() {
        return factoryConsumer;
    }

    public ExecutionTime executionTime() {
        return executionTime;
    }
}
