package io.quarkus.scheduler.runtime;

import io.quarkus.runtime.annotations.ConfigItem;
import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;

@ConfigRoot(name = "scheduler", phase = ConfigPhase.RUN_TIME)
public class SchedulerRuntimeConfig {
    /**
     * The instance id of the scheduler.
     * This is required when running clustered schedulers as each node in the cluster MUST have a unique {@code instanceId}.
     * Defaults to `AUTO` to automatically generate unique ids for each node in the cluster
     */
    @ConfigItem(defaultValue = "AUTO")
    public String instanceId;

    /**
     * The size of scheduler thread pool. This will initialise the number of worker threads
     * in the pool
     */
    @ConfigItem(defaultValue = "25")
    public int threadCount;

    /**
     * Thread priority of worker threads in the pool.
     */
    @ConfigItem(defaultValue = "5")
    public int threadPriority;
}
