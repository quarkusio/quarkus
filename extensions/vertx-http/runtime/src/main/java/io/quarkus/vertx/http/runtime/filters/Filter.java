package io.quarkus.vertx.http.runtime.filters;

import io.vertx.core.Handler;
import io.vertx.ext.web.RoutingContext;

/**
 * Represents a Filter, i.e. a route called on every HTTP request.
 * The priority attribute allows sorting the filters. Highest priority are called first.
 */
public interface Filter {

    /**
     * The handler called on HTTP request.
     * It's important that the handler call {@link RoutingContext#next()} to invoke the next filter or the user routes.
     *
     * @return the handler
     */
    Handler<RoutingContext> getHandler();

    /**
     * @return the priority of the filter.
     */
    int getPriority();

}
