package io.quarkus.arc.deployment;

import io.quarkus.runtime.annotations.ConfigGroup;
import io.quarkus.runtime.annotations.ConfigItem;

@ConfigGroup
public class ArcContextPropagationConfig {

    /**
     * If set to true and SmallRye Context Propagation extension is present then enable the context propagation for CDI
     * contexts.
     */
    @ConfigItem(defaultValue = "true")
    public boolean enabled;

}
