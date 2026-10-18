package io.quarkus.smallrye.health.runtime;

import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;
import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithDefault;

@ConfigRoot(phase = ConfigPhase.BUILD_AND_RUN_TIME_FIXED)
@ConfigMapping(prefix = "quarkus.smallrye-health")
public interface SmallRyeHealthBuildFixedConfig {

    /**
     * Specify the reported DOWN responses should be aligned with RFC 9457 - Problem Details for HTTP APIs.
     *
     * @see <a href="https://tools.ietf.org/html/rfc7807">RFC 7807</a>
     */
    @WithDefault("false")
    boolean includeProblemDetails();
}
