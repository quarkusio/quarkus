package org.acme.example.extension.runtime;

import io.quarkus.runtime.annotations.ConfigItem;
import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;

@ConfigRoot(phase = ConfigPhase.RUN_TIME)
public class ExampleRuntimeConfig {

    /**
     * Whether the banner will be displayed
     */
    @ConfigItem(defaultValue = "none")
    public String runtimeName;

}