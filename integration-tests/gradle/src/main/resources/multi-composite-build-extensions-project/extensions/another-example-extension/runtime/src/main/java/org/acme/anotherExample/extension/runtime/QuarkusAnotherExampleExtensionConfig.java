package org.acme.anotherExample.extension.runtime;

import io.quarkus.runtime.annotations.ConfigItem;
import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;

@ConfigRoot(phase = ConfigPhase.RUN_TIME, name = "anotherExample.extension")
public class QuarkusAnotherExampleExtensionConfig {

    /**
     * A Simple example flag
     */
    @ConfigItem(name = "enabled", defaultValue = "false")
    public boolean enabled;

}
