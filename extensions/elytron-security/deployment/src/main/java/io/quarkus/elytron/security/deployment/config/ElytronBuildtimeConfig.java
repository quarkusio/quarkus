package io.quarkus.elytron.security.runtime.config;

import io.quarkus.runtime.annotations.ConfigItem;
import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;

@ConfigRoot(name = "security", phase = ConfigPhase.BUILD_TIME)
public class ElytronBuildtimeConfig {

    /**
     * If the role mapper is enabled
     */
    @ConfigItem(defaultValue = "false")
    public Boolean roleMapperEnabled;
}
