package io.quarkus.vertx.deployment.spi;

import java.util.function.Consumer;

import org.jboss.logging.Logger;

import io.quarkus.builder.item.MultiBuildItem;
import io.vertx.core.internal.VertxBootstrap;

/**
 * Provide a consumer of VertxBootstrap to allow customization of
 * Vert.x bootstrap behavior, e.g. setting tracer factory or metrics.
 * <p>
 * Consumers will be called in priority order (lowest to highest), then by order key,
 * after VertxOptions customizers have been applied.
 * <p>
 * Unlike {@link VertxOptionsConsumerBuildItem}, there is no runtime alternative. {@link VertxBootstrap} being an
 * internal API, only extensions can customize it, and they must do so at build time.
 */
public final class VertxBootstrapConsumerBuildItem extends MultiBuildItem
        implements Comparable<VertxBootstrapConsumerBuildItem> {
    private final Consumer<VertxBootstrap> consumer;
    private final int priority;
    private final String orderKey;

    /**
     * @param consumer the consumer to apply to VertxBootstrap
     * @param priority consumers are called from lowest to highest priority
     * @param orderKey a stable key used to order consumers with the same priority
     */
    public VertxBootstrapConsumerBuildItem(Consumer<VertxBootstrap> consumer, int priority, String orderKey) {
        if (orderKey == null || orderKey.isEmpty()) {
            throw new IllegalArgumentException("orderKey must be present and not empty");
        }
        this.consumer = consumer;
        this.priority = priority;
        this.orderKey = orderKey;
    }

    public Consumer<VertxBootstrap> getConsumer() {
        return consumer;
    }

    @Override
    public int compareTo(VertxBootstrapConsumerBuildItem o) {
        if (this == o) {
            return 0;
        }
        int result = Integer.compare(this.priority, o.priority);
        if (result != 0) {
            return result;
        }
        Logger.getLogger(VertxBootstrapConsumerBuildItem.class).warnf(
                "Two VertxBootstrapConsumerBuildItem have the same priority (%d). The order of execution is not guaranteed. " +
                        "Consider using different priorities to ensure a deterministic order.",
                this.priority);
        result = this.orderKey.compareTo(o.orderKey);
        if (result == 0) {
            throw new IllegalStateException(
                    "Two VertxBootstrapConsumerBuildItem instances have the same priority and orderKey, which is not allowed. "
                            + "First instance: [priority: " + this.priority + ", orderKey: " + this.orderKey + "], "
                            + "second instance: [priority: " + o.priority + ", orderKey: " + o.orderKey + "]");
        }
        return result;
    }
}
