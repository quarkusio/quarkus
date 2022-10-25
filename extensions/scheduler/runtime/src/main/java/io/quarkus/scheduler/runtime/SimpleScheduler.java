package io.quarkus.scheduler.runtime;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.OptionalLong;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import jakarta.annotation.PreDestroy;
import jakarta.annotation.Priority;
import jakarta.enterprise.event.Event;
import jakarta.enterprise.event.Observes;
import jakarta.enterprise.inject.Any;
import jakarta.enterprise.inject.Typed;
import jakarta.inject.Singleton;
import jakarta.interceptor.Interceptor;

import org.jboss.logging.Logger;
import org.jboss.threads.JBossScheduledThreadPoolExecutor;

import com.cronutils.model.Cron;
import com.cronutils.model.definition.CronDefinition;
import com.cronutils.model.definition.CronDefinitionBuilder;
import com.cronutils.model.time.ExecutionTime;
import com.cronutils.parser.CronParser;

import io.quarkus.arc.Arc;
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
import io.quarkus.vertx.core.runtime.context.VertxContextSafetyToggle;
import io.smallrye.common.vertx.VertxContext;
import io.vertx.core.Context;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;

@Typed(Scheduler.class)
@Singleton
public class SimpleScheduler implements Scheduler {

    private static final Logger LOG = Logger.getLogger(SimpleScheduler.class);

    // milliseconds
    private static final long CHECK_PERIOD = 1000L;

    private final ScheduledExecutorService scheduledExecutor;
    private final ExecutorService executor;
    private final Vertx vertx;
    private volatile boolean running;
    private final List<ScheduledTask> scheduledTasks;
    private final boolean enabled;

    public SimpleScheduler(SchedulerContext context, SchedulerRuntimeConfig schedulerRuntimeConfig,
            Event<SkippedExecution> skippedExecutionEvent, Event<SuccessfulExecution> successExecutionEvent,
            Event<FailedExecution> failedExecutionEvent, Vertx vertx) {
        this.running = true;
        this.enabled = schedulerRuntimeConfig.enabled;
        this.scheduledTasks = new ArrayList<>();
        this.executor = context.getExecutor();
        this.vertx = vertx;

        if (!schedulerRuntimeConfig.enabled) {
            this.scheduledExecutor = null;
            LOG.info("Simple scheduler is disabled by config property and will not be started");
        } else if (context.getScheduledMethods().isEmpty()) {
            this.scheduledExecutor = null;
            LOG.info("No scheduled business methods found - Simple scheduler will not be started");
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
                int nameSequence = 0;
                for (Scheduled scheduled : method.getSchedules()) {
                    nameSequence++;
                    Optional<SimpleTrigger> trigger = createTrigger(method.getInvokerClassName(), parser, scheduled,
                            nameSequence, schedulerRuntimeConfig.overdueGracePeriod);
                    if (trigger.isPresent()) {
                        ScheduledInvoker invoker = new StatusEmitterInvoker(context.createInvoker(method.getInvokerClassName()),
                                successExecutionEvent, failedExecutionEvent);
                        if (scheduled.concurrentExecution() == ConcurrentExecution.SKIP) {
                            invoker = new SkipConcurrentExecutionInvoker(invoker, skippedExecutionEvent);
                        }
                        if (!scheduled.skipExecutionIf().equals(Scheduled.Never.class)) {
                            invoker = new SkipPredicateInvoker(invoker,
                                    Arc.container().select(scheduled.skipExecutionIf(), Any.Literal.INSTANCE).get(),
                                    skippedExecutionEvent);
                        }
                        scheduledTasks.add(new ScheduledTask(trigger.get(), invoker));
                    }
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
            LOG.warn("Unable to shutdown the scheduler executor", e);
        }
    }

    void checkTriggers() {
        if (!running) {
            LOG.trace("Skip all triggers - scheduler paused");
            return;
        }
        ZonedDateTime now = ZonedDateTime.now();
        LOG.tracef("Check triggers at %s", now);
        for (ScheduledTask task : scheduledTasks) {
            task.execute(now, executor, vertx);
        }
    }

    @Override
    public void pause() {
        if (!enabled) {
            LOG.warn("Scheduler is disabled and cannot be paused");
        } else {
            running = false;
        }
    }

    @Override
    public void pause(String identity) {
        Objects.requireNonNull(identity, "Cannot pause - identity is null");
        if (identity.isEmpty()) {
            LOG.warn("Cannot pause - identity is empty");
            return;
        }
        String parsedIdentity = SchedulerUtils.lookUpPropertyValue(identity);
        for (ScheduledTask task : scheduledTasks) {
            if (parsedIdentity.equals(task.trigger.id)) {
                task.trigger.setRunning(false);
                return;
            }
        }
    }

    @Override
    public boolean isPaused(String identity) {
        Objects.requireNonNull(identity);
        if (identity.isEmpty()) {
            return false;
        }
        String parsedIdentity = SchedulerUtils.lookUpPropertyValue(identity);
        for (ScheduledTask task : scheduledTasks) {
            if (parsedIdentity.equals(task.trigger.id)) {
                return !task.trigger.isRunning();
            }
        }
        return false;
    }

    @Override
    public void resume() {
        if (!enabled) {
            LOG.warn("Scheduler is disabled and cannot be resumed");
        } else {
            running = true;
        }
    }

    @Override
    public void resume(String identity) {
        Objects.requireNonNull(identity, "Cannot resume - identity is null");
        if (identity.isEmpty()) {
            LOG.warn("Cannot resume - identity is empty");
            return;
        }
        String parsedIdentity = SchedulerUtils.lookUpPropertyValue(identity);
        for (ScheduledTask task : scheduledTasks) {
            if (parsedIdentity.equals(task.trigger.id)) {
                task.trigger.setRunning(true);
                return;
            }
        }
    }

    @Override
    public boolean isRunning() {
        return enabled && running;
    }

    Optional<SimpleTrigger> createTrigger(String invokerClass, CronParser parser, Scheduled scheduled, int nameSequence,
            Duration defaultGracePeriod) {
        String id = SchedulerUtils.lookUpPropertyValue(scheduled.identity());
        if (id.isEmpty()) {
            id = nameSequence + "_" + invokerClass;
        }
        ZonedDateTime start = ZonedDateTime.now().truncatedTo(ChronoUnit.SECONDS);
        Long millisToAdd = null;
        if (scheduled.delay() > 0) {
            millisToAdd = scheduled.delayUnit().toMillis(scheduled.delay());
        } else if (!scheduled.delayed().isEmpty()) {
            millisToAdd = SchedulerUtils.parseDelayedAsMillis(scheduled);
        }
        if (millisToAdd != null) {
            start = start.toInstant().plusMillis(millisToAdd).atZone(start.getZone());
        }

        String cron = SchedulerUtils.lookUpPropertyValue(scheduled.cron());
        if (!cron.isEmpty()) {
            if (SchedulerUtils.isOff(cron)) {
                return Optional.empty();
            }
            Cron cronExpr;
            try {
                cronExpr = parser.parse(cron);
            } catch (IllegalArgumentException e) {
                throw new IllegalArgumentException("Cannot parse cron expression: " + cron, e);
            }
            return Optional.of(new CronTrigger(id, start, cronExpr,
                    SchedulerUtils.parseOverdueGracePeriod(scheduled, defaultGracePeriod)));
        } else if (!scheduled.every().isEmpty()) {
            final OptionalLong everyMillis = SchedulerUtils.parseEveryAsMillis(scheduled);
            if (!everyMillis.isPresent()) {
                return Optional.empty();
            }
            return Optional.of(new IntervalTrigger(id, start, everyMillis.getAsLong(),
                    SchedulerUtils.parseOverdueGracePeriod(scheduled, defaultGracePeriod)));
        } else {
            throw new IllegalArgumentException("Invalid schedule configuration: " + scheduled);
        }
    }

    @Override
    public List<Trigger> getScheduledJobs() {
        return scheduledTasks.stream().map(task -> task.trigger).collect(Collectors.toUnmodifiableList());
    }

    @Override
    public Trigger getScheduledJob(String identity) {
        Objects.requireNonNull(identity);
        if (identity.isEmpty()) {
            return null;
        }
        String parsedIdentity = SchedulerUtils.lookUpPropertyValue(identity);
        for (ScheduledTask task : scheduledTasks) {
            if (parsedIdentity.equals(task.trigger.id)) {
                return task.trigger;
            }
        }
        return null;
    }

    static class ScheduledTask {

        final SimpleTrigger trigger;
        final ScheduledInvoker invoker;

        ScheduledTask(SimpleTrigger trigger, ScheduledInvoker invoker) {
            this.trigger = trigger;
            this.invoker = invoker;
        }

        void execute(ZonedDateTime now, ExecutorService executor, Vertx vertx) {
            if (!trigger.isRunning()) {
                return;
            }
            ZonedDateTime scheduledFireTime = trigger.evaluate(now);
            if (scheduledFireTime != null) {
                if (invoker.isBlocking()) {
                    try {
                        executor.execute(new Runnable() {
                            @Override
                            public void run() {
                                doInvoke(now, scheduledFireTime);
                            }
                        });
                    } catch (RejectedExecutionException e) {
                        LOG.warnf("Rejected execution of a scheduled task for trigger %s", trigger);
                    }
                } else {
                    Context context = VertxContext.getOrCreateDuplicatedContext(vertx);
                    VertxContextSafetyToggle.setContextSafe(context, true);
                    context.runOnContext(new Handler<Void>() {
                        @Override
                        public void handle(Void event) {
                            doInvoke(now, scheduledFireTime);
                        }
                    });
                }
            }
        }

        void doInvoke(ZonedDateTime now, ZonedDateTime scheduledFireTime) {
            try {
                invoker.invoke(new SimpleScheduledExecution(now, scheduledFireTime, trigger));
            } catch (Throwable t) {
                // already logged by the StatusEmitterInvoker
            }
        }

    }

    static abstract class SimpleTrigger implements Trigger {

        private final String id;
        private volatile boolean running;
        protected final ZonedDateTime start;
        protected volatile ZonedDateTime lastFireTime;

        SimpleTrigger(String id, ZonedDateTime start) {
            this.id = id;
            this.start = start;
            this.running = true;
        }

        /**
         * @param now
         * @return the scheduled time if fired, {@code null} otherwise
         */
        abstract ZonedDateTime evaluate(ZonedDateTime now);

        @Override
        public Instant getPreviousFireTime() {
            ZonedDateTime last = lastFireTime;
            return last != null ? lastFireTime.toInstant() : null;
        }

        public String getId() {
            return id;
        }

        synchronized boolean isRunning() {
            return running;
        }

        synchronized void setRunning(boolean running) {
            this.running = running;
        }

    }

    static class IntervalTrigger extends SimpleTrigger {

        // milliseconds
        private final long interval;
        private final Duration gracePeriod;

        IntervalTrigger(String id, ZonedDateTime start, long interval, Duration gracePeriod) {
            super(id, start);
            this.interval = interval;
            this.gracePeriod = gracePeriod;
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
            long diff = ChronoUnit.MILLIS.between(lastFireTime, now);
            if (diff >= interval) {
                ZonedDateTime scheduledFireTime = lastFireTime.plus(Duration.ofMillis(interval));
                lastFireTime = now.truncatedTo(ChronoUnit.SECONDS);
                LOG.tracef("%s fired, diff=%s ms", this, diff);
                return scheduledFireTime;
            }
            return null;
        }

        @Override
        public Instant getNextFireTime() {
            ZonedDateTime last = lastFireTime;
            if (last == null) {
                last = start;
            }
            return last.plus(Duration.ofMillis(interval)).toInstant();
        }

        @Override
        public boolean isOverdue() {
            ZonedDateTime now = ZonedDateTime.now();
            if (now.isBefore(start)) {
                return false;
            }
            return lastFireTime == null || lastFireTime.plus(Duration.ofMillis(interval))
                    .plus(gracePeriod).isBefore(now);
        }

        @Override
        public String toString() {
            StringBuilder builder = new StringBuilder();
            builder.append("IntervalTrigger [id=").append(getId()).append(", interval=").append(interval).append("]");
            return builder.toString();
        }

    }

    static class CronTrigger extends SimpleTrigger {

        private final Cron cron;
        private final ExecutionTime executionTime;
        private final Duration gracePeriod;

        CronTrigger(String id, ZonedDateTime start, Cron cron, Duration gracePeriod) {
            super(id, start);
            this.cron = cron;
            this.executionTime = ExecutionTime.forCron(cron);
            this.lastFireTime = start;
            this.gracePeriod = gracePeriod;
        }

        @Override
        public Instant getNextFireTime() {
            Optional<ZonedDateTime> nextFireTime = executionTime.nextExecution(lastFireTime);
            return nextFireTime.isPresent() ? nextFireTime.get().toInstant() : null;
        }

        ZonedDateTime evaluate(ZonedDateTime now) {
            if (now.isBefore(start)) {
                return null;
            }
            Optional<ZonedDateTime> lastExecution = executionTime.lastExecution(now);
            if (lastExecution.isPresent()) {
                ZonedDateTime lastTruncated = lastExecution.get().truncatedTo(ChronoUnit.SECONDS);
                if (now.isAfter(lastTruncated) && lastFireTime.isBefore(lastTruncated)) {
                    LOG.tracef("%s fired, last=", this, lastTruncated);
                    lastFireTime = now;
                    return lastTruncated;
                }
            }
            return null;
        }

        @Override
        public boolean isOverdue() {
            ZonedDateTime now = ZonedDateTime.now();
            if (now.isBefore(start)) {
                return false;
            }
            Optional<ZonedDateTime> nextFireTime = executionTime.nextExecution(lastFireTime);
            return nextFireTime.isEmpty() || nextFireTime.get().plus(gracePeriod).isBefore(now);
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
