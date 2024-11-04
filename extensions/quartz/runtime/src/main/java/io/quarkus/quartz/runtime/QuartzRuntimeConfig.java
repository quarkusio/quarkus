package io.quarkus.quartz.runtime;

import java.time.Duration;
import java.util.Map;
import java.util.Optional;

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
     * The identifier of Quartz instance that must be unique for all schedulers working as if they are the same
     * <em>logical</em> Scheduler within a cluster. Use the default value {@code AUTO} or some of the configured
     * <a href="https://quarkus.io/guides/quartz#quarkus-quartz_quarkus.quartz.instance-id-generators-instance-id-generators">
     * instance ID generators</a> if you wish the identifier to be generated for you.
     */
    @ConfigItem(defaultValue = "AUTO")
    public String instanceId;

    /**
     * The amount of time in milliseconds that a trigger is allowed to be acquired and fired ahead of its scheduled fire time.
     */
    @ConfigItem(defaultValue = "0")
    public long batchTriggerAcquisitionFireAheadTimeWindow;

    /**
     * The maximum number of triggers that a scheduler node is allowed to acquire (for firing) at once.
     */
    @ConfigItem(defaultValue = "1")
    public int batchTriggerAcquisitionMaxCount;
    /**
     * The size of scheduler thread pool. This will initialize the number of worker threads in the pool.
     * <p>
     * It's important to bear in mind that Quartz threads are not used to execute scheduled methods, instead the regular Quarkus
     * thread pool is used by default. See also {@code quarkus.quartz.run-blocking-scheduled-method-on-quartz-thread}.
     */
    @ConfigItem(defaultValue = "10")
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
     *
     * @deprecated Use {@code quarkus.scheduler.start-mode} instead.
     */
    @ConfigItem
    @Deprecated
    public Optional<QuartzStartMode> startMode;

    /**
     * The maximum amount of time Quarkus will wait for currently running jobs to finish.
     * If the value is {@code 0}, then Quarkus will not wait at all for these jobs to finish
     * - it will call {@code org.quartz.Scheduler.shutdown(false)} in this case.
     */
    @ConfigItem(defaultValue = "10")
    public Duration shutdownWaitTime;

    /**
     * Simple trigger default configuration
     */
    @ConfigItem(name = "simple-trigger")
    public TriggerConfig simpleTriggerConfig = TriggerConfig.defaultConfig();

    /**
     * Cron trigger default configuration
     */
    @ConfigItem(name = "cron-trigger")
    public TriggerConfig cronTriggerConfig = TriggerConfig.defaultConfig();

    /**
     * Misfire policy per job configuration.
     */
    @ConfigDocSection
    @ConfigDocMapKey("identity")
    @ConfigItem(name = "misfire-policy")
    public Map<String, QuartzMisfirePolicyConfig> misfirePolicyPerJobs;

    /**
     * Properties that should be passed on directly to Quartz.
     * Use the full configuration property key here,
     * for instance {@code `quarkus.quartz.unsupported-properties."org.quartz.scheduler.jmx.export" = true`)}.
     *
     * <p>
     * Properties set here are completely unsupported:
     * as Quarkus doesn't generally know about these properties and their purpose,
     * there is absolutely no guarantee that they will work correctly,
     * and even if they do, that may change when upgrading to a newer version of Quarkus
     * (even just a micro/patch version).
     * <p>
     * Consider using a supported configuration property before falling back to unsupported ones.
     * If none exists, make sure to file a feature request so that a supported configuration property can be added to Quarkus,
     * and more importantly so that the configuration property is tested regularly.
     */
    @ConfigDocMapKey("full-property-key")
    public Map<String, String> unsupportedProperties;

    /**
     * When set to {@code true}, blocking scheduled methods are invoked on a thread managed by Quartz instead of a
     * thread from the regular Quarkus thread pool (default).
     * <p>
     * When this option is enabled, blocking scheduled methods do not run on a {@code duplicated context}.
     */
    @ConfigItem(defaultValue = "false")
    public boolean runBlockingScheduledMethodOnQuartzThread;

    @ConfigGroup
    public static class QuartzMisfirePolicyConfig {
        /**
         * The quartz misfire policy for this job.
         */
        @ConfigItem(defaultValue = "smart-policy", name = ConfigItem.PARENT)
        public QuartzMisfirePolicy misfirePolicy;

        public static QuartzMisfirePolicyConfig defaultConfig() {
            return new QuartzMisfirePolicyConfig();
        }
    }

    @ConfigGroup
    public static class TriggerConfig {

        /**
         * Misfire policy configuration
         * Defaults to smart-policy
         */
        @ConfigItem(name = "misfire-policy")
        public QuartzMisfirePolicyConfig misfirePolicyConfig = QuartzMisfirePolicyConfig.defaultConfig();

        public static TriggerConfig defaultConfig() {
            return new TriggerConfig();
        }
    }

}
