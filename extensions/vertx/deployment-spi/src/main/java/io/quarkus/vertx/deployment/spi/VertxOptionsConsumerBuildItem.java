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
 * Consumers will be called in priority order (lowest to highest), then by order key,
 * after VertxConfiguration has been read and applied.
 */
public final class VertxOptionsConsumerBuildItem extends MultiBuildItem implements Comparable<VertxOptionsConsumerBuildItem> {
    private final Consumer<VertxOptions> consumer;
    private final int priority;
    private final String orderKey;

    /**
     * @param optionsConsumer the consumer to apply to VertxOptions
     * @param priority consumers are called from lowest to highest priority
     * @param orderKey a stable key used to order consumers with the same priority
     */
    public VertxOptionsConsumerBuildItem(Consumer<VertxOptions> consumer, int priority, String orderKey) {
        if (orderKey == null || orderKey.isEmpty()) {
            throw new IllegalArgumentException("orderKey must be present and not empty");
        }
        this.consumer = consumer;
        this.priority = priority;
        this.orderKey = orderKey;
    }

    public Consumer<VertxOptions> getConsumer() {
        return consumer;
    }

    @Override
    public int compareTo(VertxOptionsConsumerBuildItem o) {
        if (this == o) {
            return 0;
        }
        int result = Integer.compare(this.priority, o.priority);
        if (result != 0) {
            return result;
        }
        result = this.orderKey.compareTo(o.orderKey);

        if (result == 0) {
            throw new IllegalStateException(
                    "Two VertxOptionsConsumerBuildItem instances have the same priority and orderKey, which is not allowed. "
                            + "First instance: [priority: " + this.priority + ", orderKey: " + this.orderKey + "], "
                            + "second instance: [priority: " + o.priority + ", orderKey: " + o.orderKey + "]");
        }
        return result;
    }
}
