package io.quarkus.scheduler.common.runtime;

import java.util.concurrent.TimeUnit;

import io.quarkus.scheduler.Scheduled;
import io.quarkus.scheduler.Scheduled.ConcurrentExecution;
import io.quarkus.scheduler.Scheduled.SkipPredicate;

/**
 * Build-time values of a {@link Scheduled} annotation, turned into a {@link ScheduledLiteral} at runtime.
 */
public record ScheduledMetadata(String identity, String cron, String every, long delay, TimeUnit delayUnit, String delayed,
        String overdueGracePeriod, ConcurrentExecution concurrentExecution, Class<? extends SkipPredicate> skipExecutionIf,
        String timeZone, String executeWith, String executionMaxDelay, String description) {

    public Scheduled toScheduled() {
        return new ScheduledLiteral(identity, cron, every, delay, delayUnit, delayed, overdueGracePeriod,
                concurrentExecution, skipExecutionIf, timeZone, executeWith, executionMaxDelay, description);
    }
}
