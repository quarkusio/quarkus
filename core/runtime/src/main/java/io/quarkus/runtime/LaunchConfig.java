package io.quarkus.runtime;

import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;
import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithDefault;

/**
 * Launch.
 */
@ConfigMapping(prefix = "quarkus.launch")
@ConfigRoot(phase = ConfigPhase.RUN_TIME)
public interface LaunchConfig {

    /**
     * If set to true, Quarkus will perform re-augmentation (assuming the {@code mutable-jar} package type is used)
     */
    @WithDefault("false")
    boolean rebuild();
}
