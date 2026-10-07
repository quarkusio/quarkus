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

    /**
     * When enabled, scheduled methods that would otherwise be executed on a worker thread, meaning blocking
     * scheduled methods without an explicit {@code @Blocking}, {@code @NonBlocking} or {@code @RunOnVirtualThread}
     * annotation, are executed on virtual threads instead.
     * <p>
     * Scheduled methods considered non-blocking, for instance because they return {@code CompletionStage} or
     * {@code Uni}, keep running on the event loop, and explicit annotations on the scheduled method always take
     * precedence.
     * <p>
     * Before enabling this, make sure the application does not suffer from thread pinning (mostly resolved since
     * JDK 24) or carrier thread monopolization. See the virtual threads guide for more details on how to monitor
     * this with JFR events.
     */
    @WithDefault("false")
    boolean virtualThreads();

}
