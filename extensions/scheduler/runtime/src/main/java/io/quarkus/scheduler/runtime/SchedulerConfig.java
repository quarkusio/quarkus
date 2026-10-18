package io.quarkus.scheduler.runtime;

import com.cronutils.model.CronType;

import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;
import io.quarkus.scheduler.Scheduled;
import io.quarkus.scheduler.Scheduler;
import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithDefault;
import io.smallrye.config.WithName;

@ConfigRoot(phase = ConfigPhase.BUILD_AND_RUN_TIME_FIXED)
@ConfigMapping(prefix = "quarkus.scheduler")
public interface SchedulerConfig {

    /**
     * The syntax used in CRON expressions.
     *
     * @see Scheduled#cron()
     */
    @WithDefault("quartz")
    CronType cronType();

    /**
     * Scheduled task metrics will be enabled if a metrics extension is present and this value is true.
     */
    @WithName("metrics.enabled")
    @WithDefault("false")
    boolean metricsEnabled();

    /**
     * Controls whether tracing is enabled. If set to true and the OpenTelemetry extension is present,
     * tracing will be enabled, creating automatic Spans for each scheduled task.
     */
    @WithName("tracing.enabled")
    @WithDefault("false")
    boolean tracingEnabled();

    /**
     * By default, only one {@link Scheduler} implementation is used. If set to {@code true} then a composite {@link Scheduler}
     * that delegates to all running implementations is used.
     * <p>
     * Scheduler implementations will be started depending on the value of {@code quarkus.scheduler.start-mode}, i.e. the
     * scheduler is not started unless a relevant {@link io.quarkus.scheduler.Scheduled} business method is found.
     */
    @WithDefault("false")
    boolean useCompositeScheduler();

}
