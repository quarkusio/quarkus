package io.quarkus.runtime.logging;

import io.quarkus.runtime.annotations.ConfigItem;
import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;

@ConfigRoot(name = "log", phase = ConfigPhase.BUILD_AND_RUN_TIME_FIXED)
public class LogBuildTimeConfig {

    /**
     * Whether or not logging metrics are published in case a metrics extension is present.
     */
    @ConfigItem(name = "metrics.enabled", defaultValue = "false")
    public boolean metricsEnabled;
}
