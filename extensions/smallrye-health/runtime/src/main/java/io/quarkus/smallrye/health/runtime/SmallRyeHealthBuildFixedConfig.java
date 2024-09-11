package io.quarkus.smallrye.health.runtime;

import io.quarkus.runtime.annotations.ConfigItem;
import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;

@ConfigRoot(name = "smallrye-health", phase = ConfigPhase.BUILD_AND_RUN_TIME_FIXED)
public class SmallRyeHealthBuildFixedConfig {

    /**
     * Specify the reported DOWN responses should be aligned with RFC 9457 - Problem Details for HTTP APIs.
     *
     * @see <a href="https://tools.ietf.org/html/rfc7807">RFC 7807</a>
     */
    @ConfigItem(defaultValue = "false")
    boolean includeProblemDetails;
}
