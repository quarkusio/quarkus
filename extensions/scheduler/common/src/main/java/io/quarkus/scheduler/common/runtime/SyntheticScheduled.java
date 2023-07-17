package io.quarkus.scheduler.common.runtime;

import java.util.Objects;
import java.util.concurrent.TimeUnit;

import jakarta.enterprise.util.AnnotationLiteral;

import io.quarkus.scheduler.Scheduled;

public final class SyntheticScheduled extends AnnotationLiteral<Scheduled> implements Scheduled {

    private static final long serialVersionUID = 1L;

    private final String identity;
    private final String cron;
    private final String every;
    private final long delay;
    private final TimeUnit delayUnit;
    private final String delayed;
    private final String overdueGracePeriod;
    private final ConcurrentExecution concurrentExecution;
    private final SkipPredicate skipPredicate;
    private final String timeZone;

    public SyntheticScheduled(String identity, String cron, String every, long delay, TimeUnit delayUnit, String delayed,
            String overdueGracePeriod, ConcurrentExecution concurrentExecution,
            SkipPredicate skipPredicate, String timeZone) {
        this.identity = Objects.requireNonNull(identity);
        this.cron = Objects.requireNonNull(cron);
        this.every = Objects.requireNonNull(every);
        this.delay = delay;
        this.delayUnit = Objects.requireNonNull(delayUnit);
        this.delayed = Objects.requireNonNull(delayed);
        this.overdueGracePeriod = Objects.requireNonNull(overdueGracePeriod);
        this.concurrentExecution = Objects.requireNonNull(concurrentExecution);
        this.skipPredicate = skipPredicate;
        this.timeZone = timeZone;
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
    public ConcurrentExecution concurrentExecution() {
        return concurrentExecution;
    }

    @Override
    public Class<? extends SkipPredicate> skipExecutionIf() {
        return skipPredicate != null ? skipPredicate.getClass() : Never.class;
    }

    @Override
    public String overdueGracePeriod() {
        return overdueGracePeriod;
    }

    @Override
    public String timeZone() {
        return timeZone;
    }

}
