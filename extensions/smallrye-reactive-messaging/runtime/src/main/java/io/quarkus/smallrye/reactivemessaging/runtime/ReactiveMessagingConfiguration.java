package io.quarkus.smallrye.reactivemessaging.runtime;

import io.quarkus.runtime.annotations.ConfigItem;
import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;

@ConfigRoot(phase = ConfigPhase.BUILD_TIME)
public class ReactiveMessagingConfiguration {

    /**
     * Enables or disables the <em>strict</em> mode.
     */
    @ConfigItem(defaultValue = "false")
    public boolean strict;
}
