package io.quarkus.quartz.runtime;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.OptionalLong;
import java.util.Properties;
import java.util.function.Function;
import java.util.stream.Collectors;

import jakarta.annotation.PreDestroy;
import jakarta.annotation.Priority;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.context.BeforeDestroyed;
import jakarta.enterprise.event.Event;
import jakarta.enterprise.event.Observes;
import jakarta.enterprise.inject.Any;
import jakarta.enterprise.inject.Instance;
import jakarta.enterprise.inject.Produces;
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
import org.quartz.simpl.InitThreadContextClassLoadHelper;
import org.quartz.simpl.SimpleJobFactory;
import org.quartz.spi.TriggerFiredBundle;

import com.cronutils.mapper.CronMapper;
import com.cronutils.model.Cron;
import com.cronutils.model.CronType;
import com.cronutils.model.definition.CronDefinition;
import com.cronutils.model.definition.CronDefinitionBuilder;
import com.cronutils.parser.CronParser;

import io.quarkus.arc.Arc;
import io.quarkus.arc.Subclass;
import io.quarkus.runtime.StartupEvent;
import io.quarkus.scheduler.FailedExecution;
import io.quarkus.scheduler.Scheduled;
import io.quarkus.scheduler.Scheduled.ConcurrentExecution;
import io.quarkus.scheduler.ScheduledExecution;
import io.quarkus.scheduler.Scheduler;
import io.quarkus.scheduler.SkippedExecution;
import io.quarkus.scheduler.SuccessfulExecution;
import io.quarkus.scheduler.Trigger;
import io.quarkus.scheduler.common.runtime.ScheduledInvoker;
import io.quarkus.scheduler.common.runtime.ScheduledMethodMetadata;
import io.quarkus.scheduler.common.runtime.SchedulerContext;
import io.quarkus.scheduler.common.runtime.SkipConcurrentExecutionInvoker;
import io.quarkus.scheduler.common.runtime.SkipPredicateInvoker;
import io.quarkus.scheduler.common.runtime.StatusEmitterInvoker;
import io.quarkus.scheduler.common.runtime.util.SchedulerUtils;
import io.quarkus.scheduler.runtime.SchedulerRuntimeConfig;
import io.quarkus.vertx.core.runtime.context.VertxContextSafetyToggle;
import io.smallrye.common.vertx.VertxContext;
import io.vertx.core.Context;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;

@Singleton
public class QuartzScheduler implements Scheduler {

    private static final Logger LOGGER = Logger.getLogger(QuartzScheduler.class.getName());
    private static final String INVOKER_KEY = "invoker";

    private final org.quartz.Scheduler scheduler;
    private final boolean enabled;
    private final boolean startHalted;
    private final Map<String, QuartzTrigger> scheduledTasks = new HashMap<>();

    public QuartzScheduler(SchedulerContext context, QuartzSupport quartzSupport, SchedulerRuntimeConfig schedulerRuntimeConfig,
            Event<SkippedExecution> skippedExecutionEvent, Event<SuccessfulExecution> successfulExecutionEvent,
            Event<FailedExecution> failedExecutionEvent, Instance<Job> jobs, Instance<UserTransaction> userTransaction,
            Vertx vertx) {
        enabled = schedulerRuntimeConfig.enabled;
        final Duration defaultOverdueGracePeriod = schedulerRuntimeConfig.overdueGracePeriod;
        final QuartzRuntimeConfig runtimeConfig = quartzSupport.getRuntimeConfig();

        boolean forceStart;
        if (runtimeConfig.startMode != QuartzStartMode.NORMAL) {
            startHalted = (runtimeConfig.startMode == QuartzStartMode.HALTED);
            forceStart = startHalted || (runtimeConfig.startMode == QuartzStartMode.FORCED);
        } else {
            startHalted = false;
            forceStart = false;
        }

        if (!enabled) {
            LOGGER.info("Quartz scheduler is disabled by config property and will not be started");
            this.scheduler = null;
        } else if (!forceStart && context.getScheduledMethods().isEmpty()) {
            LOGGER.info("No scheduled business methods found - Quartz scheduler will not be started");
            this.scheduler = null;
        } else {
            // identity -> scheduled invoker instance
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
                scheduler.setJobFactory(new InvokerJobFactory(scheduledTasks, jobs, vertx));
                CronType cronType = context.getCronType();
                CronDefinition def = CronDefinitionBuilder.instanceDefinitionFor(cronType);
                CronParser parser = new CronParser(def);
                if (transaction != null) {
                    transaction.begin();
                }
                for (ScheduledMethodMetadata method : context.getScheduledMethods()) {
                    int nameSequence = 0;

                    for (Scheduled scheduled : method.getSchedules()) {
                        String identity = SchedulerUtils.lookUpPropertyValue(scheduled.identity());
                        if (identity.isEmpty()) {
                            identity = ++nameSequence + "_" + method.getInvokerClassName();
                        }
                        ScheduledInvoker invoker = new StatusEmitterInvoker(context.createInvoker(method.getInvokerClassName()),
                                successfulExecutionEvent, failedExecutionEvent);
                        if (scheduled.concurrentExecution() == ConcurrentExecution.SKIP) {
                            invoker = new SkipConcurrentExecutionInvoker(invoker, skippedExecutionEvent);
                        }
                        if (!scheduled.skipExecutionIf().equals(Scheduled.Never.class)) {
                            invoker = new SkipPredicateInvoker(invoker,
                                    Arc.container().select(scheduled.skipExecutionIf(), Any.Literal.INSTANCE).get(),
                                    skippedExecutionEvent);
                        }

                        JobBuilder jobBuilder = JobBuilder.newJob(InvokerJob.class)
                                // new JobKey(identity, "io.quarkus.scheduler.Scheduler")
                                .withIdentity(identity, Scheduler.class.getName())
                                // this info is redundant but keep it for backward compatibility
                                .usingJobData(INVOKER_KEY, method.getInvokerClassName())
                                .requestRecovery();
                        ScheduleBuilder<?> scheduleBuilder;
                        QuartzRuntimeConfig.QuartzMisfirePolicyConfig perJobConfig = runtimeConfig.misfirePolicyPerJobs
                                .get(identity);
                        String cron = SchedulerUtils.lookUpPropertyValue(scheduled.cron());
                        if (!cron.isEmpty()) {
                            if (SchedulerUtils.isOff(cron)) {
                                this.pause(identity);
                                continue;
                            }
                            if (!CronType.QUARTZ.equals(cronType)) {
                                // Migrate the expression
                                Cron cronExpr = parser.parse(cron);
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
                            if (perJobConfig != null) {
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
                            }
                            scheduleBuilder = cronScheduleBuilder;
                        } else if (!scheduled.every().isEmpty()) {
                            OptionalLong everyMillis = SchedulerUtils.parseEveryAsMillis(scheduled);
                            if (!everyMillis.isPresent()) {
                                this.pause(identity);
                                continue;
                            }
                            SimpleScheduleBuilder simpleScheduleBuilder = SimpleScheduleBuilder.simpleSchedule()
                                    .withIntervalInMilliseconds(everyMillis.getAsLong())
                                    .repeatForever();
                            if (perJobConfig != null) {
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
                            }
                            scheduleBuilder = simpleScheduleBuilder;
                        } else {
                            throw new IllegalArgumentException("Invalid schedule configuration: " + scheduled);
                        }

                        JobDetail jobDetail = jobBuilder.build();
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

                        org.quartz.Trigger trigger = triggerBuilder.build();
                        org.quartz.Trigger oldTrigger = scheduler.getTrigger(trigger.getKey());
                        if (oldTrigger != null) {
                            trigger = triggerBuilder.startAt(oldTrigger.getNextFireTime()).build();
                            scheduler.rescheduleJob(trigger.getKey(), trigger);
                            LOGGER.debugf("Rescheduled business method %s with config %s", method.getMethodDescription(),
                                    scheduled);
                        } else if (!scheduler.checkExists(jobDetail.getKey())) {
                            scheduler.scheduleJob(jobDetail, trigger);
                            LOGGER.debugf("Scheduled business method %s with config %s", method.getMethodDescription(),
                                    scheduled);
                        } else {
                            // TODO remove this code in 3.0, it is only here to ensure migration after the removal of
                            // "_trigger" suffix and the need to reschedule jobs due to configuration change between build
                            // and deploy time
                            oldTrigger = scheduler.getTrigger(new TriggerKey(identity + "_trigger", Scheduler.class.getName()));
                            if (oldTrigger != null) {
                                scheduler.deleteJob(jobDetail.getKey());
                                trigger = triggerBuilder.startAt(oldTrigger.getNextFireTime()).build();
                                scheduler.scheduleJob(jobDetail, trigger);
                                LOGGER.debugf(
                                        "Rescheduled business method %s with config %s due to Trigger '%s' record being renamed after removal of '_trigger' suffix",
                                        method.getMethodDescription(),
                                        scheduled, oldTrigger.getKey().getName());
                            }
                        }
                        scheduledTasks.put(identity, new QuartzTrigger(trigger.getKey(),
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
                                SchedulerUtils.parseOverdueGracePeriod(scheduled, defaultOverdueGracePeriod)));
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
                    "Quartz scheduler is either explicitly disabled through quarkus.scheduler.enabled=false or no @Scheduled methods were found. If you only need to schedule a job programmatically you can force the start of the scheduler by setting 'quarkus.quartz.start-mode=forced'.");
        }
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
            scheduler.pauseJob(new JobKey(SchedulerUtils.lookUpPropertyValue(identity), Scheduler.class.getName()));
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
            scheduler.resumeJob(new JobKey(SchedulerUtils.lookUpPropertyValue(identity), Scheduler.class.getName()));
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
    void destroy(@Observes @BeforeDestroyed(ApplicationScoped.class) Object event) {
        if (scheduler != null) {
            try {
                // Note that this method does not return until all currently executing jobs have completed
                scheduler.shutdown(true);
            } catch (SchedulerException e) {
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
        props.put(StdSchedulerFactory.PROP_SCHED_INSTANCE_ID, "AUTO");
        props.put("org.quartz.scheduler.skipUpdateCheck", "true");
        props.put(StdSchedulerFactory.PROP_SCHED_INSTANCE_NAME, quartzSupport.getRuntimeConfig().instanceName);
        props.put(StdSchedulerFactory.PROP_SCHED_WRAP_JOB_IN_USER_TX, "false");
        props.put(StdSchedulerFactory.PROP_SCHED_SCHEDULER_THREADS_INHERIT_CONTEXT_CLASS_LOADER_OF_INITIALIZING_THREAD, "true");
        props.put(StdSchedulerFactory.PROP_THREAD_POOL_CLASS, "org.quartz.simpl.SimpleThreadPool");
        props.put(StdSchedulerFactory.PROP_SCHED_CLASS_LOAD_HELPER_CLASS, InitThreadContextClassLoadHelper.class.getName());
        props.put(StdSchedulerFactory.PROP_THREAD_POOL_PREFIX + ".threadCount",
                "" + quartzSupport.getRuntimeConfig().threadCount);
        props.put(StdSchedulerFactory.PROP_THREAD_POOL_PREFIX + ".threadPriority",
                "" + quartzSupport.getRuntimeConfig().threadPriority);
        props.put(StdSchedulerFactory.PROP_SCHED_RMI_EXPORT, "false");
        props.put(StdSchedulerFactory.PROP_SCHED_RMI_PROXY, "false");
        props.put(StdSchedulerFactory.PROP_JOB_STORE_CLASS, buildTimeConfig.storeType.clazz);

        if (buildTimeConfig.storeType.isDbStore()) {
            String dataSource = buildTimeConfig.dataSourceName.orElse("QUARKUS_QUARTZ_DEFAULT_DATASOURCE");
            QuarkusQuartzConnectionPoolProvider.setDataSourceName(dataSource);
            props.put(StdSchedulerFactory.PROP_JOB_STORE_PREFIX + ".useProperties", "true");
            props.put(StdSchedulerFactory.PROP_JOB_STORE_PREFIX + ".misfireThreshold",
                    "" + quartzSupport.getRuntimeConfig().misfireThreshold.toMillis());
            props.put(StdSchedulerFactory.PROP_JOB_STORE_PREFIX + ".tablePrefix", buildTimeConfig.tablePrefix);
            props.put(StdSchedulerFactory.PROP_JOB_STORE_PREFIX + ".dataSource", dataSource);
            props.put(StdSchedulerFactory.PROP_JOB_STORE_PREFIX + ".driverDelegateClass",
                    quartzSupport.getDriverDialect().get());
            props.put(StdSchedulerFactory.PROP_DATASOURCE_PREFIX + "." + dataSource + ".connectionProvider.class",
                    QuarkusQuartzConnectionPoolProvider.class.getName());
            if (buildTimeConfig.clustered) {
                props.put(StdSchedulerFactory.PROP_JOB_STORE_PREFIX + ".isClustered", "true");
                props.put(StdSchedulerFactory.PROP_JOB_STORE_PREFIX + ".acquireTriggersWithinLock", "true");
                props.put(StdSchedulerFactory.PROP_JOB_STORE_PREFIX + ".clusterCheckinInterval",
                        "" + quartzSupport.getBuildTimeConfig().clusterCheckinInterval);
                if (buildTimeConfig.selectWithLockSql.isPresent()) {
                    props.put(StdSchedulerFactory.PROP_JOB_STORE_PREFIX + ".selectWithLockSQL",
                            buildTimeConfig.selectWithLockSql.get());
                }
            }

            if (buildTimeConfig.storeType.isNonManagedTxJobStore()) {
                props.put(StdSchedulerFactory.PROP_JOB_STORE_PREFIX + ".nonManagedTXDataSource", dataSource);
            }
        }
        props.putAll(getAdditionalConfigurationProperties(StdSchedulerFactory.PROP_PLUGIN_PREFIX, buildTimeConfig.plugins));
        props.putAll(getAdditionalConfigurationProperties(StdSchedulerFactory.PROP_JOB_LISTENER_PREFIX,
                buildTimeConfig.jobListeners));
        props.putAll(getAdditionalConfigurationProperties(StdSchedulerFactory.PROP_TRIGGER_LISTENER_PREFIX,
                buildTimeConfig.triggerListeners));

        return props;
    }

    private Properties getAdditionalConfigurationProperties(String prefix, Map<String, QuartzExtensionPointConfig> config) {
        Properties props = new Properties();
        for (Map.Entry<String, QuartzExtensionPointConfig> configEntry : config.entrySet()) {
            props.put(String.format("%s.%s.class", prefix, configEntry.getKey()), configEntry.getValue().clazz);
            for (Map.Entry<String, String> propsEntry : configEntry.getValue().properties.entrySet()) {
                props.put(String.format("%s.%s.%s", prefix, configEntry.getKey(), propsEntry.getKey()), propsEntry.getValue());
            }
        }
        return props;
    }

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
                    try {
                        trigger.invoker.invoke(new QuartzScheduledExecution(trigger, jobExecutionContext));
                    } catch (Exception e) {
                        throw new JobExecutionException(e);
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
                LOGGER.warnf("Unable to find corresponding Quartz trigger for job %s. " +
                        "Update your Quartz table by removing all phantom jobs or make sure that there is a " +
                        "Scheduled method with the identity matching the job's name", jobName);
            }
        }
    }

    static class QuartzTrigger implements Trigger {

        final org.quartz.TriggerKey triggerKey;
        final Function<TriggerKey, org.quartz.Trigger> triggerFunction;
        final ScheduledInvoker invoker;
        final Duration gracePeriod;

        QuartzTrigger(org.quartz.TriggerKey triggerKey, Function<TriggerKey, org.quartz.Trigger> triggerFunction,
                ScheduledInvoker invoker, Duration gracePeriod) {
            this.triggerKey = triggerKey;
            this.triggerFunction = triggerFunction;
            this.invoker = invoker;
            this.gracePeriod = gracePeriod;
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

        InvokerJobFactory(Map<String, QuartzTrigger> scheduledTasks, Instance<Job> jobs, Vertx vertx) {
            this.scheduledTasks = scheduledTasks;
            this.jobs = jobs;
            this.vertx = vertx;
        }

        @SuppressWarnings("unchecked")
        @Override
        public Job newJob(TriggerFiredBundle bundle, org.quartz.Scheduler Scheduler) throws SchedulerException {
            Class<? extends Job> jobClass = bundle.getJobDetail().getJobClass();

            if (jobClass.equals(InvokerJob.class)) {
                return new InvokerJob(scheduledTasks.get(bundle.getJobDetail().getKey().getName()), vertx);
            }
            if (Subclass.class.isAssignableFrom(jobClass)) {
                // Get the original class from an intercepted bean class
                jobClass = (Class<? extends Job>) jobClass.getSuperclass();
            }
            Instance<?> instance = jobs.select(jobClass);
            if (instance.isResolvable()) {
                return (Job) instance.get();
            }
            return super.newJob(bundle, Scheduler);
        }

    }

}
