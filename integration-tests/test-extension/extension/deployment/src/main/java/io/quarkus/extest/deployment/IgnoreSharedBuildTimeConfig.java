package io.quarkus.extest.deployment;

import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;
import io.smallrye.config.ConfigMapping;

@ConfigMapping(prefix = "ignore.build-time.config")
@ConfigRoot(phase = ConfigPhase.BUILD_TIME)
public interface IgnoreSharedBuildTimeConfig {
    /** Docs */
    String buildTime();
}
