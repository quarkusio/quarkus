package io.quarkus.vertx.http.runtime.filters;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import io.vertx.core.Handler;
import io.vertx.ext.web.RoutingContext;

/**
 * Object allowing the register filters, i.e. handler called on every HTTP request.
 * This object is intended to be used as follows:
 * 
 * <pre>
 * {@code
 * public void init(@Observes Filters filters) {
 *      filters.register(rc -> {
 *          // Do something before the next filter or route
 *          rc.next();
 *         // Do something after
 *      }, 10);
 * }
 * }
 * </pre>
 * <p>
 * The handler is the filter code. It must call {@link RoutingContext#next()} to invoke the next filter or route.
 * The priority is used to sort the filters. Highest priorities are called first.
 */
public class Filters {

    private List<Filter> listOfFilters = new CopyOnWriteArrayList<>();

    /**
     * Registers a new filter.
     *
     * @param handler the filter function, must not be {@code null}
     * @param priority the priority, must not be negative
     * @return this object to chain registration.
     */
    public Filters register(Handler<RoutingContext> handler, int priority) {
        listOfFilters.add(new SimpleFilter(handler, priority));
        return this;
    }

    /**
     * @return the list of currently registered filters.
     */
    public List<Filter> getFilters() {
        return new ArrayList<>(listOfFilters);
    }

    /**
     * Simple implementation of filter.
     */
    public static class SimpleFilter implements Filter {

        private Handler<RoutingContext> handler;
        private int priority;

        @SuppressWarnings("unused")
        public SimpleFilter() {
            // Default constructor.
        }

        public SimpleFilter(Handler<RoutingContext> handler, int priority) {
            if (priority < 0) {
                throw new IllegalArgumentException("`priority` cannot be negative");
            }
            this.handler = handler;
            this.priority = priority;
        }

        public void setHandler(Handler<RoutingContext> handler) {
            this.handler = handler;
        }

        public void setPriority(int priority) {
            this.priority = priority;
        }

        @Override
        public Handler<RoutingContext> getHandler() {
            return handler;
        }

        @Override
        public int getPriority() {
            return priority;
        }

    }
}
