package io.quarkus.resteasy.reactive.common.runtime;

import io.quarkus.runtime.annotations.ConfigItem;
import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;
import io.quarkus.runtime.configuration.MemorySize;

@ConfigRoot(phase = ConfigPhase.BUILD_AND_RUN_TIME_FIXED, name = "rest")
public class QuarkusRestConfig {

    /**
     * The amount of memory that can be used to buffer input before switching to
     * blocking IO.
     */
    @ConfigItem(defaultValue = "10k")
    public MemorySize inputBufferSize;

    /**
     * By default we assume a default produced media type of "text/plain"
     * for String endpoint return types. If this is disabled, the default
     * produced media type will be "[text/plain, *&sol;*]" which is more
     * expensive due to negotiation.
     */
    @ConfigItem(defaultValue = "true")
    public boolean singleDefaultProduces;
}
