package io.quarkus.scheduler.runtime;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import javax.annotation.PreDestroy;
import javax.annotation.Priority;
import javax.enterprise.event.Observes;
import javax.enterprise.inject.Typed;
import javax.inject.Singleton;
import javax.interceptor.Interceptor;

import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.spi.ConfigProviderResolver;
import org.jboss.logging.Logger;
import org.jboss.threads.JBossScheduledThreadPoolExecutor;

import com.cronutils.model.Cron;
import com.cronutils.model.definition.CronDefinition;
import com.cronutils.model.definition.CronDefinitionBuilder;
import com.cronutils.model.time.ExecutionTime;
import com.cronutils.parser.CronParser;

import io.quarkus.runtime.StartupEvent;
import io.quarkus.scheduler.Scheduled;
import io.quarkus.scheduler.ScheduledExecution;
import io.quarkus.scheduler.Scheduler;
import io.quarkus.scheduler.Trigger;

@Typed(Scheduler.class)
@Singleton
public class SimpleScheduler implements Scheduler {

    private static final Logger LOGGER = Logger.getLogger(SimpleScheduler.class);

    // milliseconds
    private static final long CHECK_PERIOD = 1000L;

    private final ScheduledExecutorService scheduledExecutor;
    private final ExecutorService executor;
    private volatile boolean running;
    private final List<ScheduledTask> scheduledTasks;

    public SimpleScheduler(SchedulerContext context, Config config) {
        this.running = true;
        this.scheduledTasks = new ArrayList<>();
        this.executor = context.getExecutor();

        if (context.getScheduledMethods().isEmpty()) {
            this.scheduledExecutor = null;
        } else {
            this.scheduledExecutor = new JBossScheduledThreadPoolExecutor(1, new Runnable() {
                @Override
                public void run() {
                    // noop
                }
            });

            CronDefinition definition = CronDefinitionBuilder.instanceDefinitionFor(context.getCronType());
            CronParser parser = new CronParser(definition);

            for (ScheduledMethodMetadata method : context.getScheduledMethods()) {
                ScheduledInvoker invoker = context.createInvoker(method.getInvokerClassName());
                int nameSequence = 0;
                for (Scheduled scheduled : method.getSchedules()) {
                    nameSequence++;
                    SimpleTrigger trigger = createTrigger(method.getInvokerClassName(), parser, scheduled, nameSequence,
                            config);
                    scheduledTasks.add(new ScheduledTask(trigger, invoker));
                }
            }
        }
    }

    // Use Interceptor.Priority.PLATFORM_BEFORE to start the scheduler before regular StartupEvent observers
    void start(@Observes @Priority(Interceptor.Priority.PLATFORM_BEFORE) StartupEvent event) {
        if (scheduledExecutor == null) {
            return;
        }
        // Try to compute the initial delay to execute the checks near to the whole second
        // Note that this does not guarantee anything, it's just best effort
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime trunc = now.plusSeconds(1).truncatedTo(ChronoUnit.SECONDS);
        scheduledExecutor.scheduleAtFixedRate(this::checkTriggers, ChronoUnit.MILLIS.between(now, trunc), CHECK_PERIOD,
                TimeUnit.MILLISECONDS);
    }

    @PreDestroy
    void stop() {
        try {
            if (scheduledExecutor != null) {
                scheduledExecutor.shutdownNow();
            }
        } catch (Exception e) {
            LOGGER.warn("Unable to shutdown the scheduler executor", e);
        }
    }

    void checkTriggers() {
        if (!running) {
            LOGGER.trace("Skip all triggers - scheduler paused");
        }
        ZonedDateTime now = ZonedDateTime.now();

        for (ScheduledTask task : scheduledTasks) {
            LOGGER.tracef("Evaluate trigger %s", task.trigger);
            ZonedDateTime scheduledFireTime = task.trigger.evaluate(now);
            if (scheduledFireTime != null) {
                try {
                    executor.execute(new Runnable() {
                        @Override
                        public void run() {
                            try {
                                task.invoker.invoke(new SimpleScheduledExecution(now, scheduledFireTime, task.trigger));
                            } catch (Throwable t) {
                                LOGGER.errorf(t, "Error occured while executing task for trigger %s", task.trigger);
                            }
                        }
                    });
                    LOGGER.debugf("Executing scheduled task for trigger %s", task.trigger);
                } catch (RejectedExecutionException e) {
                    LOGGER.warnf("Rejected execution of a scheduled task for trigger %s", task.trigger);
                }
            }
        }
    }

    @Override
    public void pause() {
        running = false;
    }

    @Override
    public void resume() {
        running = true;
    }

    SimpleTrigger createTrigger(String invokerClass, CronParser parser, Scheduled scheduled, int nameSequence, Config config) {
        String id = scheduled.identity().trim();
        if (id.isEmpty()) {
            id = nameSequence + "_" + invokerClass;
        }
        ZonedDateTime start = ZonedDateTime.now().truncatedTo(ChronoUnit.SECONDS);
        Long millisToAdd = null;
        if (scheduled.delay() > 0) {
            millisToAdd = scheduled.delayUnit().toMillis(scheduled.delay());
        } else if (!scheduled.delayed().isEmpty()) {
            millisToAdd = Math.abs(parseDuration(scheduled, scheduled.delayed(), "delayed").toMillis());
        }
        if (millisToAdd != null) {
            start = start.toInstant().plusMillis(millisToAdd).atZone(start.getZone());
        }

        String cron = scheduled.cron().trim();
        if (!cron.isEmpty()) {
            if (SchedulerContext.isConfigValue(cron)) {
                cron = config.getValue(SchedulerContext.getConfigProperty(cron), String.class);
            }
            Cron cronExpr;
            try {
                cronExpr = parser.parse(cron);
            } catch (IllegalArgumentException e) {
                throw new IllegalArgumentException("Cannot parse cron expression: " + cron, e);
            }
            return new CronTrigger(id, start, cronExpr);
        } else if (!scheduled.every().isEmpty()) {
            return new IntervalTrigger(id, start, Math.abs(parseDuration(scheduled, scheduled.every(), "every").toMillis()));
        } else {
            throw new IllegalArgumentException("Invalid schedule configuration: " + scheduled);
        }
    }

    // Keep it public so that we can reuse the logic in the quartz extension
    public static Duration parseDuration(Scheduled scheduled, String value, String memberName) {
        value = value.trim();
        if (SchedulerContext.isConfigValue(value)) {
            value = ConfigProviderResolver.instance().getConfig().getValue(SchedulerContext.getConfigProperty(value),
                    String.class);
        }
        if (Character.isDigit(value.charAt(0))) {
            value = "PT" + value;
        }
        try {
            return Duration.parse(value);
        } catch (Exception e) {
            // This could only happen for config-based expressions
            throw new IllegalStateException("Invalid " + memberName + "() expression on: " + scheduled, e);
        }
    }

    static class ScheduledTask {

        final SimpleTrigger trigger;
        final ScheduledInvoker invoker;

        public ScheduledTask(SimpleTrigger trigger, ScheduledInvoker invoker) {
            this.trigger = trigger;
            this.invoker = invoker;
        }

    }

    static abstract class SimpleTrigger implements Trigger {

        private final String id;
        protected final ZonedDateTime start;

        public SimpleTrigger(String id, ZonedDateTime start) {
            this.id = id;
            this.start = start;
        }

        /**
         * 
         * @param now
         * @return the scheduled time if fired, {@code null} otherwise
         */
        abstract ZonedDateTime evaluate(ZonedDateTime now);

        public String getId() {
            return id;
        }

    }

    static class IntervalTrigger extends SimpleTrigger {

        private final long interval;
        private volatile ZonedDateTime lastFireTime;

        public IntervalTrigger(String id, ZonedDateTime start, long interval) {
            super(id, start);
            this.interval = interval;
        }

        @Override
        ZonedDateTime evaluate(ZonedDateTime now) {
            if (now.isBefore(start)) {
                return null;
            }
            if (lastFireTime == null) {
                // First execution
                lastFireTime = now.truncatedTo(ChronoUnit.SECONDS);
                return now;
            }
            if (ChronoUnit.MILLIS.between(lastFireTime, now) >= interval) {
                ZonedDateTime scheduledFireTime = lastFireTime.plus(Duration.ofMillis(interval));
                lastFireTime = now.truncatedTo(ChronoUnit.SECONDS);
                return scheduledFireTime;
            }
            return null;
        }

        @Override
        public Instant getNextFireTime() {
            return lastFireTime.plus(Duration.ofMillis(interval)).toInstant();
        }

        @Override
        public Instant getPreviousFireTime() {
            return lastFireTime.toInstant();
        }

        @Override
        public String toString() {
            StringBuilder builder = new StringBuilder();
            builder.append("IntervalTrigger [id=").append(getId()).append(", interval=").append(interval).append("]");
            return builder.toString();
        }

    }

    static class CronTrigger extends SimpleTrigger {

        // microseconds
        private static final long DIFF_THRESHOLD = CHECK_PERIOD * 1000;

        private final Cron cron;
        private final ExecutionTime executionTime;

        public CronTrigger(String id, ZonedDateTime start, Cron cron) {
            super(id, start);
            this.cron = cron;
            this.executionTime = ExecutionTime.forCron(cron);
        }

        @Override
        public Instant getNextFireTime() {
            Optional<ZonedDateTime> nextFireTime = executionTime.nextExecution(ZonedDateTime.now());
            return nextFireTime.isPresent() ? nextFireTime.get().toInstant() : null;
        }

        @Override
        public Instant getPreviousFireTime() {
            Optional<ZonedDateTime> prevFireTime = executionTime.lastExecution(ZonedDateTime.now());
            return prevFireTime.isPresent() ? prevFireTime.get().toInstant() : null;
        }

        ZonedDateTime evaluate(ZonedDateTime now) {
            if (now.isBefore(start)) {
                return null;
            }
            Optional<ZonedDateTime> lastFireTime = executionTime.lastExecution(now);
            if (lastFireTime.isPresent()) {
                ZonedDateTime trunc = lastFireTime.get().truncatedTo(ChronoUnit.SECONDS);
                if (now.isBefore(trunc)) {
                    return null;
                }
                // Use microseconds precision to workaround incompatibility between jdk8 and jdk9+
                long diff = ChronoUnit.MICROS.between(trunc, now);
                if (diff <= DIFF_THRESHOLD) {
                    LOGGER.debugf("%s fired, diff=%s μs", this, diff);
                    return trunc;
                }
            }
            return null;
        }

        @Override
        public String toString() {
            StringBuilder builder = new StringBuilder();
            builder.append("CronTrigger [id=").append(getId()).append(", cron=").append(cron.asString()).append("]");
            return builder.toString();
        }

    }

    static class SimpleScheduledExecution implements ScheduledExecution {

        private final ZonedDateTime fireTime;
        private final ZonedDateTime scheduledFireTime;
        private final Trigger trigger;

        public SimpleScheduledExecution(ZonedDateTime fireTime, ZonedDateTime scheduledFireTime, SimpleTrigger trigger) {
            this.fireTime = fireTime;
            this.scheduledFireTime = scheduledFireTime;
            this.trigger = trigger;
        }

        @Override
        public Trigger getTrigger() {
            return trigger;
        }

        @Override
        public Instant getFireTime() {
            return fireTime.toInstant();
        }

        @Override
        public Instant getScheduledFireTime() {
            return scheduledFireTime.toInstant();
        }

    }

}
