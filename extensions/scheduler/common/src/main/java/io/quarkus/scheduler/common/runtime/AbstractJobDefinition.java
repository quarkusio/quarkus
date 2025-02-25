package io.quarkus.scheduler.common.runtime;

import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Function;

import io.quarkus.scheduler.Scheduled;
import io.quarkus.scheduler.Scheduled.ConcurrentExecution;
import io.quarkus.scheduler.Scheduled.SkipPredicate;
import io.quarkus.scheduler.ScheduledExecution;
import io.quarkus.scheduler.Scheduler.JobDefinition;
import io.quarkus.scheduler.common.runtime.util.SchedulerUtils;
import io.smallrye.mutiny.Uni;

public abstract class AbstractJobDefinition<THIS extends JobDefinition<THIS>> implements JobDefinition<THIS> {

    protected final String identity;
    protected String cron = "";
    protected String every = "";
    protected String delayed = "";
    protected String overdueGracePeriod = "";
    protected ConcurrentExecution concurrentExecution = ConcurrentExecution.PROCEED;
    protected SkipPredicate skipPredicate = null;
    protected Class<? extends SkipPredicate> skipPredicateClass;
    protected Consumer<ScheduledExecution> task;
    protected Class<? extends Consumer<ScheduledExecution>> taskClass;
    protected Function<ScheduledExecution, Uni<Void>> asyncTask;
    protected Class<? extends Function<ScheduledExecution, Uni<Void>>> asyncTaskClass;
    protected boolean scheduled = false;
    protected String timeZone = Scheduled.DEFAULT_TIMEZONE;
    protected boolean runOnVirtualThread;
    protected String implementation = Scheduled.AUTO;
    protected String executionMaxDelay = "";

    public AbstractJobDefinition(String identity) {
        this.identity = identity;
    }

    @Override
    public THIS setCron(String cron) {
        checkScheduled();
        this.cron = Objects.requireNonNull(cron);
        return self();
    }

    @Override
    public THIS setInterval(String every) {
        checkScheduled();
        this.every = Objects.requireNonNull(every);
        return self();
    }

    @Override
    public THIS setDelayed(String period) {
        checkScheduled();
        this.delayed = Objects.requireNonNull(period);
        return self();
    }

    @Override
    public THIS setConcurrentExecution(ConcurrentExecution concurrentExecution) {
        checkScheduled();
        this.concurrentExecution = Objects.requireNonNull(concurrentExecution);
        return self();
    }

    @Override
    public THIS setSkipPredicate(SkipPredicate skipPredicate) {
        checkScheduled();
        this.skipPredicate = Objects.requireNonNull(skipPredicate);
        return self();
    }

    @Override
    public THIS setSkipPredicate(Class<? extends SkipPredicate> skipPredicateClass) {
        checkScheduled();
        this.skipPredicateClass = Objects.requireNonNull(skipPredicateClass);
        return setSkipPredicate(SchedulerUtils.instantiateBeanOrClass(skipPredicateClass));
    }

    @Override
    public THIS setOverdueGracePeriod(String period) {
        checkScheduled();
        this.overdueGracePeriod = Objects.requireNonNull(period);
        return self();
    }

    @Override
    public THIS setTimeZone(String timeZone) {
        checkScheduled();
        this.timeZone = Objects.requireNonNull(timeZone);
        return self();
    }

    @Override
    public THIS setExecuteWith(String implementation) {
        checkScheduled();
        this.implementation = Objects.requireNonNull(implementation);
        return self();
    }

    @Override
    public THIS setExecutionMaxDelay(String maxDelay) {
        checkScheduled();
        this.executionMaxDelay = maxDelay;
        return self();
    }

    @Override
    public THIS setTask(Consumer<ScheduledExecution> task, boolean runOnVirtualThread) {
        checkScheduled();
        if (asyncTask != null) {
            throw new IllegalStateException("Async task was already set");
        }
        this.task = Objects.requireNonNull(task);
        this.runOnVirtualThread = runOnVirtualThread;
        return self();
    }

    @Override
    public THIS setTask(Class<? extends Consumer<ScheduledExecution>> taskClass, boolean runOnVirtualThread) {
        this.taskClass = Objects.requireNonNull(taskClass);
        return setTask(SchedulerUtils.instantiateBeanOrClass(taskClass), runOnVirtualThread);
    }

    @Override
    public THIS setAsyncTask(Function<ScheduledExecution, Uni<Void>> asyncTask) {
        checkScheduled();
        if (task != null) {
            throw new IllegalStateException("Sync task was already set");
        }
        this.asyncTask = Objects.requireNonNull(asyncTask);
        return self();
    }

    @Override
    public THIS setAsyncTask(Class<? extends Function<ScheduledExecution, Uni<Void>>> asyncTaskClass) {
        this.asyncTaskClass = Objects.requireNonNull(asyncTaskClass);
        return setAsyncTask(SchedulerUtils.instantiateBeanOrClass(asyncTaskClass));
    }

    protected void checkScheduled() {
        if (scheduled) {
            throw new IllegalStateException("Cannot modify a job that was already scheduled");
        }
    }

    @SuppressWarnings("unchecked")
    protected THIS self() {
        return (THIS) this;
    }

}
