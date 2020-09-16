package io.quarkus.quartz.runtime;

import io.quarkus.runtime.annotations.ConfigItem;
import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;

@ConfigRoot(phase = ConfigPhase.RUN_TIME)
public class QuartzRuntimeConfig {

    /**
     * The name of the Quartz instance.
     */
    @ConfigItem(defaultValue = "QuarkusQuartzScheduler")
    public String instanceName;

    /**
     * The size of scheduler thread pool. This will initialize the number of worker threads in the pool.
     */
    @ConfigItem(defaultValue = "25")
    public int threadCount;

    /**
     * Thread priority of worker threads in the pool.
     */
    @ConfigItem(defaultValue = "5")
    public int threadPriority;

    /**
     * By default, the scheduler is not started unless a {@link io.quarkus.scheduler.Scheduled} business method is found.
     * If set to true the scheduler will be started even if no scheduled business methods are found. This is necessary for
     * "pure" programmatic scheduling.
     */
    @ConfigItem
    public boolean forceStart;

}
