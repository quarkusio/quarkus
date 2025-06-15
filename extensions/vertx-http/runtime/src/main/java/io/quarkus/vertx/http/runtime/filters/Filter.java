package io.quarkus.vertx.http.runtime.filters;

import io.vertx.core.Handler;
import io.vertx.ext.web.Route;
import io.vertx.ext.web.RoutingContext;

/**
 * Represents a Filter, i.e. a route called on every HTTP request or failure (depending on {@link #isFailureHandler()}).
 * The priority attribute allows sorting the filters. Highest priority are called first.
 */
public interface Filter {

    /**
     * The handler called on HTTP request or failure. It's important that the handler call {@link RoutingContext#next()}
     * to invoke the next filter or the user routes.
     *
     * @return the handler
     */
    Handler<RoutingContext> getHandler();

    /**
     * @return the priority of the filter.
     */
    int getPriority();

    /**
     * Whether to add {@link #getHandler()} as HTTP request handler (via {@link Route#handler(Handler)}) or as failure
     * handler (via {@link Route#failureHandler(Handler)}).
     *
     * @return true if filter should be applied on failures rather than HTTP requests
     */
    default boolean isFailureHandler() {
        return false;
    }

}
