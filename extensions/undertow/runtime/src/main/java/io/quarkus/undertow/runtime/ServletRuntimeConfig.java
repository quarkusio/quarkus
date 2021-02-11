package io.quarkus.undertow.runtime;

import java.util.Optional;

import io.quarkus.runtime.annotations.ConfigItem;
import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;
import io.quarkus.runtime.configuration.MemorySize;

@ConfigRoot(phase = ConfigPhase.RUN_TIME, name = "servlet")
public class ServletRuntimeConfig {

    /**
     * The buffer size to use for Servlet. If this is not specified the default will depend on the amount
     * of available memory. If there is less than 64mb it will default to 512b heap buffer, less that 128mb
     * 1k direct buffer and otherwise 16k direct buffers.
     *
     */
    @ConfigItem
    Optional<MemorySize> bufferSize;

    /**
     * If Servlet should use direct buffers, this gives maximum performance but can be problematic
     * in memory constrained environments
     */
    @ConfigItem
    Optional<Boolean> directBuffers;

}
