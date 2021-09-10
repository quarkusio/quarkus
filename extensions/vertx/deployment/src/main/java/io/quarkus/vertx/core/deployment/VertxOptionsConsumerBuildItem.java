package io.quarkus.vertx.core.deployment;

import java.util.function.Consumer;

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
    private final Consumer<VertxOptions> optionsConsumer;
    private final int priority;

    public VertxOptionsConsumerBuildItem(Consumer<VertxOptions> optionsConsumer, int priority) {
        this.optionsConsumer = optionsConsumer;
        this.priority = priority;
    }

    public Consumer<VertxOptions> getConsumer() {
        return optionsConsumer;
    }

    @Override
    public int compareTo(VertxOptionsConsumerBuildItem o) {
        return Integer.compare(this.priority, o.priority);
    }
}
