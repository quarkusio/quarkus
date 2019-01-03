/*
 * Copyright 2018 Red Hat, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jboss.shamrock.scheduler.runtime;

import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
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
import org.jboss.logging.Logger;
import org.jboss.shamrock.runtime.StartupEvent;
import org.jboss.shamrock.scheduler.api.Scheduled;
import org.jboss.shamrock.scheduler.api.ScheduledExecution;
import org.jboss.shamrock.scheduler.api.Scheduler;
import org.jboss.shamrock.scheduler.api.Trigger;
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
            // Impl note: we can only store primitives in JobDataMap
            JobDetail job = JobBuilder.newJob(TimerJob.class).withIdentity(name, Scheduler.class.getName()).build();
            org.quartz.Trigger trigger = TriggerBuilder.newTrigger().withIdentity(name + "_trigger", Scheduler.class.getName())
                    .startAt(new Date(Instant.now().plusMillis(delay).toEpochMilli())).withSchedule(SimpleScheduleBuilder.simpleSchedule()).build();

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
                // TODO: leverage shamrock config - these values are just copied from the default quartz.properties
                Properties props = new Properties();
                props.put("org.quartz.scheduler.instanceName", "DefaultQuartzScheduler");
                props.put("org.quartz.scheduler.rmi.export", false);
                props.put("org.quartz.scheduler.rmi.proxy", false);
                props.put("org.quartz.scheduler.wrapJobExecutionInUserTransaction", false);
                props.put("org.quartz.threadPool.class", "org.quartz.simpl.SimpleThreadPool");
                props.put("org.quartz.threadPool.threadCount", "10");
                props.put("org.quartz.threadPool.threadPriority", "5");
                props.put("org.quartz.threadPool.threadsInheritContextClassLoaderOfInitializingThread", true);
                props.put("org.quartz.threadPool.threadPriority", "5");
                props.put("org.quartz.jobStore.misfireThreshold", "60000");
                props.put("org.quartz.jobStore.class", "org.quartz.simpl.RAMJobStore");

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
                        JobBuilder jobBuilder = JobBuilder.newJob(InvokerJob.class).withIdentity(name, Scheduler.class.getName())
                                .usingJobData(SchedulerDeploymentTemplate.INVOKER_KEY, entry.getKey());
                        ScheduleBuilder<?> scheduleBuilder;

                        String cron = scheduled.cron();
                        if (!cron.isEmpty()) {
                            try {
                                if (cron.startsWith("{") && cron.endsWith("}")) {
                                    cron = config.getValue(cron.substring(1, cron.length() - 1), String.class);
                                }
                                scheduleBuilder = CronScheduleBuilder.cronSchedule(cron);
                            } catch (RuntimeException e) {
                                LOGGER.warnf(e, "Invalid CRON expression: %s", cron);
                                continue;
                            }
                        } else if (!scheduled.every().isEmpty()) {
                            String every = scheduled.every();
                            if (every.startsWith("{") && every.endsWith("}")) {
                                every = config.getValue(every.substring(1, every.length() - 1), String.class);
                            }
                            if (Character.isDigit(every.charAt(0))) {
                                every = "PT" + every;
                            }
                            Duration duration;
                            try {
                                duration = Duration.parse(every);
                            } catch (Exception e) {
                                LOGGER.warnf(e, "Invalid period expression: %s", scheduled.every());
                                continue;
                            }
                            scheduleBuilder = SimpleScheduleBuilder.simpleSchedule().withIntervalInMilliseconds(duration.toMillis()).repeatForever();
                        } else {
                            throw new IllegalArgumentException("Invalid schedule configuration: " + scheduled);
                        }

                        TriggerBuilder<?> triggerBuilder = TriggerBuilder.newTrigger().withIdentity(name + "_trigger", Scheduler.class.getName())
                                .withSchedule(scheduleBuilder);
                        if (scheduled.delay() > 0) {
                            triggerBuilder.startAt(new Date(Instant.now().plusMillis(scheduled.delayUnit().toMillis(scheduled.delay())).toEpochMilli()));
                        }
                        scheduler.scheduleJob(jobBuilder.build(), triggerBuilder.build());
                        LOGGER.debugf("Scheduled business method %s with config %s", schedulerConfig.getDescription(entry.getKey()), scheduled);
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
            String invokerClass = context.getJobDetail().getJobDataMap().getString(SchedulerDeploymentTemplate.INVOKER_KEY);
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
