package io.quarkus.scheduler.common.runtime;

import java.util.Objects;
import java.util.concurrent.TimeUnit;

import jakarta.enterprise.util.AnnotationLiteral;

import io.quarkus.scheduler.Scheduled;

/**
 * A {@link Scheduled} literal created from build-time metadata of a {@code @Scheduled} business method.
 */
public final class ScheduledLiteral extends AnnotationLiteral<Scheduled> implements Scheduled {

    private static final long serialVersionUID = 1L;

    private final String identity;
    private final String cron;
    private final String every;
    private final long delay;
    private final TimeUnit delayUnit;
    private final String delayed;
    private final String overdueGracePeriod;
    private final ConcurrentExecution concurrentExecution;
    private final Class<? extends SkipPredicate> skipExecutionIf;
    private final String timeZone;
    private final String executeWith;
    private final String executionMaxDelay;
    private final String description;

    public ScheduledLiteral(String identity, String cron, String every, long delay, TimeUnit delayUnit, String delayed,
            String overdueGracePeriod, ConcurrentExecution concurrentExecution, Class<? extends SkipPredicate> skipExecutionIf,
            String timeZone, String executeWith, String executionMaxDelay, String description) {
        this.identity = Objects.requireNonNull(identity);
        this.cron = Objects.requireNonNull(cron);
        this.every = Objects.requireNonNull(every);
        this.delay = delay;
        this.delayUnit = Objects.requireNonNull(delayUnit);
        this.delayed = Objects.requireNonNull(delayed);
        this.overdueGracePeriod = Objects.requireNonNull(overdueGracePeriod);
        this.concurrentExecution = Objects.requireNonNull(concurrentExecution);
        this.skipExecutionIf = Objects.requireNonNull(skipExecutionIf);
        this.timeZone = Objects.requireNonNull(timeZone);
        this.executeWith = Objects.requireNonNull(executeWith);
        this.executionMaxDelay = Objects.requireNonNull(executionMaxDelay);
        this.description = Objects.requireNonNull(description);
    }

    @Override
    public String identity() {
        return identity;
    }

    @Override
    public String cron() {
        return cron;
    }

    @Override
    public String every() {
        return every;
    }

    @Override
    public long delay() {
        return delay;
    }

    @Override
    public TimeUnit delayUnit() {
        return delayUnit;
    }

    @Override
    public String delayed() {
        return delayed;
    }

    @Override
    public String overdueGracePeriod() {
        return overdueGracePeriod;
    }

    @Override
    public ConcurrentExecution concurrentExecution() {
        return concurrentExecution;
    }

    @Override
    public Class<? extends SkipPredicate> skipExecutionIf() {
        return skipExecutionIf;
    }

    @Override
    public String timeZone() {
        return timeZone;
    }

    @Override
    public String executeWith() {
        return executeWith;
    }

    @Override
    public String executionMaxDelay() {
        return executionMaxDelay;
    }

    @Override
    public String description() {
        return description;
    }

}
