package io.quarkus.scheduler.runtime;

import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import javax.annotation.PreDestroy;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;
import javax.enterprise.inject.Typed;
import javax.inject.Inject;

import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.ConfigProvider;
import org.graalvm.nativeimage.ImageInfo;
import org.jboss.logging.Logger;
import org.quartz.CronScheduleBuilder;
import org.quartz.Job;
import org.quartz.JobBuilder;
import org.quartz.JobDetail;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.quartz.ScheduleBuilder;
import org.quartz.SchedulerException;
import org.quartz.SchedulerFactory;
import org.quartz.SimpleScheduleBuilder;
import org.quartz.TriggerBuilder;
import org.quartz.impl.StdSchedulerFactory;
import org.quartz.spi.JobFactory;
import org.quartz.spi.TriggerFiredBundle;

import io.quarkus.runtime.StartupEvent;
import io.quarkus.scheduler.Scheduled;
import io.quarkus.scheduler.ScheduledExecution;
import io.quarkus.scheduler.Scheduler;
import io.quarkus.scheduler.Trigger;

/**
 *
 * @author Martin Kouba
 */
@Typed(Scheduler.class)
@ApplicationScoped
public class QuartzScheduler implements Scheduler {

    private static final Logger LOGGER = Logger.getLogger(QuartzScheduler.class.getName());

    @Inject
    SchedulerConfiguration schedulerConfig;

    private final AtomicBoolean running = new AtomicBoolean(false);

    private org.quartz.Scheduler scheduler;

    private final Map<String, ScheduledInvoker> invokers = new ConcurrentHashMap<>();

    private final AtomicInteger timerIdx = new AtomicInteger();

    private final Map<String, Runnable> timers = new ConcurrentHashMap<>();

    @Override
    public void pause() {
        if (running.get()) {
            try {
                scheduler.pauseAll();
            } catch (SchedulerException e) {
                LOGGER.warn("Unable to pause scheduler", e);
            }
        }
    }

    @Override
    public void resume() {
        if (running.get()) {
            try {
                scheduler.resumeAll();
            } catch (SchedulerException e) {
                LOGGER.warn("Unable to resume scheduler", e);
            }
        }
    }

    @Override
    public void startTimer(long delay, Runnable action) {
        if (running.get()) {
            int idx = timerIdx.incrementAndGet();
            String name = "timer_" + idx;
            timers.put(name, action);
            // Impl note: we can only stateStore primitives in JobDataMap
            JobDetail job = JobBuilder.newJob(TimerJob.class).withIdentity(name, Scheduler.class.getName()).build();
            org.quartz.Trigger trigger = TriggerBuilder.newTrigger().withIdentity(name + "_trigger", Scheduler.class.getName())
                    .startAt(new Date(Instant.now().plusMillis(delay).toEpochMilli()))
                    .withSchedule(SimpleScheduleBuilder.simpleSchedule()).build();

            try {
                scheduler.scheduleJob(job, trigger);
            } catch (SchedulerException e) {
                timers.remove(name);
                throw new IllegalStateException("Unable to schedule timer", e);
            }
        } else {
            LOGGER.warn("Scheduler not running");
        }
    }

    void start(@Observes StartupEvent startupEvent) {
        if (running.compareAndSet(false, true)) {

            try {
                Properties props = getSchedulerConfigurationProperties();

                SchedulerFactory schedulerFactory = new StdSchedulerFactory(props);
                scheduler = schedulerFactory.getScheduler();

                // Set custom job factory
                scheduler.setJobFactory(new JobFactory() {

                    @Override
                    public Job newJob(TriggerFiredBundle bundle, org.quartz.Scheduler scheduler) throws SchedulerException {
                        Class<? extends Job> jobClass = bundle.getJobDetail().getJobClass();
                        if (jobClass.equals(InvokerJob.class)) {
                            return new InvokerJob();
                        } else if (jobClass.equals(TimerJob.class)) {
                            return new TimerJob();
                        }
                        throw new IllegalStateException("Unsupported job class: " + jobClass);
                    }
                });

                for (Entry<String, List<Scheduled>> entry : schedulerConfig.getSchedules().entrySet()) {

                    Config config = ConfigProvider.getConfig();
                    int idx = 1;

                    for (Scheduled scheduled : entry.getValue()) {

                        // Job name: 1_MyService_Invoker
                        String name = idx++ + "_" + entry.getKey();
                        JobBuilder jobBuilder = JobBuilder.newJob(InvokerJob.class)
                                .withIdentity(name, Scheduler.class.getName())
                                .usingJobData(SchedulerDeploymentRecorder.INVOKER_KEY, entry.getKey());
                        ScheduleBuilder<?> scheduleBuilder;

                        String cron = scheduled.cron().trim();
                        if (!cron.isEmpty()) {
                            try {
                                if (SchedulerConfiguration.isConfigValue(cron)) {
                                    cron = config.getValue(SchedulerConfiguration.getConfigProperty(cron), String.class);
                                }
                                scheduleBuilder = CronScheduleBuilder.cronSchedule(cron);
                            } catch (RuntimeException e) {
                                // This should only happen for config-based expressions
                                throw new IllegalStateException("Invalid cron() expression on: " + scheduled, e);
                            }
                        } else if (!scheduled.every().isEmpty()) {
                            String every = scheduled.every().trim();
                            if (SchedulerConfiguration.isConfigValue(every)) {
                                every = config.getValue(SchedulerConfiguration.getConfigProperty(every), String.class);
                            }
                            if (Character.isDigit(every.charAt(0))) {
                                every = "PT" + every;
                            }
                            Duration duration;
                            try {
                                duration = Duration.parse(every);
                            } catch (Exception e) {
                                // This should only happen for config-based expressions
                                throw new IllegalStateException("Invalid every() expression on: " + scheduled, e);
                            }
                            scheduleBuilder = SimpleScheduleBuilder.simpleSchedule()
                                    .withIntervalInMilliseconds(duration.toMillis()).repeatForever();
                        } else {
                            throw new IllegalArgumentException("Invalid schedule configuration: " + scheduled);
                        }

                        TriggerBuilder<?> triggerBuilder = TriggerBuilder.newTrigger()
                                .withIdentity(name + "_trigger", Scheduler.class.getName())
                                .withSchedule(scheduleBuilder);
                        if (scheduled.delay() > 0) {
                            triggerBuilder.startAt(new Date(Instant.now()
                                    .plusMillis(scheduled.delayUnit().toMillis(scheduled.delay())).toEpochMilli()));
                        }
                        scheduler.scheduleJob(jobBuilder.build(), triggerBuilder.build());
                        LOGGER.debugf("Scheduled business method %s with config %s",
                                schedulerConfig.getDescription(entry.getKey()), scheduled);
                    }
                }

                scheduler.start();

            } catch (SchedulerException e) {
                throw new IllegalStateException("Unable to start Scheduler", e);
            }
        } else {
            LOGGER.warnf("Unable to start scheduler - already started");
        }
    }

    private Properties getSchedulerConfigurationProperties() {
        Properties props = new Properties();
        SchedulerRuntimeConfig schedulerRuntimeConfig = SchedulerConfigHolder.getSchedulerRuntimeConfig();
        SchedulerBuildTimeConfig schedulerBuildTimeConfig = SchedulerConfigHolder.getSchedulerBuildTimeConfig();

        props.put(StdSchedulerFactory.PROP_SCHED_WRAP_JOB_IN_USER_TX, false);
        props.put(StdSchedulerFactory.PROP_THREAD_POOL_CLASS, "org.quartz.simpl.SimpleThreadPool");
        props.put(StdSchedulerFactory.PROP_SCHED_INSTANCE_ID, schedulerRuntimeConfig.instanceId);
        props.put("org.quartz.threadPool.threadsInheritContextClassLoaderOfInitializingThread", true);
        props.put(StdSchedulerFactory.PROP_SCHED_INSTANCE_NAME, schedulerBuildTimeConfig.instanceName);
        props.put("org.quartz.threadPool.threadCount", String.valueOf(schedulerRuntimeConfig.threadCount));
        props.put("org.quartz.threadPool.threadPriority", String.valueOf(schedulerRuntimeConfig.threadPriority));
        props.put("org.quartz.jobStore.misfireThreshold",
                String.valueOf(schedulerBuildTimeConfig.stateStore.misfireThreshold.toMillis()));

        StateStoreType stateStore = schedulerBuildTimeConfig.stateStore.type;
        switch (stateStore) {
            case IN_MEMORY:
                props.put(StdSchedulerFactory.PROP_JOB_STORE_CLASS, StateStoreType.IN_MEMORY.clazz);
                break;
            case JDBC: {
                if (ImageInfo.inImageRuntimeCode()) {
                    /**
                     * Defaulting to {@link org.quartz.simpl.RAMJobStore} since Quartz scheduler relies on Object
                     * serialization to persist job details in database.
                     * This is feature is not supported yet in Native Image.
                     * See https://github.com/quarkusio/quarkus/issues/2656
                     * See https://github.com/oracle/graal/issues/460
                     */
                    props.put(StdSchedulerFactory.PROP_JOB_STORE_CLASS, StateStoreType.IN_MEMORY.clazz);
                    LOGGER.warnf("Quartz scheduler relies on Object serialization to persist job details in database. " +
                            "This feature is not supported yet in Native Image: see https://github.com/oracle/graal/issues/460. Defaulting to usage of RAMJobStore.");
                } else {
                    props.put(StdSchedulerFactory.PROP_JOB_STORE_CLASS, StateStoreType.JDBC.clazz);
                    String dataSourceName = schedulerBuildTimeConfig.stateStore.datasource.name.orElse("QUARKUS_SCHEDULER_DS");
                    props.put("org.quartz.jobStore.useProperties", true);
                    props.put("org.quartz.jobStore.dataSource", dataSourceName);
                    props.put("org.quartz.jobStore.tablePrefix", "QRTZ_");
                    props.put("org.quartz.jobStore.driverDelegateClass",
                            schedulerBuildTimeConfig.stateStore.datasource.driverDelegateClass);
                    props.put("org.quartz.dataSource." + dataSourceName + ".connectionProvider.class",
                            AgroalQuartzConnectionPoolingProvider.class.getName());
                    Optional<Boolean> clusterEnabled = schedulerBuildTimeConfig.stateStore.clusterEnabled;
                    if (clusterEnabled.isPresent()) {
                        String interval = "20000"; // Default to 20 seconds
                        Optional<Duration> clusterCheckingInterval = schedulerBuildTimeConfig.stateStore.clusterCheckingInterval;
                        if (clusterCheckingInterval.isPresent()) {
                            interval = String.valueOf(clusterCheckingInterval.get().toMillis());
                        }
                        props.put("org.quartz.jobStore.isClustered", clusterEnabled.get().booleanValue());
                        props.put("org.quartz.jobStore.clusterCheckinInterval", interval);
                    }
                }
            }
        }

        return props;
    }

    @PreDestroy
    void destroy() {
        if (running.compareAndSet(true, false)) {
            if (scheduler != null) {
                try {
                    scheduler.shutdown();
                } catch (SchedulerException e) {
                    LOGGER.warnf("Unable to shutdown scheduler", e);
                }
            }
        }
    }

    class InvokerJob implements Job {

        @Override
        public void execute(JobExecutionContext context) throws JobExecutionException {
            Trigger trigger = new Trigger() {

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
            };
            String invokerClass = context.getJobDetail().getJobDataMap().getString(SchedulerDeploymentRecorder.INVOKER_KEY);
            invokers.computeIfAbsent(invokerClass, schedulerConfig::createInvoker).invoke(new ScheduledExecution() {

                @Override
                public Trigger getTrigger() {
                    return trigger;
                }

                @Override
                public Instant getScheduledFireTime() {
                    return context.getScheduledFireTime().toInstant();
                }

                @Override
                public Instant getFireTime() {
                    return context.getFireTime().toInstant();
                }
            });
        }

    }

    class TimerJob implements Job {

        @Override
        public void execute(JobExecutionContext context) throws JobExecutionException {
            Runnable action = timers.remove(context.getJobDetail().getKey().getName());
            if (action != null) {
                action.run();
            } else {
                LOGGER.warnf("No timer action found for key: %s", context.getJobDetail().getKey());
            }
            try {
                scheduler.deleteJob(context.getJobDetail().getKey());
            } catch (SchedulerException e) {
                LOGGER.warnf(e, "Unable to delete timer job for key: %s", context.getJobDetail().getKey());
            }
        }

    }

}
