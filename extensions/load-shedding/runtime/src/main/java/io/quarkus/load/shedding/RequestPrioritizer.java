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
 * rules must be followed. That is, if multiple implementations are provided with different
 * {@link jakarta.annotation.Priority} values, only the implementations with the highest
 * priority are retained.
 * <p>
 * For HTTP requests, the type of the request ({@code R}) is {@link io.vertx.ext.web.RoutingContext}.
 *
 * @param <R> type of the request
 */
public interface RequestPrioritizer<R> {
    boolean appliesTo(Object request);

    RequestPriority priority(R request);
}
