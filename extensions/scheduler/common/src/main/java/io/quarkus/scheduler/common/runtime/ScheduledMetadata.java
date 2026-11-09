package io.quarkus.scheduler.common.runtime;

import java.lang.annotation.Annotation;
import java.util.concurrent.TimeUnit;

import io.quarkus.scheduler.Scheduled;
import io.quarkus.scheduler.Scheduled.ConcurrentExecution;
import io.quarkus.scheduler.Scheduled.SkipPredicate;

/**
 * Build-time values of a {@link Scheduled} annotation. The record components are the members of the annotation, so it
 * implements the annotation directly and can be captured by a service.
 * <p>
 * A record cannot extend {@link jakarta.enterprise.util.AnnotationLiteral}, so {@link #equals(Object)} and
 * {@link #hashCode()} follow the contract of {@link Annotation} instead of the default record semantics. This keeps an
 * instance interchangeable with a real annotation in collections.
 */
public record ScheduledMetadata(String identity, String cron, String every, long delay, TimeUnit delayUnit, String delayed,
        String overdueGracePeriod, ConcurrentExecution concurrentExecution, Class<? extends SkipPredicate> skipExecutionIf,
        String timeZone, String executeWith, String executionMaxDelay, String description) implements Scheduled {

    @Override
    public Class<? extends Annotation> annotationType() {
        return Scheduled.class;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof Scheduled other)) {
            return false;
        }
        return identity.equals(other.identity())
                && cron.equals(other.cron())
                && every.equals(other.every())
                && delay == other.delay()
                && delayUnit == other.delayUnit()
                && delayed.equals(other.delayed())
                && overdueGracePeriod.equals(other.overdueGracePeriod())
                && concurrentExecution == other.concurrentExecution()
                && skipExecutionIf.equals(other.skipExecutionIf())
                && timeZone.equals(other.timeZone())
                && executeWith.equals(other.executeWith())
                && executionMaxDelay.equals(other.executionMaxDelay())
                && description.equals(other.description());
    }

    @Override
    public int hashCode() {
        return member("identity", identity)
                + member("cron", cron)
                + member("every", every)
                + member("delay", Long.hashCode(delay))
                + member("delayUnit", delayUnit)
                + member("delayed", delayed)
                + member("overdueGracePeriod", overdueGracePeriod)
                + member("concurrentExecution", concurrentExecution)
                + member("skipExecutionIf", skipExecutionIf)
                + member("timeZone", timeZone)
                + member("executeWith", executeWith)
                + member("executionMaxDelay", executionMaxDelay)
                + member("description", description);
    }

    private static int member(String name, Object value) {
        return member(name, value.hashCode());
    }

    private static int member(String name, int valueHash) {
        return (127 * name.hashCode()) ^ valueHash;
    }
}
