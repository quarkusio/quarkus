package io.quarkus.quartz.runtime;

import java.time.Duration;
import java.util.Map;
import java.util.Optional;

import io.quarkus.runtime.annotations.ConfigDocMapKey;
import io.quarkus.runtime.annotations.ConfigDocSection;
import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;
import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithDefault;
import io.smallrye.config.WithName;
import io.smallrye.config.WithParentName;

@ConfigRoot(phase = ConfigPhase.RUN_TIME)
@ConfigMapping(prefix = "quarkus.quartz")
public interface QuartzRuntimeConfig {

    /**
     * The name of the Quartz instance.
     */
    @WithDefault("QuarkusQuartzScheduler")
    String instanceName();

    /**
     * The identifier of Quartz instance that must be unique for all schedulers working as if they are the same
     * <em>logical</em> Scheduler within a cluster. Use the default value {@code AUTO} or some of the configured
     * <a href="https://quarkus.io/guides/quartz#quarkus-quartz_quarkus.quartz.instance-id-generators-instance-id-generators">
     * instance ID generators</a> if you wish the identifier to be generated for you.
     */
    @WithDefault("AUTO")
    String instanceId();

    /**
     * The amount of time in milliseconds that a trigger is allowed to be acquired and fired ahead of its scheduled fire time.
     */
    @WithDefault("0")
    long batchTriggerAcquisitionFireAheadTimeWindow();

    /**
     * The maximum number of triggers that a scheduler node is allowed to acquire (for firing) at once.
     */
    @WithDefault("1")
    int batchTriggerAcquisitionMaxCount();

    /**
     * The size of scheduler thread pool. This will initialize the number of worker threads in the pool.
     * <p>
     * It's important to bear in mind that Quartz threads are not used to execute scheduled methods, instead the regular Quarkus
     * thread pool is used by default. See also {@code quarkus.quartz.run-blocking-scheduled-method-on-quartz-thread}.
     */
    @WithDefault("10")
    int threadCount();

    /**
     * Thread priority of worker threads in the pool.
     */
    @WithDefault("5")
    int threadPriority();

    /**
     * Defines how late the schedulers should be to be considered misfired.
     */
    @WithDefault("60")
    Duration misfireThreshold();

    /**
     * The maximum amount of time Quarkus will wait for currently running jobs to finish.
     * If the value is {@code 0}, then Quarkus will not wait at all for these jobs to finish
     * - it will call {@code org.quartz.Scheduler.shutdown(false)} in this case.
     */
    @WithDefault("10")
    Duration shutdownWaitTime();

    /**
     * Simple trigger default configuration
     */
    @WithName("simple-trigger")
    TriggerConfig simpleTriggerConfig();

    /**
     * Cron trigger default configuration
     */
    @WithName("cron-trigger")
    TriggerConfig cronTriggerConfig();

    /**
     * Misfire policy per job configuration.
     */
    @ConfigDocSection
    @ConfigDocMapKey("identity")
    @WithName("misfire-policy")
    Map<String, QuartzMisfirePolicyConfig> misfirePolicyPerJobs();

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
    Map<String, String> unsupportedProperties();

    /**
     * When set to {@code true}, blocking scheduled methods are invoked on a thread managed by Quartz instead of a
     * thread from the regular Quarkus thread pool (default).
     * <p>
     * When this option is enabled, blocking scheduled methods do not run on a {@code duplicated context}.
     */
    @WithDefault("false")
    boolean runBlockingScheduledMethodOnQuartzThread();

    /**
     * The name of the datasource to use.
     * <p>
     * This property in valid only in combination with {@link QuartzBuildTimeConfig#deferDatasourceCheck()}
     */
    Optional<String> deferredDatasourceName();

    interface QuartzMisfirePolicyConfig {
        /**
         * The quartz misfire policy for this job.
         */
        @WithDefault("smart-policy")
        @WithParentName
        QuartzMisfirePolicy misfirePolicy();

    }

    interface TriggerConfig {

        /**
         * Misfire policy configuration
         * Defaults to smart-policy
         */
        @WithName("misfire-policy")
        QuartzMisfirePolicyConfig misfirePolicyConfig();
    }

}
