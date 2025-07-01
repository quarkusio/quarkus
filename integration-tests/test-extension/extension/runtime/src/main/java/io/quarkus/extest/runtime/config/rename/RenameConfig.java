package io.quarkus.extest.runtime.config.rename;

import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;
import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithDefault;

@ConfigMapping(prefix = "quarkus.rename")
@ConfigRoot(phase = ConfigPhase.BUILD_AND_RUN_TIME_FIXED)
public interface RenameConfig {
    /**
     *
     */
    String prop();

    /**
     *
     */
    String onlyInNew();

    /**
     *
     */
    String onlyInOld();

    /**
     *
     */
    String inBoth();

    /**
     *
     */
    @WithDefault("default")
    String withDefault();
}
