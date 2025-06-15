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

    // predefined system priorities
    public static final int CORS = 300;
    public static final int AUTHENTICATION = 200;
    public static final int AUTHORIZATION = 100;
    private static final int AUTH_FAILURE_HANDLER = Integer.MIN_VALUE + 1;

    private final Handler<RoutingContext> handler;
    private final int priority;
    private final boolean isFailureHandler;

    /**
     * Creates a new instance of {@link FilterBuildItem}.
     *
     * @param handler
     *        the handler, if {@code null} the filter won't be used.
     * @param priority
     *        the priority, higher priority gets invoked first. Priority is only used to sort filters, user routes
     *        are called afterwards. Must be positive.
     */
    public FilterBuildItem(Handler<RoutingContext> handler, int priority) {
        this.handler = handler;
        checkPriority(priority);
        this.priority = priority;
        this.isFailureHandler = false;
    }

    private FilterBuildItem(Handler<RoutingContext> handler, int priority, boolean checkPriority,
            boolean isFailureHandler) {
        this.handler = handler;
        if (checkPriority) {
            checkPriority(priority);
        }
        this.priority = priority;
        this.isFailureHandler = isFailureHandler;
    }

    /**
     * Creates a new instance of {@link FilterBuildItem} with an authentication failure handler.
     *
     * @param authFailureHandler
     *        authentication failure handler
     */
    private FilterBuildItem(Handler<RoutingContext> authFailureHandler) {
        this.handler = authFailureHandler;
        this.isFailureHandler = true;
        this.priority = AUTH_FAILURE_HANDLER;
    }

    /**
     * Creates a new instance of {@link FilterBuildItem} with an authentication failure handler. The handler will be
     * added as next to last, right before {@link io.quarkus.vertx.http.runtime.QuarkusErrorHandler}.
     */
    public static FilterBuildItem ofAuthenticationFailureHandler(Handler<RoutingContext> authFailureHandler) {
        return new FilterBuildItem(authFailureHandler);
    }

    /**
     * Creates a new instance of {@link FilterBuildItem} with an authentication failure handler. The handler will be
     * added right before any handlers added by {@link FilterBuildItem#ofAuthenticationFailureHandler(Handler)}
     */
    public static FilterBuildItem ofPreAuthenticationFailureHandler(Handler<RoutingContext> authFailureHandler) {
        return new FilterBuildItem(authFailureHandler, AUTH_FAILURE_HANDLER + 1, false, true);
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
        if (isFailureHandler && priority == AUTH_FAILURE_HANDLER) {
            // create filter for penultimate auth failure handler
            final Filters.SimpleFilter filter = new Filters.SimpleFilter();
            filter.setPriority(AUTH_FAILURE_HANDLER);
            filter.setFailureHandler(true);
            filter.setHandler(handler);
            return filter;
        } else {
            return new Filters.SimpleFilter(handler, priority, isFailureHandler);
        }
    }

}
