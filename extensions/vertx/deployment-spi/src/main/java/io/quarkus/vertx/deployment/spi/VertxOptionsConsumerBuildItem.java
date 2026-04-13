package io.quarkus.vertx.deployment.spi;

import java.util.function.Consumer;

import org.jboss.logging.Logger;

import io.quarkus.builder.item.MultiBuildItem;
import io.vertx.core.VertxOptions;

/**
 * Provide a consumer of VertxOptions to allow customization of
 * Vert.x system behavior, e.g. setting MetricsOptions to enable
 * and configure a metrics provider.
 * <p>
 * Consumers will be called in priority order (lowest to highest)
 * after VertxConfiguration has been read and applied.
 */
public final class VertxOptionsConsumerBuildItem extends MultiBuildItem implements Comparable<VertxOptionsConsumerBuildItem> {
    private final Consumer<VertxOptions> consumer;
    private final int priority;

    public VertxOptionsConsumerBuildItem(Consumer<VertxOptions> consumer, int priority) {
        this.consumer = consumer;
        this.priority = priority;
    }

    public Consumer<VertxOptions> getConsumer() {
        return consumer;
    }

    @Override
    public int compareTo(VertxOptionsConsumerBuildItem o) {
        int priorityComparison = Integer.compare(this.priority, o.priority);
        if (priorityComparison != 0) {
            return priorityComparison;
        }
        Logger.getLogger(VertxOptionsConsumerBuildItem.class).warnf(
                "Two VertxOptionsConsumerBuildItems have the same priority (%d). The order of execution is not guaranteed. " +
                        "Consider using different priorities to ensure a deterministic order.",
                this.priority);
        return priorityComparison;
    }
}
