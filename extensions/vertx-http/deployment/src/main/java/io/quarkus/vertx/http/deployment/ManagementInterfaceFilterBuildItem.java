package io.quarkus.vertx.http.deployment;

import io.quarkus.builder.item.MultiBuildItem;
import io.quarkus.vertx.http.runtime.filters.Filter;
import io.quarkus.vertx.http.runtime.filters.Filters;
import io.vertx.core.Handler;
import io.vertx.ext.web.RoutingContext;

/**
 * A handler that is applied to the management interface route
 */
public final class ManagementInterfaceFilterBuildItem extends MultiBuildItem {

    // predefined system priorities
    public static final int AUTHENTICATION = -200;
    public static final int AUTHORIZATION = -100;

    private final Handler<RoutingContext> handler;
    private final int priority;

    /**
     * Creates a new instance of {@link ManagementInterfaceFilterBuildItem}.
     *
     * @param handler
     *        the handler, if {@code null} the filter won't be used.
     * @param priority
     *        the priority, higher priority gets invoked first. Priority is only used to sort filters, user routes
     *        are called afterwards. Must be positive.
     */
    public ManagementInterfaceFilterBuildItem(Handler<RoutingContext> handler, int priority) {
        this.handler = handler;
        this.priority = priority;
    }

    public Handler<RoutingContext> getHandler() {
        return handler;
    }

    public int getPriority() {
        return priority;
    }

    /**
     * @return a filter object wrapping the handler and priority.
     */
    public Filter toFilter() {
        return new Filters.SimpleFilter(handler, priority, false);
    }

}
