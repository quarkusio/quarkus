package io.quarkus.extest.runtime.config;

import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;
import io.smallrye.config.ConfigMapping;
import io.smallrye.config.ConfigValue;

@ConfigMapping(prefix = "quarkus.bt")
@ConfigRoot(phase = ConfigPhase.RUN_TIME)
public interface TestShadowBuildTimeToRunTimeConfig {
    /** */
    ConfigValue btConfigValue();
}
