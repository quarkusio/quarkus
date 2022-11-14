package io.quarkus.vertx.http.deployment;

import io.quarkus.builder.item.MultiBuildItem;
import io.quarkus.vertx.http.runtime.filters.Filter;
import io.quarkus.vertx.http.runtime.filters.Filters;
import io.vertx.core.Handler;
import io.vertx.ext.web.RoutingContext;

/**
 * A handler that is applied to every route
 */
public final class FilterBuildItem extends MultiBuildItem {

    //predefined system priorities
    public static final int CORS = 300;
    public static final int AUTHENTICATION = 200;
    public static final int AUTHORIZATION = 100;

    private final Handler<RoutingContext> handler;
    private final int priority;
    private final boolean isFailureHandler;

    /**
     * Creates a new instance of {@link FilterBuildItem}.
     *
     * @param handler the handler, if {@code null} the filter won't be used.
     * @param priority the priority, higher priority gets invoked first. Priority is only used to sort filters, user
     *        routes are called afterwards. Must be positive.
     */
    public FilterBuildItem(Handler<RoutingContext> handler, int priority) {
        this.handler = handler;
        checkPriority(priority);
        this.priority = priority;
        this.isFailureHandler = false;
    }

    /**
     * Creates a new instance of {@link FilterBuildItem}.
     *
     * @param handler the handler, if {@code null} the filter won't be used.
     * @param priority the priority, higher priority gets invoked first. Priority is only used to sort filters, user
     *        routes are called afterwards. Must be positive.
     * @param isFailureHandler whether an HTTP request or failure should be routed to a handler.
     */
    public FilterBuildItem(Handler<RoutingContext> handler, int priority, boolean isFailureHandler) {
        this.handler = handler;
        checkPriority(priority);
        this.priority = priority;
        this.isFailureHandler = isFailureHandler;
    }

    private void checkPriority(int priority) {
        if (priority < 0) {
            throw new IllegalArgumentException("`priority` must be positive");
        }
    }

    public Handler<RoutingContext> getHandler() {
        return handler;
    }

    public int getPriority() {
        return priority;
    }

    public boolean isFailureHandler() {
        return isFailureHandler;
    }

    /**
     * @return a filter object wrapping the handler and priority.
     */
    public Filter toFilter() {
        return new Filters.SimpleFilter(handler, priority, isFailureHandler);
    }

}
