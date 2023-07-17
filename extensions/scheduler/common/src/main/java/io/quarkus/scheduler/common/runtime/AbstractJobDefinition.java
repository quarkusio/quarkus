package io.quarkus.scheduler.common.runtime;

import java.util.function.Consumer;
import java.util.function.Function;

import io.quarkus.scheduler.Scheduled;
import io.quarkus.scheduler.Scheduled.ConcurrentExecution;
import io.quarkus.scheduler.Scheduled.SkipPredicate;
import io.quarkus.scheduler.ScheduledExecution;
import io.quarkus.scheduler.Scheduler.JobDefinition;
import io.smallrye.mutiny.Uni;

public abstract class AbstractJobDefinition implements JobDefinition {

    protected final String identity;
    protected String cron = "";
    protected String every = "";
    protected String delayed = "";
    protected String overdueGracePeriod = "";
    protected ConcurrentExecution concurrentExecution = ConcurrentExecution.PROCEED;
    protected SkipPredicate skipPredicate = null;
    protected Consumer<ScheduledExecution> task;
    protected Function<ScheduledExecution, Uni<Void>> asyncTask;
    protected boolean scheduled = false;
    protected String timeZone = Scheduled.DEFAULT_TIMEZONE;

    public AbstractJobDefinition(String identity) {
        this.identity = identity;
    }

    @Override
    public JobDefinition setCron(String cron) {
        checkScheduled();
        this.cron = cron;
        return this;
    }

    @Override
    public JobDefinition setInterval(String every) {
        checkScheduled();
        this.every = every;
        return this;
    }

    @Override
    public JobDefinition setDelayed(String period) {
        checkScheduled();
        this.delayed = period;
        return this;
    }

    @Override
    public JobDefinition setConcurrentExecution(ConcurrentExecution concurrentExecution) {
        checkScheduled();
        this.concurrentExecution = concurrentExecution;
        return this;
    }

    @Override
    public JobDefinition setSkipPredicate(SkipPredicate skipPredicate) {
        checkScheduled();
        this.skipPredicate = skipPredicate;
        return this;
    }

    @Override
    public JobDefinition setOverdueGracePeriod(String period) {
        checkScheduled();
        this.overdueGracePeriod = period;
        return this;
    }

    @Override
    public JobDefinition setTimeZone(String timeZone) {
        checkScheduled();
        this.timeZone = timeZone;
        return this;
    }

    @Override
    public JobDefinition setTask(Consumer<ScheduledExecution> task) {
        checkScheduled();
        if (asyncTask != null) {
            throw new IllegalStateException("Async task was already set");
        }
        this.task = task;
        return this;
    }

    @Override
    public JobDefinition setAsyncTask(Function<ScheduledExecution, Uni<Void>> asyncTask) {
        checkScheduled();
        if (task != null) {
            throw new IllegalStateException("Sync task was already set");
        }
        this.asyncTask = asyncTask;
        return this;
    }

    protected void checkScheduled() {
        if (scheduled) {
            throw new IllegalStateException("Cannot modify a job that was already scheduled");
        }
    }

}
