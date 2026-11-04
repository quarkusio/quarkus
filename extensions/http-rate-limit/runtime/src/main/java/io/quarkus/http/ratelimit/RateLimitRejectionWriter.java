package io.quarkus.http.ratelimit;

import io.vertx.ext.web.RoutingContext;

/**
 * Writes the response for a request that a rule rejected; it must set the status and end the response. Runs on the
 * event loop; must not block.
 * <p>
 * The {@code Retry-After} header is already set, and the request body is being drained. If it throws before ending
 * the response, the request fails with that exception.
 */
public interface RateLimitRejectionWriter {

    void write(RoutingContext context, RateLimitRejectionInfo rejection);
}
