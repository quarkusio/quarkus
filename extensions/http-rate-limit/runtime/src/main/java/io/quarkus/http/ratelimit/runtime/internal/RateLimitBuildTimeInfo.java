package io.quarkus.http.ratelimit.runtime.internal;

import java.util.Set;

/**
 * What the rate limiter needs to know from build time.
 *
 * @param controlPlanePaths exact paths that are never rate limited, such as the health probes
 */
public record RateLimitBuildTimeInfo(Set<String> controlPlanePaths) {
}
