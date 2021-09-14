package io.quarkus.smallrye.reactivemessaging.runtime;

import io.quarkus.runtime.annotations.ConfigItem;
import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;

@ConfigRoot(phase = ConfigPhase.BUILD_AND_RUN_TIME_FIXED)
public class ReactiveMessagingConfiguration {

    /**
     * Whether or not Reactive Messaging metrics are published in case a metrics extension is present
     * (default to false).
     */
    @ConfigItem(name = "metrics.enabled")
    public boolean metricsEnabled;

    /**
     * Enables or disables the strict validation mode.
     */
    @ConfigItem(name = "strict", defaultValue = "false")
    public boolean strict;
}
