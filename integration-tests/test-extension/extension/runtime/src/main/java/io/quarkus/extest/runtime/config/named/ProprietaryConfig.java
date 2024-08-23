package io.quarkus.extest.runtime.config.named;

import io.quarkus.runtime.annotations.ConfigItem;
import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;

@ConfigRoot(prefix = "proprietary", phase = ConfigPhase.BUILD_AND_RUN_TIME_FIXED, name = "root.config")
public class ProprietaryConfig {
    /** */
    @ConfigItem
    public String value;
}
