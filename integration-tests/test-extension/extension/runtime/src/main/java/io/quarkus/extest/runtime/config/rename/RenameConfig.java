package io.quarkus.extest.runtime.config.rename;

import io.quarkus.runtime.annotations.ConfigItem;
import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;

@ConfigRoot(phase = ConfigPhase.BUILD_AND_RUN_TIME_FIXED)
public class RenameConfig {
    /**
     *
     */
    @ConfigItem
    public String prop;
    /**
     *
     */
    @ConfigItem
    public String onlyInNew;
    /**
     *
     */
    @ConfigItem
    public String onlyInOld;
    /**
     *
     */
    @ConfigItem
    public String inBoth;
    /**
     *
     */
    @ConfigItem(defaultValue = "default")
    public String withDefault;
}
