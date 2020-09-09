package io.quarkus.quartz.runtime;

import java.time.Instant;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import javax.annotation.PreDestroy;
import javax.annotation.Priority;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.context.BeforeDestroyed;
import javax.enterprise.event.Event;
import javax.enterprise.event.Observes;
import javax.enterprise.inject.Instance;
import javax.enterprise.inject.Produces;
import javax.inject.Singleton;
import javax.interceptor.Interceptor;
import javax.transaction.SystemException;
import javax.transaction.UserTransaction;

import org.eclipse.microprofile.config.Config;
import org.jboss.logging.Logger;
import org.quartz.CronScheduleBuilder;
import org.quartz.Job;
import org.quartz.JobBuilder;
import org.quartz.JobDetail;
import org.quartz.JobExecutionContext;
import org.quartz.ScheduleBuilder;
import org.quartz.SchedulerException;
import org.quartz.SchedulerFactory;
import org.quartz.SimpleScheduleBuilder;
import org.quartz.TriggerBuilder;
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
import io.quarkus.arc.InstanceHandle;
import io.quarkus.runtime.StartupEvent;
import io.quarkus.scheduler.Scheduled;
import io.quarkus.scheduler.Scheduled.ConcurrentExecution;
import io.quarkus.scheduler.ScheduledExecution;
import io.quarkus.scheduler.Scheduler;
import io.quarkus.scheduler.SkippedExecution;
import io.quarkus.scheduler.Trigger;
import io.quarkus.scheduler.runtime.ScheduledInvoker;
import io.quarkus.scheduler.runtime.ScheduledMethodMetadata;
import io.quarkus.scheduler.runtime.SchedulerContext;
import io.quarkus.scheduler.runtime.SchedulerRuntimeConfig;
import io.quarkus.scheduler.runtime.SimpleScheduler;
import io.quarkus.scheduler.runtime.SkipConcurrentExecutionInvoker;

@Singleton
public class QuartzScheduler implements Scheduler {

    private static final Logger LOGGER = Logger.getLogger(QuartzScheduler.class.getName());
    private static final String INVOKER_KEY = "invoker";

    private final org.quartz.Scheduler scheduler;
    private final boolean enabled;

    @Produces
    @Singleton
    org.quartz.Scheduler produceQuartzScheduler() {
        if (scheduler == null) {
            throw new IllegalStateException(
                    "Quartz scheduler is either explicitly disabled through quarkus.scheduler.enabled=false or no @Scheduled methods were found. If you only need to schedule a job programmatically you can force the start of the scheduler via quarkus.quartz.force-start=true");
        }
        return scheduler;
    }

    public QuartzScheduler(SchedulerContext context, QuartzSupport quartzSupport, Config config,
            SchedulerRuntimeConfig schedulerRuntimeConfig, Event<SkippedExecution> skippedExecutionEvent, Instance<Job> jobs) {
        enabled = schedulerRuntimeConfig.enabled;
        if (!enabled) {
            LOGGER.info("Quartz scheduler is disabled by config property and will not be started");
            this.scheduler = null;
        } else if (!quartzSupport.getRuntimeConfig().forceStart && context.getScheduledMethods().isEmpty()) {
            LOGGER.info("No scheduled business methods found - Quartz scheduler will not be started");
            this.scheduler = null;
        } else {
            // identity -> scheduled invoker instance
            Map<String, ScheduledInvoker> invokers = new HashMap<>();
            UserTransaction transaction = null;

            try (InstanceHandle<UserTransaction> handle = Arc.container().instance(UserTransaction.class)) {
                boolean manageTx = quartzSupport.getBuildTimeConfig().storeType.isNonManagedTxJobStore();
                if (manageTx && handle.isAvailable()) {
                    transaction = handle.get();
                }
                Properties props = getSchedulerConfigurationProperties(quartzSupport);

                SchedulerFactory schedulerFactory = new StdSchedulerFactory(props);
                scheduler = schedulerFactory.getScheduler();

                // Set custom job factory
                scheduler.setJobFactory(new InvokerJobFactory(invokers, jobs));

                CronType cronType = context.getCronType();
                CronDefinition def = CronDefinitionBuilder.instanceDefinitionFor(cronType);
                CronParser parser = new CronParser(def);
                if (transaction != null) {
                    transaction.begin();
                }
                for (ScheduledMethodMetadata method : context.getScheduledMethods()) {
                    int nameSequence = 0;

                    for (Scheduled scheduled : method.getSchedules()) {
                        String identity = scheduled.identity().trim();
                        if (identity.isEmpty()) {
                            identity = ++nameSequence + "_" + method.getInvokerClassName();
                        }
                        ScheduledInvoker invoker = context.createInvoker(method.getInvokerClassName());
                        if (scheduled.concurrentExecution() == ConcurrentExecution.SKIP) {
                            invoker = new SkipConcurrentExecutionInvoker(invoker, skippedExecutionEvent);
                        }
                        invokers.put(identity, invoker);

                        JobBuilder jobBuilder = JobBuilder.newJob(InvokerJob.class)
                                // new JobKey(identity, "io.quarkus.scheduler.Scheduler")
                                .withIdentity(identity, Scheduler.class.getName())
                                // this info is redundant but keep it for backward compatibility
                                .usingJobData(INVOKER_KEY, method.getInvokerClassName())
                                .requestRecovery();
                        ScheduleBuilder<?> scheduleBuilder;

                        String cron = scheduled.cron().trim();
                        if (!cron.isEmpty()) {
                            if (SchedulerContext.isConfigValue(cron)) {
                                cron = config.getValue(SchedulerContext.getConfigProperty(cron), String.class);
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
                            scheduleBuilder = CronScheduleBuilder.cronSchedule(cron);
                        } else if (!scheduled.every().isEmpty()) {
                            scheduleBuilder = SimpleScheduleBuilder.simpleSchedule()
                                    .withIntervalInMilliseconds(
                                            SimpleScheduler.parseDuration(scheduled, scheduled.every(), "every").toMillis())
                                    .repeatForever();
                        } else {
                            throw new IllegalArgumentException("Invalid schedule configuration: " + scheduled);
                        }

                        TriggerBuilder<?> triggerBuilder = TriggerBuilder.newTrigger()
                                .withIdentity(identity + "_trigger", Scheduler.class.getName())
                                .withSchedule(scheduleBuilder);

                        Long millisToAdd = null;
                        if (scheduled.delay() > 0) {
                            millisToAdd = scheduled.delayUnit().toMillis(scheduled.delay());
                        } else if (!scheduled.delayed().isEmpty()) {
                            millisToAdd = Math
                                    .abs(SimpleScheduler.parseDuration(scheduled, scheduled.delayed(), "delayed").toMillis());
                        }
                        if (millisToAdd != null) {
                            triggerBuilder.startAt(new Date(Instant.now()
                                    .plusMillis(millisToAdd).toEpochMilli()));
                        }

                        JobDetail job = jobBuilder.build();
                        if (scheduler.checkExists(job.getKey())) {
                            scheduler.deleteJob(job.getKey());
                        }
                        scheduler.scheduleJob(job, triggerBuilder.build());
                        LOGGER.debugf("Scheduled business method %s with config %s", method.getMethodDescription(),
                                scheduled);
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
                LOGGER.warn("Unable to pause scheduler", e);
            }
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
                LOGGER.warn("Unable to resume scheduler", e);
            }
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

    // Use Interceptor.Priority.PLATFORM_BEFORE to start the scheduler before regular StartupEvent observers
    void start(@Observes @Priority(Interceptor.Priority.PLATFORM_BEFORE) StartupEvent startupEvent) {
        if (scheduler == null) {
            return;
        }
        try {
            scheduler.start();
        } catch (SchedulerException e) {
            throw new IllegalStateException("Unable to start Scheduler", e);
        }
    }

    /**
     * Need to gracefully shutdown the scheduler making sure that all triggers have been
     * released before datasource shutdown.
     *
     * @param event ignored
     */
    void destroy(@BeforeDestroyed(ApplicationScoped.class) Object event) { //
        if (scheduler != null) {
            try {
                scheduler.shutdown(true); // gracefully shutdown
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
            props.put(StdSchedulerFactory.PROP_JOB_STORE_PREFIX + ".misfireThreshold", "60000");
            props.put(StdSchedulerFactory.PROP_JOB_STORE_PREFIX + ".tablePrefix", buildTimeConfig.tablePrefix);
            props.put(StdSchedulerFactory.PROP_JOB_STORE_PREFIX + ".dataSource", dataSource);
            props.put(StdSchedulerFactory.PROP_JOB_STORE_PREFIX + ".driverDelegateClass",
                    quartzSupport.getDriverDialect().get());
            props.put(StdSchedulerFactory.PROP_DATASOURCE_PREFIX + "." + dataSource + ".connectionProvider.class",
                    QuarkusQuartzConnectionPoolProvider.class.getName());
            if (buildTimeConfig.clustered) {
                props.put(StdSchedulerFactory.PROP_JOB_STORE_PREFIX + ".isClustered", "true");
                props.put(StdSchedulerFactory.PROP_JOB_STORE_PREFIX + ".clusterCheckinInterval", "20000"); // 20 seconds
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

        final Map<String, ScheduledInvoker> invokers;

        InvokerJob(Map<String, ScheduledInvoker> invokers) {
            this.invokers = invokers;
        }

        @Override
        public void execute(JobExecutionContext context) {
            QuartzTrigger trigger = new QuartzTrigger(context);
            ScheduledInvoker scheduledInvoker = invokers.get(context.getJobDetail().getKey().getName());
            if (scheduledInvoker != null) { // could be null from previous runs
                scheduledInvoker.invoke(new QuartzScheduledExecution(trigger));
            }
        }
    }

    static class QuartzTrigger implements Trigger {

        final JobExecutionContext context;

        public QuartzTrigger(JobExecutionContext context) {
            this.context = context;
        }

        @Override
        public Instant getNextFireTime() {
            Date nextFireTime = context.getTrigger().getNextFireTime();
            return nextFireTime != null ? nextFireTime.toInstant() : null;
        }

        @Override
        public Instant getPreviousFireTime() {
            Date previousFireTime = context.getTrigger().getPreviousFireTime();
            return previousFireTime != null ? previousFireTime.toInstant() : null;
        }

        @Override
        public String getId() {
            return context.getTrigger().getKey().toString();
        }

    }

    static class QuartzScheduledExecution implements ScheduledExecution {

        final QuartzTrigger trigger;

        public QuartzScheduledExecution(QuartzTrigger trigger) {
            this.trigger = trigger;
        }

        @Override
        public Trigger getTrigger() {
            return trigger;
        }

        @Override
        public Instant getFireTime() {
            return trigger.context.getScheduledFireTime().toInstant();
        }

        @Override
        public Instant getScheduledFireTime() {
            return trigger.context.getFireTime().toInstant();
        }

    }

    static class InvokerJobFactory extends SimpleJobFactory {

        final Map<String, ScheduledInvoker> invokers;
        final Instance<Job> jobs;

        InvokerJobFactory(Map<String, ScheduledInvoker> invokers, Instance<Job> jobs) {
            this.invokers = invokers;
            this.jobs = jobs;
        }

        @Override
        public Job newJob(TriggerFiredBundle bundle, org.quartz.Scheduler Scheduler) throws SchedulerException {
            Class<? extends Job> jobClass = bundle.getJobDetail().getJobClass();
            if (jobClass.equals(InvokerJob.class)) {
                return new InvokerJob(invokers);
            }
            Instance<?> instance = jobs.select(jobClass);
            if (instance.isResolvable()) {
                return (Job) instance.get();
            }
            return super.newJob(bundle, Scheduler);
        }

    }

}
