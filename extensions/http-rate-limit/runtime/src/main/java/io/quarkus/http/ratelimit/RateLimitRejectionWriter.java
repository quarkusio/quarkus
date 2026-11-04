package io.quarkus.http.ratelimit;

import io.vertx.ext.web.RoutingContext;

/**
 * Writes every 429 or 503 response produced by the rate limiter. Use {@link RateLimitRejectionInfo#statusCode()}
 * for the status and {@link RateLimitRejectionInfo#reason()} to distinguish an exceeded quota from a failed lookup
 * or an unavailable store. It must set the status and end the response. Runs on the
 * event loop; must not block.
 * <p>
 * The {@code Retry-After} header is already set, and the request body is being drained. If it throws before ending
 * the response, the request fails with that exception.
 */
public interface RateLimitRejectionWriter {

    void write(RoutingContext context, RateLimitRejectionInfo rejection);
}
