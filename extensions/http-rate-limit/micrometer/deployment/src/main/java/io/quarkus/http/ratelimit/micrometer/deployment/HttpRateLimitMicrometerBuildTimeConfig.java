package io.quarkus.http.ratelimit.micrometer.deployment;

import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;
import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithDefault;
import io.smallrye.config.WithName;

@ConfigMapping(prefix = "quarkus.http.rate-limit")
@ConfigRoot(phase = ConfigPhase.BUILD_TIME)
public interface HttpRateLimitMicrometerBuildTimeConfig {
    /**
     * Whether metrics about rate limiting should be published to Micrometer.
     */
    @WithDefault("true")
    @WithName("metrics.enabled")
    boolean metricsEnabled();
}
