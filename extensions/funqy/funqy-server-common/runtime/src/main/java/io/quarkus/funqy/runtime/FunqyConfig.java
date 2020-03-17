package io.quarkus.funqy.runtime;

import java.util.Optional;

import io.quarkus.runtime.annotations.ConfigItem;
import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;

@ConfigRoot(phase = ConfigPhase.BUILD_AND_RUN_TIME_FIXED)
public class FunqyConfig {

    /**
     * The function to export. If there is more than one function
     * defined for this deployment, then you must set this variable.
     * If there is only a single function, you do not have to set this config item.
     *
     */
    @ConfigItem
    public Optional<String> export;
}
