package io.quarkus.quartz.runtime;

import java.time.Duration;
import java.util.Map;

import io.quarkus.runtime.annotations.ConfigDocMapKey;
import io.quarkus.runtime.annotations.ConfigDocSection;
import io.quarkus.runtime.annotations.ConfigGroup;
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
     * Defines how late the schedulers should be to be considered misfired.
     */
    @ConfigItem(defaultValue = "60")
    public Duration misfireThreshold;

    /**
     * Scheduler can be started in different modes: normal, forced or halted.
     * By default, the scheduler is not started unless a {@link io.quarkus.scheduler.Scheduled} business method
     * is found.
     * If set to "forced", scheduler will be started even if no scheduled business methods are found.
     * This is necessary for "pure" programmatic scheduling.
     * Additionally, setting it to "halted" will behave just like forced mode but the scheduler will not start
     * triggering jobs until an explicit start is called from the main scheduler.
     * This is useful to programmatically register listeners before scheduler starts performing some work.
     */
    @ConfigItem(defaultValue = "normal")
    public QuartzStartMode startMode;

    /**
     * Misfire policy per job configuration.
     */
    @ConfigDocSection
    @ConfigDocMapKey("identity")
    @ConfigItem(name = "misfire-policy")
    public Map<String, QuartzMisfirePolicyConfig> misfirePolicyPerJobs;

    @ConfigGroup
    public static class QuartzMisfirePolicyConfig {
        /**
         * The quartz misfire policy for this job.
         */
        @ConfigItem(defaultValue = "smart-policy", name = ConfigItem.PARENT)
        public QuartzMisfirePolicy misfirePolicy;
    }

}
