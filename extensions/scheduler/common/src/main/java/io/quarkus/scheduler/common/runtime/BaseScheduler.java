package io.quarkus.scheduler.common.runtime;

import java.time.Duration;
import java.util.OptionalLong;
import java.util.concurrent.ScheduledExecutorService;

import jakarta.enterprise.inject.Instance;

import io.quarkus.scheduler.Scheduled;
import io.quarkus.scheduler.Scheduled.ConcurrentExecution;
import io.quarkus.scheduler.Scheduled.SkipPredicate;
import io.quarkus.scheduler.common.runtime.util.SchedulerUtils;
import io.quarkus.scheduler.spi.JobInstrumenter;
import io.vertx.core.Vertx;

public class BaseScheduler {

    protected final Vertx vertx;
    protected final CronParser cronParser;
    protected final Duration defaultOverdueGracePeriod;
    protected final Events events;
    protected final Instance<JobInstrumenter> jobInstrumenter;
    protected final ScheduledExecutorService blockingExecutor;

    public BaseScheduler(Vertx vertx, CronParser cronParser,
            Duration defaultOverdueGracePeriod, Events events, Instance<JobInstrumenter> jobInstrumenter,
            ScheduledExecutorService blockingExecutor) {
        this.vertx = vertx;
        this.cronParser = cronParser;
        this.defaultOverdueGracePeriod = defaultOverdueGracePeriod;
        this.events = events;
        this.jobInstrumenter = jobInstrumenter;
        this.blockingExecutor = blockingExecutor;
    }

    protected UnsupportedOperationException notStarted() {
        return new UnsupportedOperationException("Scheduler was not started");
    }

    protected ScheduledInvoker initInvoker(ScheduledInvoker invoker, Events events,
            ConcurrentExecution concurrentExecution, Scheduled.SkipPredicate skipPredicate, JobInstrumenter instrumenter,
            Vertx vertx, boolean skipOffloadingInvoker,
            OptionalLong delay, ScheduledExecutorService blockingExecutor) {
        invoker = new StatusEmitterInvoker(invoker, events.successExecution, events.failedExecution);
        if (concurrentExecution == ConcurrentExecution.SKIP) {
            invoker = new SkipConcurrentExecutionInvoker(invoker, events.skippedExecution);
        }
        if (skipPredicate != null) {
            invoker = new SkipPredicateInvoker(invoker, skipPredicate, events.skippedExecution);
        }
        if (instrumenter != null) {
            invoker = new InstrumentedInvoker(invoker, instrumenter);
        }
        if (!skipOffloadingInvoker) {
            invoker = new OffloadingInvoker(invoker, vertx);
        }
        if (delay.isPresent()) {
            invoker = new DelayedExecutionInvoker(invoker, delay.getAsLong(), blockingExecutor, events.delayedExecution);
        }
        return invoker;
    }

    protected Scheduled.SkipPredicate initSkipPredicate(Class<? extends SkipPredicate> predicateClass) {
        if (predicateClass.equals(Scheduled.Never.class)) {
            return null;
        }
        return SchedulerUtils.instantiateBeanOrClass(predicateClass);
    }

}
