package io.quarkus.scheduler.runtime;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.OptionalLong;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
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
import io.quarkus.scheduler.common.runtime.SkipConcurrentExecutionInvoker;
import io.quarkus.scheduler.common.runtime.SkipPredicateInvoker;
import io.quarkus.scheduler.common.runtime.StatusEmitterInvoker;
import io.quarkus.scheduler.common.runtime.SyntheticScheduled;
import io.quarkus.scheduler.common.runtime.util.SchedulerUtils;
import io.quarkus.scheduler.runtime.SchedulerRuntimeConfig.StartMode;
import io.quarkus.vertx.core.runtime.context.VertxContextSafetyToggle;
import io.smallrye.common.vertx.VertxContext;
import io.vertx.core.Context;
import io.vertx.core.Handler;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;

@Typed(Scheduler.class)
@Singleton
public class SimpleScheduler implements Scheduler {

    private static final Logger LOG = Logger.getLogger(SimpleScheduler.class);

    // milliseconds
    private static final long CHECK_PERIOD = 1000L;

    private final ScheduledExecutorService scheduledExecutor;
    private final Vertx vertx;
    private volatile boolean running;
    private final ConcurrentMap<String, ScheduledTask> scheduledTasks;
    private final boolean enabled;
    private final CronParser cronParser;
    private final Duration defaultOverdueGracePeriod;
    private final Event<SkippedExecution> skippedExecutionEvent;
    private final Event<SuccessfulExecution> successExecutionEvent;
    private final Event<FailedExecution> failedExecutionEvent;
    private final Event<SchedulerPaused> schedulerPausedEvent;
    private final Event<SchedulerResumed> schedulerResumedEvent;
    private final Event<ScheduledJobPaused> scheduledJobPausedEvent;
    private final Event<ScheduledJobResumed> scheduledJobResumedEvent;

    public SimpleScheduler(SchedulerContext context, SchedulerRuntimeConfig schedulerRuntimeConfig,
            Event<SkippedExecution> skippedExecutionEvent, Event<SuccessfulExecution> successExecutionEvent,
            Event<FailedExecution> failedExecutionEvent, Event<SchedulerPaused> schedulerPausedEvent,
            Event<SchedulerResumed> schedulerResumedEvent, Event<ScheduledJobPaused> scheduledJobPausedEvent,
            Event<ScheduledJobResumed> scheduledJobResumedEvent, Vertx vertx) {
        this.running = true;
        this.enabled = schedulerRuntimeConfig.enabled;
        this.scheduledTasks = new ConcurrentHashMap<>();
        this.vertx = vertx;
        this.skippedExecutionEvent = skippedExecutionEvent;
        this.successExecutionEvent = successExecutionEvent;
        this.failedExecutionEvent = failedExecutionEvent;
        this.schedulerPausedEvent = schedulerPausedEvent;
        this.schedulerResumedEvent = schedulerResumedEvent;
        this.scheduledJobPausedEvent = scheduledJobPausedEvent;
        this.scheduledJobResumedEvent = scheduledJobResumedEvent;

        CronDefinition definition = CronDefinitionBuilder.instanceDefinitionFor(context.getCronType());
        this.cronParser = new CronParser(definition);
        this.defaultOverdueGracePeriod = schedulerRuntimeConfig.overdueGracePeriod;

        if (!schedulerRuntimeConfig.enabled) {
            this.scheduledExecutor = null;
            LOG.info("Simple scheduler is disabled by config property and will not be started");
            return;
        }

        StartMode startMode = schedulerRuntimeConfig.startMode.orElse(StartMode.NORMAL);
        if (startMode == StartMode.NORMAL && context.getScheduledMethods().isEmpty()) {
            this.scheduledExecutor = null;
            LOG.info("No scheduled business methods found - Simple scheduler will not be started");
            return;
        }

        // This executor is used to check all registered triggers every second
        this.scheduledExecutor = new JBossScheduledThreadPoolExecutor(1, new Runnable() {
            @Override
            public void run() {
                // noop
            }
        });

        if (startMode == StartMode.HALTED) {
            running = false;
        }

        // Create triggers and invokers for @Scheduled methods
        for (ScheduledMethod method : context.getScheduledMethods()) {
            int nameSequence = 0;
            for (Scheduled scheduled : method.getSchedules()) {
                nameSequence++;
                String id = SchedulerUtils.lookUpPropertyValue(scheduled.identity());
                if (id.isEmpty()) {
                    id = nameSequence + "_" + method.getMethodDescription();
                }
                Optional<SimpleTrigger> trigger = createTrigger(id, method.getMethodDescription(), cronParser, scheduled,
                        defaultOverdueGracePeriod);
                if (trigger.isPresent()) {
                    ScheduledInvoker invoker = initInvoker(context.createInvoker(method.getInvokerClassName()),
                            skippedExecutionEvent, successExecutionEvent, failedExecutionEvent,
                            scheduled.concurrentExecution(), initSkipPredicate(scheduled.skipExecutionIf()));
                    scheduledTasks.put(trigger.get().id, new ScheduledTask(trigger.get(), invoker, false));
                }
            }
        }
    }

    @Override
    public JobDefinition newJob(String identity) {
        Objects.requireNonNull(identity);
        if (scheduledTasks.containsKey(identity)) {
            throw new IllegalStateException("A job with this identity is already scheduled: " + identity);
        }
        return new SimpleJobDefinition(identity);
    }

    @Override
    public Trigger unscheduleJob(String identity) {
        Objects.requireNonNull(identity);
        if (!identity.isEmpty()) {
            String parsedIdentity = SchedulerUtils.lookUpPropertyValue(identity);
            ScheduledTask task = scheduledTasks.get(parsedIdentity);
            if (task != null && task.isProgrammatic) {
                if (scheduledTasks.remove(task.trigger.id) != null) {
                    return task.trigger;
                }
            }
        }
        return null;
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
        for (ScheduledTask task : scheduledTasks.values()) {
            task.execute(now, vertx);
        }
    }

    @Override
    public void pause() {
        if (!enabled) {
            LOG.warn("Scheduler is disabled and cannot be paused");
        } else {
            running = false;
            Events.fire(schedulerPausedEvent, SchedulerPaused.INSTANCE);
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
        ScheduledTask task = scheduledTasks.get(parsedIdentity);
        if (task != null) {
            task.trigger.setRunning(false);
            Events.fire(scheduledJobPausedEvent, new ScheduledJobPaused(task.trigger));
        }
    }

    @Override
    public boolean isPaused(String identity) {
        Objects.requireNonNull(identity);
        if (identity.isEmpty()) {
            return false;
        }
        String parsedIdentity = SchedulerUtils.lookUpPropertyValue(identity);
        ScheduledTask task = scheduledTasks.get(parsedIdentity);
        if (task != null) {
            return !task.trigger.isRunning();
        }
        return false;
    }

    @Override
    public void resume() {
        if (!enabled) {
            LOG.warn("Scheduler is disabled and cannot be resumed");
        } else {
            running = true;
            Events.fire(schedulerResumedEvent, SchedulerResumed.INSTANCE);
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
        ScheduledTask task = scheduledTasks.get(parsedIdentity);
        if (task != null) {
            task.trigger.setRunning(true);
            Events.fire(scheduledJobResumedEvent, new ScheduledJobResumed(task.trigger));
        }
    }

    @Override
    public boolean isRunning() {
        return enabled && running;
    }

    @Override
    public List<Trigger> getScheduledJobs() {
        return scheduledTasks.values().stream().map(task -> task.trigger).collect(Collectors.toUnmodifiableList());
    }

    @Override
    public Trigger getScheduledJob(String identity) {
        Objects.requireNonNull(identity);
        if (identity.isEmpty()) {
            return null;
        }
        String parsedIdentity = SchedulerUtils.lookUpPropertyValue(identity);
        ScheduledTask task = scheduledTasks.get(parsedIdentity);
        if (task != null) {
            return task.trigger;
        }
        return null;
    }

    Optional<SimpleTrigger> createTrigger(String id, String methodDescription, CronParser parser, Scheduled scheduled,
            Duration defaultGracePeriod) {
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
                    SchedulerUtils.parseOverdueGracePeriod(scheduled, defaultGracePeriod),
                    SchedulerUtils.parseCronTimeZone(scheduled), methodDescription));
        } else if (!scheduled.every().isEmpty()) {
            final OptionalLong everyMillis = SchedulerUtils.parseEveryAsMillis(scheduled);
            if (!everyMillis.isPresent()) {
                return Optional.empty();
            }
            return Optional.of(new IntervalTrigger(id, start, everyMillis.getAsLong(),
                    SchedulerUtils.parseOverdueGracePeriod(scheduled, defaultGracePeriod), methodDescription));
        } else {
            throw new IllegalArgumentException("Either the 'cron' expression or the 'every' period must be set: " + scheduled);
        }
    }

    public static ScheduledInvoker initInvoker(ScheduledInvoker invoker, Event<SkippedExecution> skippedExecutionEvent,
            Event<SuccessfulExecution> successExecutionEvent,
            Event<FailedExecution> failedExecutionEvent, ConcurrentExecution concurrentExecution,
            Scheduled.SkipPredicate skipPredicate) {
        invoker = new StatusEmitterInvoker(invoker, successExecutionEvent, failedExecutionEvent);
        if (concurrentExecution == ConcurrentExecution.SKIP) {
            invoker = new SkipConcurrentExecutionInvoker(invoker, skippedExecutionEvent);
        }
        if (skipPredicate != null) {
            invoker = new SkipPredicateInvoker(invoker, skipPredicate, skippedExecutionEvent);
        }
        return invoker;
    }

    public static Scheduled.SkipPredicate initSkipPredicate(Class<? extends SkipPredicate> predicateClass) {
        if (predicateClass.equals(Scheduled.Never.class)) {
            return null;
        }
        return Arc.container().select(predicateClass, Any.Literal.INSTANCE).get();
    }

    static class ScheduledTask {

        final boolean isProgrammatic;
        final SimpleTrigger trigger;
        final ScheduledInvoker invoker;

        ScheduledTask(SimpleTrigger trigger, ScheduledInvoker invoker, boolean isProgrammatic) {
            this.trigger = trigger;
            this.invoker = invoker;
            this.isProgrammatic = isProgrammatic;
        }

        void execute(ZonedDateTime now, Vertx vertx) {
            if (!trigger.isRunning()) {
                return;
            }
            ZonedDateTime scheduledFireTime = trigger.evaluate(now);
            if (scheduledFireTime != null) {
                Context context = VertxContext.getOrCreateDuplicatedContext(vertx);
                VertxContextSafetyToggle.setContextSafe(context, true);
                if (invoker.isBlocking()) {
                    context.executeBlocking(new Handler<Promise<Object>>() {
                        @Override
                        public void handle(Promise<Object> p) {
                            try {
                                doInvoke(now, scheduledFireTime);
                            } finally {
                                p.complete();
                            }
                        }
                    }, false);
                } else {
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

        protected final String id;
        protected final String methodDescription;
        private volatile boolean running;
        protected final ZonedDateTime start;
        protected volatile ZonedDateTime lastFireTime;

        SimpleTrigger(String id, ZonedDateTime start, String description) {
            this.id = id;
            this.start = start;
            this.running = true;
            this.methodDescription = description;
        }

        /**
         * @param now The current date-time in the default time zone
         * @return the scheduled time if fired, {@code null} otherwise
         */
        abstract ZonedDateTime evaluate(ZonedDateTime now);

        @Override
        public Instant getPreviousFireTime() {
            ZonedDateTime last = lastFireTime;
            return last != null ? last.toInstant() : null;
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

        public String getMethodDescription() {
            return methodDescription;
        }

    }

    static class IntervalTrigger extends SimpleTrigger {

        // milliseconds
        private final long interval;
        private final Duration gracePeriod;

        IntervalTrigger(String id, ZonedDateTime start, long interval, Duration gracePeriod, String description) {
            super(id, start, description);
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
        private final ZoneId timeZone;

        CronTrigger(String id, ZonedDateTime start, Cron cron, Duration gracePeriod, ZoneId timeZone, String description) {
            super(id, start, description);
            this.cron = cron;
            this.executionTime = ExecutionTime.forCron(cron);
            this.lastFireTime = start;
            this.gracePeriod = gracePeriod;
            this.timeZone = timeZone;
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
            ZonedDateTime zonedNow = timeZone == null ? now : now.withZoneSameInstant(timeZone);
            Optional<ZonedDateTime> lastExecution = executionTime.lastExecution(zonedNow);
            if (lastExecution.isPresent()) {
                ZonedDateTime lastTruncated = lastExecution.get().truncatedTo(ChronoUnit.SECONDS);
                if (zonedNow.isAfter(lastTruncated) && lastFireTime.isBefore(lastTruncated)) {
                    LOG.tracef("%s fired, last=", this, lastTruncated);
                    lastFireTime = zonedNow;
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
            ZonedDateTime zonedNow = timeZone == null ? now : now.withZoneSameInstant(timeZone);
            Optional<ZonedDateTime> nextFireTime = executionTime.nextExecution(lastFireTime);
            return nextFireTime.isEmpty() || nextFireTime.get().plus(gracePeriod).isBefore(zonedNow);
        }

        @Override
        public String toString() {
            return "CronTrigger [id=" + id + ", cron=" + cron.asString() + ", gracePeriod=" + gracePeriod + ", timeZone="
                    + timeZone + "]";
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

    class SimpleJobDefinition extends AbstractJobDefinition {

        SimpleJobDefinition(String id) {
            super(id);
        }

        @Override
        public Trigger schedule() {
            checkScheduled();
            if (task == null && asyncTask == null) {
                throw new IllegalStateException("Either sync or async task must be set");
            }
            scheduled = true;
            ScheduledInvoker invoker;
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
            Scheduled scheduled = new SyntheticScheduled(identity, cron, every, 0, TimeUnit.MINUTES, delayed,
                    overdueGracePeriod, concurrentExecution, skipPredicate, timeZone);
            Optional<SimpleTrigger> trigger = createTrigger(identity, null, cronParser, scheduled,
                    defaultOverdueGracePeriod);
            if (trigger.isPresent()) {
                SimpleTrigger simpleTrigger = trigger.get();
                invoker = initInvoker(invoker, skippedExecutionEvent, successExecutionEvent,
                        failedExecutionEvent, concurrentExecution, skipPredicate);
                ScheduledTask scheduledTask = new ScheduledTask(trigger.get(), invoker, true);
                ScheduledTask existing = scheduledTasks.putIfAbsent(simpleTrigger.id, scheduledTask);
                if (existing != null) {
                    throw new IllegalStateException("A job with this identity is already scheduled: " + identity);
                }
                return simpleTrigger;
            }
            return null;
        }

    }

}
