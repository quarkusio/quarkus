package io.quarkus.load.shedding;

/**
 * Assigns a {@linkplain RequestPriority priority} to a request. All prioritizers
 * are inspected and the first one that returns {@code true} for {@link #appliesTo(Object)}
 * is taken.
 * <p>
 * If no prioritizer applies to a given request, the priority of {@link RequestPriority#NORMAL}
 * is assumed. By default, a prioritizer for non-application endpoints is present, which
 * assigns them the {@link RequestPriority#CRITICAL} priority.
 * <p>
 * An implementation must be a CDI bean, otherwise it is ignored. CDI typesafe resolution
 * rules must be followed. That is, if multiple implementations exist with a different
 * {@link jakarta.annotation.Priority} value and some of them are {@code @Alternative}s,
 * only the alternatives with the highest priority value are retained. If no implementation
 * is an alternative, all implementations are retained and are sorted in descending
 * {@code @Priority} order (highest priority value comes first).
 * <p>
 * Note that the built-in prioritizer for non-application endpoints declares no
 * {@code @Priority}, so an implementation that must be inspected before it has to declare
 * a higher {@code @Priority} value.
 * <p>
 * For HTTP requests, the type of the request ({@code R}) is {@link io.vertx.ext.web.RoutingContext}.
 *
 * @param <R> type of the request
 */
public interface RequestPrioritizer<R> {
    /**
     * Returns whether this request prioritizer applies to given {@code request}.
     *
     * @param request the request, never {@code null}
     * @return whether this request prioritizer applies to given {@code request}
     */
    boolean appliesTo(Object request);

    /**
     * Returns the priority of the given {@code request}.
     *
     * @param request the request, never {@code null}
     * @return the priority of the given {@code request}
     */
    RequestPriority priority(R request);
}
