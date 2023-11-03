package io.quarkus.extest.runtime.config;

import io.quarkus.runtime.annotations.ConfigItem;
import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;
import io.smallrye.config.ConfigValue;

@ConfigRoot(name = "bt", phase = ConfigPhase.RUN_TIME)
public class TestShadowBuildTimeToRunTimeConfig {
    /** */
    @ConfigItem
    public ConfigValue btConfigValue;
}
