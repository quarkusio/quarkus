package io.quarkus.extest.runtime.config;

import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;
import io.smallrye.config.ConfigMapping;

@ConfigMapping(prefix = "ignore.build-time.config")
@ConfigRoot(phase = ConfigPhase.RUN_TIME)
public interface IgnoreSharedRuntimeConfig {
    /** Docs */
    String runtime();
}
