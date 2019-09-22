package io.quarkus.vertx.http.deployment;

import java.util.Objects;

import io.quarkus.builder.item.MultiBuildItem;
import io.vertx.core.Handler;
import io.vertx.ext.web.RoutingContext;

/**
 * A handler that is applied to every route
 */
public final class FilterBuildItem extends MultiBuildItem implements Comparable<FilterBuildItem> {

    private final Handler<RoutingContext> handler;
    private final int priority;

    /**
     * Creates a new instance of {@link FilterBuildItem}.
     *
     * @param handler the handler, must not be {@code null}
     * @param priority the priority, higher priority gets invoked first. Priority is only used to sort filters, user
     *        routes are called afterwards. Must be positive.
     */
    public FilterBuildItem(Handler<RoutingContext> handler, int priority) {
        if (handler == null) {
            throw new IllegalArgumentException("`handler` must not be `null`");
        }
        this.handler = handler;
        if (priority < 0) {
            throw new IllegalArgumentException("`priority` must be positive");
        }
        this.priority = priority;
    }

    public Handler<RoutingContext> getHandler() {
        return handler;
    }

    public int getPriority() {
        return priority;
    }

    @Override
    public int compareTo(FilterBuildItem o) {
        return Integer.compare(o.getPriority(), this.getPriority());
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        FilterBuildItem that = (FilterBuildItem) o;
        return priority == that.priority &&
                handler.equals(that.handler);
    }

    @Override
    public int hashCode() {
        return Objects.hash(handler, priority);
    }
}
