package io.quarkus.qrs.runtime;

import io.quarkus.runtime.annotations.ConfigItem;
import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;
import io.quarkus.runtime.configuration.MemorySize;

@ConfigRoot(phase = ConfigPhase.BUILD_AND_RUN_TIME_FIXED)
public class QrsConfig {

    /**
     * If this is set to true then requests are executed
     * on a worker thread by default.
     */
    @ConfigItem(defaultValue = "false")
    public boolean blocking = false;

    /**
     * The amount of memory that can be used to buffer input before switching to
     * blocking IO.
     *
     */
    @ConfigItem(defaultValue = "10k")
    public MemorySize inputBufferSize;

}
