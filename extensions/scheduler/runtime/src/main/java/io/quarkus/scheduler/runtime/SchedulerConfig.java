package io.quarkus.scheduler.runtime;

import com.cronutils.model.CronType;

import io.quarkus.runtime.annotations.ConfigItem;
import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;
import io.quarkus.scheduler.Scheduled;
import io.quarkus.scheduler.Scheduler;

@ConfigRoot(phase = ConfigPhase.BUILD_AND_RUN_TIME_FIXED)
public class SchedulerConfig {

    /**
     * The syntax used in CRON expressions.
     *
     * @see Scheduled#cron()
     */
    @ConfigItem(defaultValue = "quartz")
    public CronType cronType;

    /**
     * Scheduled task metrics will be enabled if a metrics extension is present and this value is true.
     */
    @ConfigItem(name = "metrics.enabled")
    public boolean metricsEnabled;

    /**
     * Controls whether tracing is enabled. If set to true and the OpenTelemetry extension is present,
     * tracing will be enabled, creating automatic Spans for each scheduled task.
     */
    @ConfigItem(name = "tracing.enabled")
    public boolean tracingEnabled;

    /**
     * By default, only one {@link Scheduler} implementation is used. If set to {@code true} then a composite {@link Scheduler}
     * that delegates to all running implementations is used.
     * <p>
     * Scheduler implementations will be started depending on the value of {@code quarkus.scheduler.start-mode}, i.e. the
     * scheduler is not started unless a relevant {@link io.quarkus.scheduler.Scheduled} business method is found.
     */
    @ConfigItem(defaultValue = "false")
    public boolean useCompositeScheduler;

}
