package io.quarkus.vertx.deployment.spi;

import java.util.function.Consumer;

import org.jboss.logging.Logger;

import io.quarkus.builder.item.MultiBuildItem;
import io.vertx.core.internal.VertxBootstrap;

/**
 * Provide a consumer of VertxBootstrap to allow customization of
 * Vert.x bootstrap behavior, e.g. setting tracer factory or metrics.
 * <p>
 * Consumers will be called in priority order (lowest to highest)
 * after VertxOptions customizers have been applied.
 * <p>
 * When multiple build items have the same priority, the order of execution
 * is not guaranteed.
 * <p>
 * Unlike {@link VertxOptionsConsumerBuildItem}, there is no runtime alternative. {@link VertxBootstrap} being an
 * internal API, only extensions can customize it, and they must do so at build time.
 */
public final class VertxBootstrapConsumerBuildItem extends MultiBuildItem
        implements Comparable<VertxBootstrapConsumerBuildItem> {
    private final Consumer<VertxBootstrap> consumer;
    private final int priority;

    public VertxBootstrapConsumerBuildItem(Consumer<VertxBootstrap> consumer, int priority) {
        this.consumer = consumer;
        this.priority = priority;
    }

    public Consumer<VertxBootstrap> getConsumer() {
        return consumer;
    }

    @Override
    public int compareTo(VertxBootstrapConsumerBuildItem o) {
        int priorityComparison = Integer.compare(this.priority, o.priority);
        if (priorityComparison != 0) {
            return priorityComparison;
        }
        Logger.getLogger(VertxBootstrapConsumerBuildItem.class).warnf(
                "Two VertxBootstrapConsumerBuildItem have the same priority (%d). The order of execution is not guaranteed. " +
                        "Consider using different priorities to ensure a deterministic order.",
                this.priority);
        return priorityComparison;
    }
}
