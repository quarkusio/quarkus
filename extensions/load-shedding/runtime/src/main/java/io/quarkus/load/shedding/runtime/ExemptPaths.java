package io.quarkus.load.shedding.runtime;

import java.util.Set;

import io.vertx.ext.web.RoutingContext;

/**
 * Paths that are never subject to load shedding, whether priority load shedding is enabled or not,
 * such as the health probes and the metrics endpoint.
 */
public record ExemptPaths(Set<String> paths) {

    public boolean contains(RoutingContext ctx) {
        return paths.contains(ctx.normalizedPath());
    }
}
