package io.quarkus.quartz.runtime;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.OptionalLong;
import java.util.Properties;
import java.util.Set;
import java.util.TimeZone;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import jakarta.annotation.PreDestroy;
import jakarta.annotation.Priority;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.context.BeforeDestroyed;
import jakarta.enterprise.event.Event;
import jakarta.enterprise.event.Observes;
import jakarta.enterprise.event.Reception;
import jakarta.enterprise.inject.Instance;
import jakarta.enterprise.inject.Produces;
import jakarta.enterprise.inject.Typed;
import jakarta.inject.Singleton;
import jakarta.interceptor.Interceptor;
import jakarta.transaction.SystemException;
import jakarta.transaction.UserTransaction;

import org.jboss.logging.Logger;
import org.quartz.CronScheduleBuilder;
import org.quartz.Job;
import org.quartz.JobBuilder;
import org.quartz.JobDetail;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.quartz.JobKey;
import org.quartz.ScheduleBuilder;
import org.quartz.SchedulerException;
import org.quartz.SchedulerFactory;
import org.quartz.SimpleScheduleBuilder;
import org.quartz.Trigger.TriggerState;
import org.quartz.TriggerBuilder;
import org.quartz.TriggerKey;
import org.quartz.impl.StdSchedulerFactory;
import org.quartz.impl.matchers.GroupMatcher;
import org.quartz.simpl.InitThreadContextClassLoadHelper;
import org.quartz.simpl.SimpleJobFactory;
import org.quartz.spi.TriggerFiredBundle;

import com.cronutils.mapper.CronMapper;
import com.cronutils.model.Cron;
import com.cronutils.model.CronType;
import com.cronutils.model.definition.CronDefinition;
import com.cronutils.model.definition.CronDefinitionBuilder;
import com.cronutils.parser.CronParser;

import io.quarkus.arc.Subclass;
import io.quarkus.quartz.QuartzScheduler;
import io.quarkus.runtime.StartupEvent;
import io.quarkus.scheduler.FailedExecution;
import io.quarkus.scheduler.Scheduled;
import io.quarkus.scheduler.Scheduled.SkipPredicate;
import io.quarkus.scheduler.ScheduledExecution;
import io.quarkus.scheduler.ScheduledJobPaused;
import io.quarkus.scheduler.ScheduledJobResumed;
import io.quarkus.scheduler.Scheduler;
import io.quarkus.scheduler.SchedulerPaused;
import io.quarkus.scheduler.SchedulerResumed;
import io.quarkus.scheduler.SkippedExecution;
import io.quarkus.scheduler.SuccessfulExecution;
import io.quarkus.scheduler.Trigger;
import io.quarkus.scheduler.common.runtime.AbstractJobDefinition;
import io.quarkus.scheduler.common.runtime.DefaultInvoker;
import io.quarkus.scheduler.common.runtime.Events;
import io.quarkus.scheduler.common.runtime.ScheduledInvoker;
import io.quarkus.scheduler.common.runtime.ScheduledMethod;
import io.quarkus.scheduler.common.runtime.SchedulerContext;
import io.quarkus.scheduler.common.runtime.SyntheticScheduled;
import io.quarkus.scheduler.common.runtime.util.SchedulerUtils;
import io.quarkus.scheduler.runtime.SchedulerConfig;
import io.quarkus.scheduler.runtime.SchedulerRuntimeConfig;
import io.quarkus.scheduler.runtime.SchedulerRuntimeConfig.StartMode;
import io.quarkus.scheduler.runtime.SimpleScheduler;
import io.quarkus.scheduler.spi.JobInstrumenter;
import io.quarkus.vertx.core.runtime.context.VertxContextSafetyToggle;
import io.quarkus.virtual.threads.VirtualThreadsRecorder;
import io.smallrye.common.vertx.VertxContext;
import io.smallrye.mutiny.Uni;
import io.vertx.core.Context;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;

/**
 * Although this class is not part of the public API it must not be renamed in order to preserve backward compatibility. The
 * name of this class can be stored in a Quartz table in the database. See https://github.com/quarkusio/quarkus/issues/29177
 * for more information.
 */
@Typed({ QuartzScheduler.class, Scheduler.class })
@Singleton
public class QuartzSchedulerImpl implements QuartzScheduler {

    private static final Logger LOGGER = Logger.getLogger(QuartzSchedulerImpl.class.getName());
    private static final String INVOKER_KEY = "invoker";

    private final org.quartz.Scheduler scheduler;
    private final boolean startHalted;
    private final Duration shutdownWaitTime;
    private final boolean enabled;
    private final CronType cronType;
    private final CronParser cronParser;
    private final Duration defaultOverdueGracePeriod;
    private final Map<String, QuartzTrigger> scheduledTasks = new ConcurrentHashMap<>();
    private final Event<SkippedExecution> skippedExecutionEvent;
    private final Event<SuccessfulExecution> successExecutionEvent;
    private final Event<FailedExecution> failedExecutionEvent;
    private final Event<SchedulerPaused> schedulerPausedEvent;
    private final Event<SchedulerResumed> schedulerResumedEvent;
    private final Event<ScheduledJobPaused> scheduledJobPausedEvent;
    private final Event<ScheduledJobResumed> scheduledJobResumedEvent;
    private final QuartzRuntimeConfig runtimeConfig;
    private final SchedulerConfig schedulerConfig;
    private final Instance<JobInstrumenter> jobInstrumenter;
    private final StoreType storeType;

    public QuartzSchedulerImpl(SchedulerContext context, QuartzSupport quartzSupport,
            SchedulerRuntimeConfig schedulerRuntimeConfig,
            Event<SkippedExecution> skippedExecutionEvent, Event<SuccessfulExecution> successExecutionEvent,
            Event<FailedExecution> failedExecutionEvent, Event<SchedulerPaused> schedulerPausedEvent,
            Event<SchedulerResumed> schedulerResumedEvent, Event<ScheduledJobPaused> scheduledJobPausedEvent,
            Event<ScheduledJobResumed> scheduledJobResumedEvent,
            Instance<Job> jobs, Instance<UserTransaction> userTransaction,
            Vertx vertx,
            SchedulerConfig schedulerConfig, Instance<JobInstrumenter> jobInstrumenter) {
        this.shutdownWaitTime = quartzSupport.getRuntimeConfig().shutdownWaitTime;
        this.skippedExecutionEvent = skippedExecutionEvent;
        this.successExecutionEvent = successExecutionEvent;
        this.failedExecutionEvent = failedExecutionEvent;
        this.schedulerPausedEvent = schedulerPausedEvent;
        this.schedulerResumedEvent = schedulerResumedEvent;
        this.scheduledJobPausedEvent = scheduledJobPausedEvent;
        this.scheduledJobResumedEvent = scheduledJobResumedEvent;
        this.runtimeConfig = quartzSupport.getRuntimeConfig();
        this.enabled = schedulerRuntimeConfig.enabled;
        this.defaultOverdueGracePeriod = schedulerRuntimeConfig.overdueGracePeriod;
        this.schedulerConfig = schedulerConfig;
        this.jobInstrumenter = jobInstrumenter;
        this.storeType = quartzSupport.getBuildTimeConfig().storeType;

        StartMode startMode = initStartMode(schedulerRuntimeConfig, runtimeConfig);

        boolean forceStart;
        if (startMode != StartMode.NORMAL) {
            startHalted = (startMode == StartMode.HALTED);
            forceStart = startHalted || (startMode == StartMode.FORCED);
        } else {
            startHalted = false;
            forceStart = false;
        }

        var simpleTriggerConfig = runtimeConfig.simpleTriggerConfig;
        var cronTriggerConfig = runtimeConfig.cronTriggerConfig;
        if (!QuartzMisfirePolicy.validCronValues().contains(cronTriggerConfig.misfirePolicyConfig.misfirePolicy)) {
            throw new IllegalArgumentException(
                    "Global cron trigger misfire policy configured with invalid option. Valid options are: "
                            + QuartzMisfirePolicy.validCronValues().stream()
                                    .map(QuartzMisfirePolicy::dashedName)
                                    .collect(Collectors.joining(", ")));
        }
        if (!QuartzMisfirePolicy.validSimpleValues().contains(simpleTriggerConfig.misfirePolicyConfig.misfirePolicy)) {
            throw new IllegalArgumentException(
                    "Global simple trigger misfire policy configured with invalid option. Valid options are: "
                            + QuartzMisfirePolicy.validSimpleValues().stream()
                                    .map(QuartzMisfirePolicy::dashedName)
                                    .collect(Collectors.joining(", ")));
        }

        cronType = context.getCronType();
        CronDefinition def = CronDefinitionBuilder.instanceDefinitionFor(cronType);
        cronParser = new CronParser(def);

        JobInstrumenter instrumenter = null;
        if (schedulerConfig.tracingEnabled && jobInstrumenter.isResolvable()) {
            instrumenter = jobInstrumenter.get();
        }

        if (!enabled) {
            LOGGER.info("Quartz scheduler is disabled by config property and will not be started");
            this.scheduler = null;
        } else if (!forceStart && context.getScheduledMethods().isEmpty()) {
            LOGGER.info("No scheduled business methods found - Quartz scheduler will not be started");
            this.scheduler = null;
        } else {
            UserTransaction transaction = null;

            try {
                boolean manageTx = quartzSupport.getBuildTimeConfig().storeType.isNonManagedTxJobStore();
                if (manageTx && userTransaction.isResolvable()) {
                    transaction = userTransaction.get();
                }
                Properties props = getSchedulerConfigurationProperties(quartzSupport);

                SchedulerFactory schedulerFactory = new StdSchedulerFactory(props);
                scheduler = schedulerFactory.getScheduler();

                // Set custom job factory
                scheduler.setJobFactory(
                        new InvokerJobFactory(scheduledTasks, jobs, vertx, instrumenter));

                if (transaction != null) {
                    transaction.begin();
                }
                Function<TriggerKey, org.quartz.Trigger> triggerFun = new Function<>() {
                    @Override
                    public org.quartz.Trigger apply(TriggerKey triggerKey) {
                        try {
                            return scheduler.getTrigger(triggerKey);
                        } catch (SchedulerException e) {
                            throw new IllegalStateException(e);
                        }
                    }
                };

                for (ScheduledMethod method : context.getScheduledMethods()) {
                    int nameSequence = 0;

                    for (Scheduled scheduled : method.getSchedules()) {
                        String identity = SchedulerUtils.lookUpPropertyValue(scheduled.identity());
                        if (identity.isEmpty()) {
                            identity = ++nameSequence + "_" + method.getInvokerClassName();
                        }

                        ScheduledInvoker invoker = SimpleScheduler.initInvoker(
                                context.createInvoker(method.getInvokerClassName()),
                                skippedExecutionEvent, successExecutionEvent, failedExecutionEvent,
                                scheduled.concurrentExecution(),
                                SimpleScheduler.initSkipPredicate(scheduled.skipExecutionIf()), instrumenter);

                        JobDetail jobDetail = createJobDetail(identity, method.getInvokerClassName());
                        Optional<TriggerBuilder<?>> triggerBuilder = createTrigger(identity, scheduled, cronType, runtimeConfig,
                                jobDetail);

                        if (triggerBuilder.isPresent()) {
                            org.quartz.Trigger trigger = triggerBuilder.get().build();
                            org.quartz.Trigger oldTrigger = scheduler.getTrigger(trigger.getKey());
                            if (oldTrigger != null) {
                                trigger = triggerBuilder.get().startAt(oldTrigger.getNextFireTime()).build();
                                scheduler.rescheduleJob(trigger.getKey(), trigger);
                                LOGGER.debugf("Rescheduled business method %s with config %s", method.getMethodDescription(),
                                        scheduled);
                            } else if (!scheduler.checkExists(trigger.getKey())) {
                                scheduler.scheduleJob(jobDetail, trigger);
                                LOGGER.debugf("Scheduled business method %s with config %s", method.getMethodDescription(),
                                        scheduled);
                            } else {
                                // TODO remove this code in 3.0, it is only here to ensure migration after the removal of
                                // "_trigger" suffix and the need to reschedule jobs due to configuration change between build
                                // and deploy time
                                oldTrigger = scheduler
                                        .getTrigger(new TriggerKey(identity + "_trigger", Scheduler.class.getName()));
                                if (oldTrigger != null) {
                                    scheduler.deleteJob(jobDetail.getKey());
                                    trigger = triggerBuilder.get().startAt(oldTrigger.getNextFireTime()).build();
                                    scheduler.scheduleJob(jobDetail, trigger);
                                    LOGGER.debugf(
                                            "Rescheduled business method %s with config %s due to Trigger '%s' record being renamed after removal of '_trigger' suffix",
                                            method.getMethodDescription(),
                                            scheduled, oldTrigger.getKey().getName());
                                }
                            }
                            scheduledTasks.put(identity,
                                    new QuartzTrigger(trigger.getKey(), triggerFun, invoker,
                                            SchedulerUtils.parseOverdueGracePeriod(scheduled, defaultOverdueGracePeriod),
                                            quartzSupport.getRuntimeConfig().runBlockingScheduledMethodOnQuartzThread, false,
                                            method.getMethodDescription()));
                        } else {
                            // The job is disabled
                            scheduler.deleteJob(new JobKey(identity, Scheduler.class.getName()));
                        }
                    }
                }

                // Find persistent jobs scheduled with JobDefinition
                if (storeType.isDbStore()) {
                    Set<TriggerKey> triggers = scheduler
                            .getTriggerKeys(GroupMatcher.triggerGroupEquals(Scheduler.class.getName()));

                    for (TriggerKey triggerKey : triggers) {
                        JobDetail jobDetail = scheduler.getJobDetail(new JobKey(triggerKey.getName(), triggerKey.getGroup()));
                        if (jobDetail == null) {
                            throw new IllegalStateException("Unable to obtain the job detail for " + triggerKey);
                        }

                        String scheduledJson = jobDetail.getJobDataMap().getString(SCHEDULED_METADATA);
                        if (scheduledJson != null) {
                            SyntheticScheduled scheduled = SyntheticScheduled.fromJson(scheduledJson);
                            org.quartz.Trigger oldTrigger = scheduler.getTrigger(triggerKey);
                            if (oldTrigger == null) {
                                throw new IllegalStateException("Unable to obtain the trigger for " + triggerKey);
                            }
                            createJobDefinitionQuartzTrigger(new SerializedExecutionMetadata(jobDetail), scheduled, oldTrigger);
                        }
                    }
                }

                if (transaction != null) {
                    transaction.commit();
                }
            } catch (Throwable e) {
                if (transaction != null) {
                    try {
                        transaction.rollback();
                    } catch (SystemException ex) {
                        LOGGER.error("Unable to rollback transaction", ex);
                    }
                }
                throw new IllegalStateException("Unable to create Scheduler", e);
            }
        }
    }

    @Produces
    @Singleton
    org.quartz.Scheduler produceQuartzScheduler() {
        if (scheduler == null) {
            throw new IllegalStateException(
                    "Quartz scheduler is either explicitly disabled through quarkus.scheduler.enabled=false or no @Scheduled methods were found. If you only need to schedule a job programmatically you can force the start of the scheduler by setting 'quarkus.scheduler.start-mode=forced'.");
        }
        return scheduler;
    }

    @Override
    public org.quartz.Scheduler getScheduler() {
        return scheduler;
    }

    @Override
    public void pause() {
        if (!enabled) {
            LOGGER.warn("Quartz Scheduler is disabled and cannot be paused");
        } else {
            try {
                if (scheduler != null) {
                    scheduler.standby();
                    Events.fire(schedulerPausedEvent, SchedulerPaused.INSTANCE);
                }
            } catch (SchedulerException e) {
                throw new RuntimeException("Unable to pause scheduler", e);
            }
        }
    }

    @Override
    public void pause(String identity) {
        Objects.requireNonNull(identity, "Cannot pause - identity is null");
        if (identity.isEmpty()) {
            LOGGER.warn("Cannot pause - identity is empty");
            return;
        }
        try {
            String parsedIdentity = SchedulerUtils.lookUpPropertyValue(identity);
            QuartzTrigger trigger = scheduledTasks.get(parsedIdentity);
            if (trigger != null) {
                scheduler.pauseJob(new JobKey(parsedIdentity, Scheduler.class.getName()));
                Events.fire(scheduledJobPausedEvent, new ScheduledJobPaused(trigger));
            }
        } catch (SchedulerException e) {
            throw new RuntimeException("Unable to pause job", e);
        }
    }

    @Override
    public boolean isPaused(String identity) {
        Objects.requireNonNull(identity);
        if (identity.isEmpty()) {
            return false;
        }
        try {
            List<? extends org.quartz.Trigger> triggers = scheduler
                    .getTriggersOfJob(new JobKey(SchedulerUtils.lookUpPropertyValue(identity), Scheduler.class.getName()));
            if (triggers.isEmpty()) {
                return false;
            }
            for (org.quartz.Trigger trigger : triggers) {
                try {
                    if (scheduler.getTriggerState(trigger.getKey()) != TriggerState.PAUSED) {
                        return false;
                    }
                } catch (SchedulerException e) {
                    LOGGER.warnf("Cannot obtain the trigger state for %s", trigger.getKey());
                    return false;
                }
            }
            return true;
        } catch (SchedulerException e1) {
            LOGGER.warnf(e1, "Cannot obtain triggers for job with identity %s", identity);
            return false;
        }
    }

    @Override
    public void resume() {
        if (!enabled) {
            LOGGER.warn("Quartz Scheduler is disabled and cannot be resumed");
        } else {
            try {
                if (scheduler != null) {
                    scheduler.start();
                    Events.fire(schedulerResumedEvent, SchedulerResumed.INSTANCE);
                }
            } catch (SchedulerException e) {
                throw new RuntimeException("Unable to resume scheduler", e);
            }
        }
    }

    @Override
    public void resume(String identity) {
        Objects.requireNonNull(identity, "Cannot resume - identity is null");
        if (identity.isEmpty()) {
            LOGGER.warn("Cannot resume - identity is empty");
            return;
        }
        try {
            String parsedIdentity = SchedulerUtils.lookUpPropertyValue(identity);
            QuartzTrigger trigger = scheduledTasks.get(parsedIdentity);
            if (trigger != null) {
                scheduler.resumeJob(new JobKey(SchedulerUtils.lookUpPropertyValue(parsedIdentity), Scheduler.class.getName()));
                Events.fire(scheduledJobResumedEvent, new ScheduledJobResumed(trigger));
            }
        } catch (SchedulerException e) {
            throw new RuntimeException("Unable to resume job", e);
        }
    }

    @Override
    public boolean isRunning() {
        if (!enabled || scheduler == null) {
            return false;
        } else {
            try {
                return !scheduler.isInStandbyMode();
            } catch (SchedulerException e) {
                throw new IllegalStateException("Could not evaluate standby mode", e);
            }
        }
    }

    @Override
    public List<Trigger> getScheduledJobs() {
        return List.copyOf(scheduledTasks.values());
    }

    @Override
    public Trigger getScheduledJob(String identity) {
        Objects.requireNonNull(identity);
        if (identity.isEmpty()) {
            return null;
        }
        return scheduledTasks.get(SchedulerUtils.lookUpPropertyValue(identity));
    }

    @Override
    public JobDefinition newJob(String identity) {
        Objects.requireNonNull(identity);
        if (scheduledTasks.containsKey(identity)) {
            throw new IllegalStateException("A job with this identity is already scheduled: " + identity);
        }
        return new QuartzJobDefinition(identity);
    }

    @Override
    public Trigger unscheduleJob(String identity) {
        Objects.requireNonNull(identity);
        if (!identity.isEmpty()) {
            String parsedIdentity = SchedulerUtils.lookUpPropertyValue(identity);
            QuartzTrigger trigger = scheduledTasks.get(parsedIdentity);
            if (trigger != null && trigger.isProgrammatic) {
                if (scheduledTasks.remove(identity) != null) {
                    try {
                        scheduler.unscheduleJob(trigger.triggerKey);
                    } catch (SchedulerException e) {
                        throw new IllegalStateException("Unable to unschedule job with identity: " + identity);
                    }
                    return trigger;
                }
            }
        }
        return null;
    }

    // Use Interceptor.Priority.PLATFORM_BEFORE to start the scheduler before regular StartupEvent observers
    void start(@Observes @Priority(Interceptor.Priority.PLATFORM_BEFORE) StartupEvent startupEvent) {
        if (scheduler == null || startHalted) {
            return;
        }
        try {
            scheduler.start();
        } catch (SchedulerException e) {
            throw new IllegalStateException("Unable to start Scheduler", e);
        }
    }

    /**
     * Need to gracefully shut down the scheduler making sure that all triggers have been
     * released before datasource shutdown.
     *
     * @param event ignored
     */
    void destroy(@Observes(notifyObserver = Reception.IF_EXISTS) @BeforeDestroyed(ApplicationScoped.class) Object event) {
        if (scheduler != null) {
            try {
                if (shutdownWaitTime.isZero()) {
                    scheduler.shutdown(false);
                } else {
                    CompletableFuture.supplyAsync(new Supplier<>() {
                        @Override
                        public Void get() {
                            // Note that this method does not return until all currently executing jobs have completed
                            try {
                                scheduler.shutdown(true);
                            } catch (SchedulerException e) {
                                throw new RuntimeException(e);
                            }
                            return null;
                        }
                    }).get(shutdownWaitTime.toMillis(), TimeUnit.MILLISECONDS);
                }

            } catch (Exception e) {
                LOGGER.warnf("Unable to gracefully shutdown the scheduler", e);
            }
        }
    }

    @PreDestroy
    void destroy() {
        if (scheduler != null) {
            try {
                if (!scheduler.isShutdown()) {
                    scheduler.shutdown(false); // force shutdown
                }
            } catch (SchedulerException e) {
                LOGGER.warnf("Unable to shutdown the scheduler", e);
            }
        }
    }

    private Properties getSchedulerConfigurationProperties(QuartzSupport quartzSupport) {
        Properties props = new Properties();
        QuartzBuildTimeConfig buildTimeConfig = quartzSupport.getBuildTimeConfig();
        QuartzRuntimeConfig runtimeConfig = quartzSupport.getRuntimeConfig();

        props.put("org.quartz.scheduler.skipUpdateCheck", "true");
        props.put(StdSchedulerFactory.PROP_SCHED_INSTANCE_NAME, runtimeConfig.instanceName);
        props.put(StdSchedulerFactory.PROP_SCHED_BATCH_TIME_WINDOW,
                "" + runtimeConfig.batchTriggerAcquisitionFireAheadTimeWindow);
        props.put(StdSchedulerFactory.PROP_SCHED_MAX_BATCH_SIZE, "" + runtimeConfig.batchTriggerAcquisitionMaxCount);
        props.put(StdSchedulerFactory.PROP_SCHED_WRAP_JOB_IN_USER_TX, "false");
        props.put(StdSchedulerFactory.PROP_SCHED_SCHEDULER_THREADS_INHERIT_CONTEXT_CLASS_LOADER_OF_INITIALIZING_THREAD, "true");
        props.put(StdSchedulerFactory.PROP_THREAD_POOL_CLASS, "org.quartz.simpl.SimpleThreadPool");
        props.put(StdSchedulerFactory.PROP_SCHED_CLASS_LOAD_HELPER_CLASS, InitThreadContextClassLoadHelper.class.getName());
        props.put(StdSchedulerFactory.PROP_THREAD_POOL_PREFIX + ".threadCount", "" + runtimeConfig.threadCount);
        props.put(StdSchedulerFactory.PROP_THREAD_POOL_PREFIX + ".threadPriority", "" + runtimeConfig.threadPriority);
        props.put(StdSchedulerFactory.PROP_SCHED_RMI_EXPORT, "false");
        props.put(StdSchedulerFactory.PROP_SCHED_RMI_PROXY, "false");
        props.put(StdSchedulerFactory.PROP_JOB_STORE_CLASS, buildTimeConfig.storeType.clazz);

        if (buildTimeConfig.storeType.isDbStore()) {
            String dataSource = buildTimeConfig.dataSourceName.orElse("QUARKUS_QUARTZ_DEFAULT_DATASOURCE");
            QuarkusQuartzConnectionPoolProvider.setDataSourceName(dataSource);
            boolean serializeJobData = buildTimeConfig.serializeJobData.orElse(false);
            props.put(StdSchedulerFactory.PROP_JOB_STORE_USE_PROP, serializeJobData ? "false" : "true");
            props.put(StdSchedulerFactory.PROP_JOB_STORE_PREFIX + ".misfireThreshold",
                    "" + runtimeConfig.misfireThreshold.toMillis());
            props.put(StdSchedulerFactory.PROP_JOB_STORE_PREFIX + ".tablePrefix", buildTimeConfig.tablePrefix);
            props.put(StdSchedulerFactory.PROP_JOB_STORE_PREFIX + ".dataSource", dataSource);
            props.put(StdSchedulerFactory.PROP_JOB_STORE_PREFIX + ".driverDelegateClass",
                    quartzSupport.getDriverDialect().get());
            props.put(StdSchedulerFactory.PROP_DATASOURCE_PREFIX + "." + dataSource + ".connectionProvider.class",
                    QuarkusQuartzConnectionPoolProvider.class.getName());
            props.put(StdSchedulerFactory.PROP_JOB_STORE_PREFIX + ".acquireTriggersWithinLock", "true");
            if (buildTimeConfig.clustered) {
                props.put(StdSchedulerFactory.PROP_JOB_STORE_PREFIX + ".isClustered", "true");
                props.put(StdSchedulerFactory.PROP_JOB_STORE_PREFIX + ".clusterCheckinInterval",
                        "" + buildTimeConfig.clusterCheckinInterval);
                if (buildTimeConfig.selectWithLockSql.isPresent()) {
                    props.put(StdSchedulerFactory.PROP_JOB_STORE_PREFIX + ".selectWithLockSQL",
                            buildTimeConfig.selectWithLockSql.get());
                }
            }

            if (buildTimeConfig.storeType.isNonManagedTxJobStore()) {
                props.put(StdSchedulerFactory.PROP_JOB_STORE_PREFIX + ".nonManagedTXDataSource", dataSource);
            }
        }
        QuartzExtensionPointConfig instanceIdGenerator = buildTimeConfig.instanceIdGenerators.get(runtimeConfig.instanceId);
        if (runtimeConfig.instanceId.equals(StdSchedulerFactory.AUTO_GENERATE_INSTANCE_ID) || instanceIdGenerator != null) {
            props.put(StdSchedulerFactory.PROP_SCHED_INSTANCE_ID, StdSchedulerFactory.AUTO_GENERATE_INSTANCE_ID);
        } else {
            if (runtimeConfig.instanceId.equals(StdSchedulerFactory.SYSTEM_PROPERTY_AS_INSTANCE_ID)) {
                LOGGER.warn("Prefer to configure the 'SystemPropertyInstanceIdGenerator' within the instance ID generators, "
                        + "so the system property name can be changed and the application can be native.");
            }
            props.put(StdSchedulerFactory.PROP_SCHED_INSTANCE_ID, runtimeConfig.instanceId);
        }
        if (instanceIdGenerator != null) {
            putExtensionConfigurationProperties(props, StdSchedulerFactory.PROP_SCHED_INSTANCE_ID_GENERATOR_PREFIX,
                    instanceIdGenerator);
        }
        putExtensionConfigurationProperties(props, StdSchedulerFactory.PROP_PLUGIN_PREFIX, buildTimeConfig.plugins);
        putExtensionConfigurationProperties(props, StdSchedulerFactory.PROP_JOB_LISTENER_PREFIX, buildTimeConfig.jobListeners);
        putExtensionConfigurationProperties(props, StdSchedulerFactory.PROP_TRIGGER_LISTENER_PREFIX,
                buildTimeConfig.triggerListeners);

        return props;
    }

    private void putExtensionConfigurationProperties(Properties props, String prefix,
            Map<String, QuartzExtensionPointConfig> configs) {
        configs.forEach((configKey, config) -> {
            putExtensionConfigurationProperties(props, String.format("%s.%s", prefix, configKey), config);
        });
    }

    private void putExtensionConfigurationProperties(Properties props, String prefix, QuartzExtensionPointConfig config) {
        props.put(String.format("%s.class", prefix), config.clazz);
        config.properties.forEach((propName, propValue) -> {
            props.put(String.format("%s.%s", prefix, propName), propValue);
        });
    }

    @SuppressWarnings("deprecation")
    StartMode initStartMode(SchedulerRuntimeConfig schedulerRuntimeConfig, QuartzRuntimeConfig quartzRuntimeConfig) {
        if (schedulerRuntimeConfig.startMode.isPresent()) {
            StartMode startMode = schedulerRuntimeConfig.startMode.get();
            if (quartzRuntimeConfig.startMode.isPresent()) {
                QuartzStartMode quartzStartMode = quartzRuntimeConfig.startMode.get();
                if ((startMode == StartMode.NORMAL
                        && quartzStartMode != QuartzStartMode.NORMAL)
                        || (startMode == StartMode.FORCED && quartzStartMode != QuartzStartMode.FORCED)
                        || (startMode == StartMode.HALTED && quartzStartMode != QuartzStartMode.HALTED)) {
                    throw new IllegalStateException(
                            "Inconsistent scheduler startup mode configuration; quarkus.scheduler.startMode=" + startMode
                                    + " does not match quarkus.quartz.startMode=" + quartzStartMode);
                }
            }
            return startMode;
        } else {
            if (quartzRuntimeConfig.startMode.isPresent()) {
                QuartzStartMode quartzStartMode = quartzRuntimeConfig.startMode.get();
                switch (quartzStartMode) {
                    case NORMAL:
                        return StartMode.NORMAL;
                    case FORCED:
                        return StartMode.FORCED;
                    case HALTED:
                        return StartMode.HALTED;
                    default:
                        throw new IllegalStateException();
                }
            } else {
                return StartMode.NORMAL;
            }
        }
    }

    private JobDetail createJobDetail(String identity, String invokerClassName) {
        return JobBuilder.newJob(InvokerJob.class)
                // new JobKey(identity, "io.quarkus.scheduler.Scheduler")
                .withIdentity(identity, Scheduler.class.getName())
                // this info is redundant but keep it for backward compatibility
                .usingJobData(INVOKER_KEY, invokerClassName)
                .requestRecovery().build();
    }

    /**
     * Returns an empty {@link Optional} if the job is disabled.
     *
     * @param identity
     * @param scheduled
     * @param cronType
     * @param runtimeConfig
     * @param jobDetail
     * @return the trigger builder
     * @see SchedulerUtils#isOff(String)
     */
    private Optional<TriggerBuilder<?>> createTrigger(String identity, Scheduled scheduled, CronType cronType,
            QuartzRuntimeConfig runtimeConfig, JobDetail jobDetail) {

        ScheduleBuilder<?> scheduleBuilder;
        String cron = SchedulerUtils.lookUpPropertyValue(scheduled.cron());
        if (!cron.isEmpty()) {
            if (SchedulerUtils.isOff(cron)) {
                return Optional.empty();
            }
            if (!CronType.QUARTZ.equals(cronType)) {
                // Migrate the expression
                Cron cronExpr = cronParser.parse(cron);
                switch (cronType) {
                    case UNIX:
                        cron = CronMapper.fromUnixToQuartz().map(cronExpr).asString();
                        break;
                    case CRON4J:
                        cron = CronMapper.fromCron4jToQuartz().map(cronExpr).asString();
                        break;
                    default:
                        break;
                }
            }
            CronScheduleBuilder cronScheduleBuilder = CronScheduleBuilder.cronSchedule(cron);
            ZoneId timeZone = SchedulerUtils.parseCronTimeZone(scheduled);
            if (timeZone != null) {
                cronScheduleBuilder.inTimeZone(TimeZone.getTimeZone(timeZone));
            }
            QuartzRuntimeConfig.QuartzMisfirePolicyConfig perJobConfig = runtimeConfig.misfirePolicyPerJobs
                    .getOrDefault(identity, runtimeConfig.cronTriggerConfig.misfirePolicyConfig);
            switch (perJobConfig.misfirePolicy) {
                case SMART_POLICY:
                    // this is the default, doing nothing
                    break;
                case IGNORE_MISFIRE_POLICY:
                    cronScheduleBuilder.withMisfireHandlingInstructionIgnoreMisfires();
                    break;
                case FIRE_NOW:
                    cronScheduleBuilder.withMisfireHandlingInstructionFireAndProceed();
                    break;
                case CRON_TRIGGER_DO_NOTHING:
                    cronScheduleBuilder.withMisfireHandlingInstructionDoNothing();
                    break;
                case SIMPLE_TRIGGER_RESCHEDULE_NOW_WITH_EXISTING_REPEAT_COUNT:
                case SIMPLE_TRIGGER_RESCHEDULE_NOW_WITH_REMAINING_REPEAT_COUNT:
                case SIMPLE_TRIGGER_RESCHEDULE_NEXT_WITH_EXISTING_COUNT:
                case SIMPLE_TRIGGER_RESCHEDULE_NEXT_WITH_REMAINING_COUNT:
                    throw new IllegalArgumentException("Cron job " + identity
                            + " configured with invalid misfire policy "
                            + perJobConfig.misfirePolicy.dashedName() +
                            "\nValid options are: "
                            + QuartzMisfirePolicy.validCronValues().stream()
                                    .map(QuartzMisfirePolicy::dashedName)
                                    .collect(Collectors.joining(", ")));
            }

            scheduleBuilder = cronScheduleBuilder;
        } else if (!scheduled.every().isEmpty()) {
            OptionalLong everyMillis = SchedulerUtils.parseEveryAsMillis(scheduled);
            if (!everyMillis.isPresent()) {
                return Optional.empty();
            }
            SimpleScheduleBuilder simpleScheduleBuilder = SimpleScheduleBuilder.simpleSchedule()
                    .withIntervalInMilliseconds(everyMillis.getAsLong())
                    .repeatForever();
            QuartzRuntimeConfig.QuartzMisfirePolicyConfig perJobConfig = runtimeConfig.misfirePolicyPerJobs
                    .getOrDefault(identity, runtimeConfig.simpleTriggerConfig.misfirePolicyConfig);
            switch (perJobConfig.misfirePolicy) {
                case SMART_POLICY:
                    // this is the default, doing nothing
                    break;
                case IGNORE_MISFIRE_POLICY:
                    simpleScheduleBuilder.withMisfireHandlingInstructionIgnoreMisfires();
                    break;
                case FIRE_NOW:
                    simpleScheduleBuilder.withMisfireHandlingInstructionFireNow();
                    break;
                case SIMPLE_TRIGGER_RESCHEDULE_NOW_WITH_EXISTING_REPEAT_COUNT:
                    simpleScheduleBuilder.withMisfireHandlingInstructionNowWithExistingCount();
                    break;
                case SIMPLE_TRIGGER_RESCHEDULE_NOW_WITH_REMAINING_REPEAT_COUNT:
                    simpleScheduleBuilder.withMisfireHandlingInstructionNowWithRemainingCount();
                    break;
                case SIMPLE_TRIGGER_RESCHEDULE_NEXT_WITH_EXISTING_COUNT:
                    simpleScheduleBuilder.withMisfireHandlingInstructionNextWithExistingCount();
                    break;
                case SIMPLE_TRIGGER_RESCHEDULE_NEXT_WITH_REMAINING_COUNT:
                    simpleScheduleBuilder.withMisfireHandlingInstructionNextWithRemainingCount();
                    break;
                case CRON_TRIGGER_DO_NOTHING:
                    throw new IllegalArgumentException("Simple job " + identity
                            + " configured with invalid misfire policy "
                            + perJobConfig.misfirePolicy.dashedName() +
                            "\nValid options are: "
                            + QuartzMisfirePolicy.validSimpleValues().stream()
                                    .map(QuartzMisfirePolicy::dashedName)
                                    .collect(Collectors.joining(", ")));
            }
            scheduleBuilder = simpleScheduleBuilder;
        } else {
            throw new IllegalArgumentException("Invalid schedule configuration: " + scheduled);
        }

        TriggerBuilder<?> triggerBuilder = TriggerBuilder.newTrigger()
                .withIdentity(identity, Scheduler.class.getName())
                .forJob(jobDetail)
                .withSchedule(scheduleBuilder);

        Long millisToAdd = null;
        if (scheduled.delay() > 0) {
            millisToAdd = scheduled.delayUnit().toMillis(scheduled.delay());
        } else if (!scheduled.delayed().isEmpty()) {
            millisToAdd = SchedulerUtils.parseDelayedAsMillis(scheduled);
        }
        if (millisToAdd != null) {
            triggerBuilder.startAt(new Date(Instant.now()
                    .plusMillis(millisToAdd).toEpochMilli()));
        }
        return Optional.of(triggerBuilder);
    }

    class QuartzJobDefinition extends AbstractJobDefinition implements ExecutionMetadata {

        QuartzJobDefinition(String id) {
            super(id);
        }

        @Override
        public boolean isRunOnVirtualThread() {
            return runOnVirtualThread;
        }

        @Override
        public Consumer<ScheduledExecution> task() {
            return task;
        }

        @Override
        public Function<ScheduledExecution, Uni<Void>> asyncTask() {
            return asyncTask;
        }

        @Override
        public SkipPredicate skipPredicate() {
            return skipPredicate;
        }

        @Override
        public Class<? extends Consumer<ScheduledExecution>> taskClass() {
            return taskClass;
        }

        @Override
        public Class<? extends Function<ScheduledExecution, Uni<Void>>> asyncTaskClass() {
            return asyncTaskClass;
        }

        @Override
        public Class<? extends SkipPredicate> skipPredicateClass() {
            return skipPredicateClass;
        }

        @Override
        public JobDefinition setSkipPredicate(SkipPredicate skipPredicate) {
            if (storeType.isDbStore() && skipPredicateClass == null) {
                throw new IllegalStateException(
                        "A skip predicate instance cannot be scheduled programmatically if DB store type is used; register a skip predicate class instead");
            }
            return super.setSkipPredicate(skipPredicate);
        }

        @Override
        public JobDefinition setTask(Consumer<ScheduledExecution> task, boolean runOnVirtualThread) {
            if (storeType.isDbStore() && taskClass == null) {
                throw new IllegalStateException(
                        "A task instance cannot be scheduled programmatically if DB store type is used; register a task class instead");
            }
            return super.setTask(task, runOnVirtualThread);
        }

        @Override
        public JobDefinition setAsyncTask(Function<ScheduledExecution, Uni<Void>> asyncTask) {
            if (storeType.isDbStore() && asyncTaskClass == null) {
                throw new IllegalStateException(
                        "An async task instance cannot be scheduled programmatically if DB store type is used; register an async task class instead");
            }
            return super.setAsyncTask(asyncTask);
        }

        @Override
        public Trigger schedule() {
            checkScheduled();
            if (task == null && asyncTask == null) {
                throw new IllegalStateException("Either sync or async task must be set");
            }
            scheduled = true;
            SyntheticScheduled scheduled = new SyntheticScheduled(identity, cron, every, 0, TimeUnit.MINUTES, delayed,
                    overdueGracePeriod, concurrentExecution, skipPredicate, timeZone);
            return createJobDefinitionQuartzTrigger(this, scheduled, null);
        }

    }

    interface ExecutionMetadata {

        Consumer<ScheduledExecution> task();

        Class<? extends Consumer<ScheduledExecution>> taskClass();

        Function<ScheduledExecution, Uni<Void>> asyncTask();

        Class<? extends Function<ScheduledExecution, Uni<Void>>> asyncTaskClass();

        boolean isRunOnVirtualThread();

        SkipPredicate skipPredicate();

        Class<? extends SkipPredicate> skipPredicateClass();
    }

    static final String SCHEDULED_METADATA = "scheduled_metadata";
    static final String EXECUTION_METADATA_TASK_CLASS = "execution_metadata_task_class";
    static final String EXECUTION_METADATA_ASYNC_TASK_CLASS = "execution_metadata_async_task_class";
    static final String EXECUTION_METADATA_RUN_ON_VIRTUAL_THREAD = "execution_metadata_run_on_virtual_thread";
    static final String EXECUTION_METADATA_SKIP_PREDICATE_CLASS = "execution_metadata_skip_predicate_class";

    QuartzTrigger createJobDefinitionQuartzTrigger(ExecutionMetadata executionMetadata, SyntheticScheduled scheduled,
            org.quartz.Trigger oldTrigger) {
        ScheduledInvoker invoker;
        Consumer<ScheduledExecution> task = executionMetadata.task();
        Function<ScheduledExecution, Uni<Void>> asyncTask = executionMetadata.asyncTask();
        boolean runOnVirtualThread = executionMetadata.isRunOnVirtualThread();
        SkipPredicate skipPredicate = executionMetadata.skipPredicate();

        if (task != null) {
            // Use the default invoker to make sure the CDI request context is activated
            invoker = new DefaultInvoker() {
                @Override
                public CompletionStage<Void> invokeBean(ScheduledExecution execution) {
                    try {
                        task.accept(execution);
                        return CompletableFuture.completedStage(null);
                    } catch (Exception e) {
                        return CompletableFuture.failedStage(e);
                    }
                }

                @Override
                public boolean isRunningOnVirtualThread() {
                    return runOnVirtualThread;
                }
            };
        } else {
            invoker = new DefaultInvoker() {

                @Override
                public CompletionStage<Void> invokeBean(ScheduledExecution execution) {
                    try {
                        return asyncTask.apply(execution).subscribeAsCompletionStage();
                    } catch (Exception e) {
                        return CompletableFuture.failedStage(e);
                    }
                }

                @Override
                public boolean isBlocking() {
                    return false;
                }

            };
        }

        JobBuilder jobBuilder = JobBuilder.newJob(InvokerJob.class)
                // new JobKey(identity, "io.quarkus.scheduler.Scheduler")
                .withIdentity(scheduled.identity(), Scheduler.class.getName())
                // this info is redundant but keep it for backward compatibility
                .usingJobData(INVOKER_KEY, QuartzSchedulerImpl.class.getName());
        if (storeType.isDbStore()) {
            jobBuilder.usingJobData(SCHEDULED_METADATA, scheduled.toJson())
                    .usingJobData(EXECUTION_METADATA_RUN_ON_VIRTUAL_THREAD, Boolean.toString(runOnVirtualThread));
            if (executionMetadata.taskClass() != null) {
                jobBuilder.usingJobData(EXECUTION_METADATA_TASK_CLASS, executionMetadata.taskClass().getName());
            } else if (executionMetadata.asyncTaskClass() != null) {
                jobBuilder.usingJobData(EXECUTION_METADATA_TASK_CLASS, executionMetadata.asyncTaskClass().getName());
            }
            if (executionMetadata.skipPredicateClass() != null) {
                jobBuilder.usingJobData(EXECUTION_METADATA_SKIP_PREDICATE_CLASS,
                        executionMetadata.skipPredicateClass().getName());
            }
        }

        JobDetail jobDetail = jobBuilder.requestRecovery().build();

        org.quartz.Trigger trigger;
        Optional<TriggerBuilder<?>> triggerBuilder = createTrigger(scheduled.identity(), scheduled, cronType, runtimeConfig,
                jobDetail);
        if (triggerBuilder.isPresent()) {
            if (oldTrigger != null) {
                trigger = triggerBuilder.get().startAt(oldTrigger.getNextFireTime()).build();
            } else {
                trigger = triggerBuilder.get().build();
            }
        } else {
            if (oldTrigger != null) {
                throw new IllegalStateException(
                        "Job [" + scheduled.identity() + "] that was previously scheduled programmatically cannot be disabled");
            }
            // Job is disabled
            return null;
        }

        JobInstrumenter instrumenter = null;
        if (schedulerConfig.tracingEnabled && jobInstrumenter.isResolvable()) {
            instrumenter = jobInstrumenter.get();
        }
        invoker = SimpleScheduler.initInvoker(invoker, skippedExecutionEvent, successExecutionEvent,
                failedExecutionEvent, scheduled.concurrentExecution(), skipPredicate, instrumenter);
        QuartzTrigger quartzTrigger = new QuartzTrigger(trigger.getKey(),
                new Function<>() {
                    @Override
                    public org.quartz.Trigger apply(TriggerKey triggerKey) {
                        try {
                            return scheduler.getTrigger(triggerKey);
                        } catch (SchedulerException e) {
                            throw new IllegalStateException(e);
                        }
                    }
                }, invoker,
                SchedulerUtils.parseOverdueGracePeriod(scheduled, defaultOverdueGracePeriod),
                runtimeConfig.runBlockingScheduledMethodOnQuartzThread, true, null);
        QuartzTrigger existing = scheduledTasks.putIfAbsent(scheduled.identity(), quartzTrigger);

        if (existing != null) {
            throw new IllegalStateException("A job with this identity is already scheduled: " + scheduled.identity());
        }

        try {
            if (oldTrigger != null) {
                scheduler.rescheduleJob(trigger.getKey(), trigger);
                LOGGER.debugf("Rescheduled job definition with config %s", scheduled);
            } else {
                scheduler.scheduleJob(jobDetail, trigger);
                LOGGER.debugf("Scheduled job definition with config %s", scheduled);
            }
        } catch (SchedulerException e) {
            throw new IllegalStateException(e);
        }
        return quartzTrigger;
    }

    /**
     * Although this class is not part of the public API it must not be renamed in order to preserve backward compatibility. The
     * name of this class can be stored in a Quartz table in the database. See https://github.com/quarkusio/quarkus/issues/29177
     * for more information.
     */
    static class InvokerJob implements Job {

        final QuartzTrigger trigger;
        final Vertx vertx;

        InvokerJob(QuartzTrigger trigger, Vertx vertx) {
            this.trigger = trigger;
            this.vertx = vertx;
        }

        @Override
        public void execute(JobExecutionContext jobExecutionContext) throws JobExecutionException {
            if (trigger != null && trigger.invoker != null) { // could be null from previous runs
                if (trigger.invoker.isBlocking()) {
                    if (trigger.runBlockingMethodOnQuartzThread) {
                        try {
                            trigger.invoker.invoke(new QuartzScheduledExecution(trigger, jobExecutionContext));
                        } catch (Exception e) {
                            // already logged by the StatusEmitterInvoker
                        }
                    } else {
                        Context context = VertxContext.getOrCreateDuplicatedContext(vertx);
                        VertxContextSafetyToggle.setContextSafe(context, true);
                        if (trigger.invoker.isRunningOnVirtualThread()) {
                            // While counter-intuitive, we switch to a safe context, so that context is captured and attached
                            // to the virtual thread.
                            context.runOnContext(new Handler<Void>() {
                                @Override
                                public void handle(Void event) {
                                    VirtualThreadsRecorder.getCurrent().execute(new Runnable() {
                                        @Override
                                        public void run() {
                                            try {
                                                trigger.invoker
                                                        .invoke(new QuartzScheduledExecution(trigger, jobExecutionContext));
                                            } catch (Exception ignored) {
                                                // already logged by the StatusEmitterInvoker
                                            }
                                        }
                                    });
                                }
                            });
                        } else {
                            context.executeBlocking(new Callable<Object>() {

                                @Override
                                public Object call() throws Exception {
                                    return trigger.invoker.invoke(new QuartzScheduledExecution(trigger, jobExecutionContext));
                                }
                            }, false);
                        }
                    }
                } else {
                    Context context = VertxContext.getOrCreateDuplicatedContext(vertx);
                    VertxContextSafetyToggle.setContextSafe(context, true);
                    context.runOnContext(new Handler<Void>() {
                        @Override
                        public void handle(Void event) {
                            try {
                                trigger.invoker.invoke(new QuartzScheduledExecution(trigger, jobExecutionContext));
                            } catch (Exception e) {
                                // already logged by the StatusEmitterInvoker
                            }
                        }
                    });
                }
            } else {
                String jobName = jobExecutionContext.getJobDetail().getKey().getName();
                LOGGER.warnf("Unable to find corresponding Quartz trigger for job %s. "
                        + "Update your Quartz table by removing all phantom jobs or make sure that there is a "
                        + "Scheduled method with the identity matching the job's name", jobName);
            }
        }
    }

    static class QuartzTrigger implements Trigger {

        final org.quartz.TriggerKey triggerKey;
        final Function<TriggerKey, org.quartz.Trigger> triggerFunction;
        final ScheduledInvoker invoker;
        final Duration gracePeriod;
        final boolean isProgrammatic;
        final String methodDescription;

        final boolean runBlockingMethodOnQuartzThread;

        QuartzTrigger(org.quartz.TriggerKey triggerKey, Function<TriggerKey, org.quartz.Trigger> triggerFunction,
                ScheduledInvoker invoker, Duration gracePeriod, boolean runBlockingMethodOnQuartzThread,
                boolean isProgrammatic, String methodDescription) {
            this.triggerKey = triggerKey;
            this.triggerFunction = triggerFunction;
            this.invoker = invoker;
            this.gracePeriod = gracePeriod;
            this.runBlockingMethodOnQuartzThread = runBlockingMethodOnQuartzThread;
            this.isProgrammatic = isProgrammatic;
            this.methodDescription = methodDescription;
        }

        @Override
        public Instant getNextFireTime() {
            Date nextFireTime = getTrigger().getNextFireTime();
            return nextFireTime != null ? nextFireTime.toInstant() : null;
        }

        @Override
        public Instant getPreviousFireTime() {
            Date previousFireTime = getTrigger().getPreviousFireTime();
            return previousFireTime != null ? previousFireTime.toInstant() : null;
        }

        @Override
        public boolean isOverdue() {
            Instant nextFireTime = getNextFireTime();
            if (nextFireTime == null) {
                return false;
            }
            return LocalDateTime.ofInstant(nextFireTime, ZoneId.systemDefault()).plus(gracePeriod)
                    .isBefore(LocalDateTime.now());
        }

        @Override
        public String getId() {
            return getTrigger().getKey().getName();
        }

        private org.quartz.Trigger getTrigger() {
            return triggerFunction.apply(triggerKey);
        }

        @Override
        public String getMethodDescription() {
            return methodDescription;
        }

    }

    static class QuartzScheduledExecution implements ScheduledExecution {

        final JobExecutionContext context;
        final QuartzTrigger trigger;

        QuartzScheduledExecution(QuartzTrigger trigger, JobExecutionContext context) {
            this.trigger = trigger;
            this.context = context;
        }

        @Override
        public Trigger getTrigger() {
            return trigger;
        }

        @Override
        public Instant getFireTime() {
            return context.getFireTime().toInstant();
        }

        @Override
        public Instant getScheduledFireTime() {
            return context.getScheduledFireTime().toInstant();
        }

    }

    static class InvokerJobFactory extends SimpleJobFactory {

        final Map<String, QuartzTrigger> scheduledTasks;
        final Instance<Job> jobs;
        final Vertx vertx;
        final JobInstrumenter instrumenter;

        InvokerJobFactory(Map<String, QuartzTrigger> scheduledTasks, Instance<Job> jobs, Vertx vertx,
                JobInstrumenter instrumenter) {
            this.scheduledTasks = scheduledTasks;
            this.jobs = jobs;
            this.vertx = vertx;
            this.instrumenter = instrumenter;

        }

        @SuppressWarnings("unchecked")
        @Override
        public Job newJob(TriggerFiredBundle bundle, org.quartz.Scheduler Scheduler) throws SchedulerException {
            Class<? extends Job> jobClass = bundle.getJobDetail().getJobClass();

            if (jobClass.equals(InvokerJob.class)) {
                // This is a job backed by a @Scheduled method or a JobDefinition
                return new InvokerJob(scheduledTasks.get(bundle.getJobDetail().getKey().getName()), vertx);
            }
            if (Subclass.class.isAssignableFrom(jobClass)) {
                // Get the original class from an intercepted bean class
                jobClass = (Class<? extends Job>) jobClass.getSuperclass();
            }
            Instance<?> instance = jobs.select(jobClass);
            if (instance.isResolvable()) {
                // This is a job backed by a CDI bean
                return jobWithSpanWrapper((Job) instance.get());
            }
            // Instantiate a plain job class
            return jobWithSpanWrapper(super.newJob(bundle, Scheduler));
        }

        private Job jobWithSpanWrapper(Job job) {
            if (instrumenter != null) {
                return new InstrumentedJob(job, instrumenter);
            }
            return job;
        }

    }

    static class SerializedExecutionMetadata implements ExecutionMetadata {

        private final Class<? extends Consumer<ScheduledExecution>> taskClass;
        private final Class<? extends Function<ScheduledExecution, Uni<Void>>> asyncTaskClass;
        private final boolean runOnVirtualThread;
        private final Class<? extends SkipPredicate> skipPredicateClass;

        @SuppressWarnings("unchecked")
        public SerializedExecutionMetadata(JobDetail jobDetail) {
            ClassLoader tccl = Thread.currentThread().getContextClassLoader();
            String taskClassStr = jobDetail.getJobDataMap().getString(EXECUTION_METADATA_TASK_CLASS);
            try {
                this.taskClass = taskClassStr != null
                        ? (Class<? extends Consumer<ScheduledExecution>>) tccl.loadClass(taskClassStr)
                        : null;
            } catch (ClassNotFoundException e) {
                throw new IllegalStateException("Cannot load task class: " + taskClassStr);
            }
            String asyncTaskClassStr = jobDetail.getJobDataMap().getString(EXECUTION_METADATA_ASYNC_TASK_CLASS);
            try {
                this.asyncTaskClass = asyncTaskClassStr != null
                        ? (Class<? extends Function<ScheduledExecution, Uni<Void>>>) tccl.loadClass(asyncTaskClassStr)
                        : null;
            } catch (ClassNotFoundException e) {
                throw new IllegalStateException("Cannot load async task class: " + taskClassStr);
            }

            String skipPredicateClassStr = jobDetail.getJobDataMap().getString(EXECUTION_METADATA_SKIP_PREDICATE_CLASS);
            try {
                this.skipPredicateClass = skipPredicateClassStr != null
                        ? (Class<? extends SkipPredicate>) tccl.loadClass(skipPredicateClassStr)
                        : null;
            } catch (ClassNotFoundException e) {
                throw new IllegalStateException("Cannot load skip predicate class: " + taskClassStr);
            }
            this.runOnVirtualThread = Boolean
                    .parseBoolean(jobDetail.getJobDataMap().getString(EXECUTION_METADATA_RUN_ON_VIRTUAL_THREAD));
        }

        @Override
        public Consumer<ScheduledExecution> task() {
            return taskClass != null ? SchedulerUtils.instantiateBeanOrClass(taskClass) : null;
        }

        @Override
        public Class<? extends Consumer<ScheduledExecution>> taskClass() {
            return taskClass;
        }

        @Override
        public Function<ScheduledExecution, Uni<Void>> asyncTask() {
            return asyncTaskClass != null ? SchedulerUtils.instantiateBeanOrClass(asyncTaskClass) : null;
        }

        @Override
        public Class<? extends Function<ScheduledExecution, Uni<Void>>> asyncTaskClass() {
            return asyncTaskClass;
        }

        @Override
        public boolean isRunOnVirtualThread() {
            return runOnVirtualThread;
        }

        @Override
        public SkipPredicate skipPredicate() {
            return skipPredicateClass != null ? SchedulerUtils.instantiateBeanOrClass(skipPredicateClass) : null;
        }

        @Override
        public Class<? extends SkipPredicate> skipPredicateClass() {
            return skipPredicateClass;
        }

    }

}
