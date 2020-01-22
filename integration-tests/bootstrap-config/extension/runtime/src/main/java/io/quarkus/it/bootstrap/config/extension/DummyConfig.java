package io.quarkus.it.bootstrap.config.extension;

import io.quarkus.runtime.annotations.ConfigItem;
import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;

@ConfigRoot(phase = ConfigPhase.BOOTSTRAP)
public class DummyConfig {

    /**
     * dummy name
     */
    public String name;

    /**
     * dummy times
     */
    @ConfigItem(defaultValue = "2")
    public Integer times;
}
