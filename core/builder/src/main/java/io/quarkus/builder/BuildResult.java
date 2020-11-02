package io.quarkus.builder;

import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.wildfly.common.Assert;

import io.quarkus.builder.diag.Diagnostic;
import io.quarkus.builder.item.MultiBuildItem;
import io.quarkus.builder.item.SimpleBuildItem;
import io.quarkus.qlue.Success;

/**
 * The final result of a successful deployment operation.
 *
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 */
public final class BuildResult {
    private final Success success;

    BuildResult(final Success success) {
        this.success = success;
    }

    /**
     * Consume the value produced for the named item.
     *
     * @param type the item type (must not be {@code null})
     * @return the produced item (may be {@code null})
     * @throws IllegalArgumentException if this deployer was not declared to consume {@code type}, or if {@code type} is
     *         {@code null}
     * @throws ClassCastException if the cast failed
     */
    public <T extends SimpleBuildItem> T consume(Class<T> type) {
        return type.cast(success.consume(LegacySimpleItem.class, type).getItem());
    }

    /**
     * Consume the value produced for the named item.
     *
     * @param type the item type (must not be {@code null})
     * @return the produced item (may be {@code null})
     * @throws ClassCastException if the cast failed
     */
    public <T extends SimpleBuildItem> T consumeOptional(Class<T> type) {
        LegacySimpleItem item = success.consumeOptional(LegacySimpleItem.class, type);
        return item == null ? null : type.cast(item.getItem());
    }

    /**
     * Consume all of the values produced for the named item.
     *
     * @param type the item element type (must not be {@code null})
     * @return the produced items (may be empty, will not be {@code null})
     * @throws IllegalArgumentException if this deployer was not declared to consume {@code type}
     */
    public <T extends MultiBuildItem> List<T> consumeMulti(Class<T> type) {
        List<LegacyMultiItem> list = success.consumeMulti(LegacyMultiItem.class, type);
        if (list.isEmpty()) {
            return Collections.emptyList();
        }
        int size = list.size();
        List<T> result = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            result.add(i, type.cast(list.get(i).getItem()));
        }
        return result;
    }

    /**
     * Get the diagnostics reported during build.
     *
     * @return the diagnostics reported during build
     */
    @Deprecated
    public List<Diagnostic> getDiagnostics() {
        return Collections.emptyList();
    }

    /**
     * Get the amount of elapsed time from the time the operation was initiated to the time it was completed.
     *
     * @param timeUnit the time unit to return
     * @return the time
     */
    public long getDuration(TimeUnit timeUnit) {
        // todo: timeUnit.toChronoUnit, Java 9+
        Duration duration = success.getDuration();
        switch (timeUnit) {
            case NANOSECONDS:
                return duration.get(ChronoUnit.NANOS);
            case MICROSECONDS:
                return duration.get(ChronoUnit.MICROS);
            case MILLISECONDS:
                return duration.get(ChronoUnit.MILLIS);
            case SECONDS:
                return duration.get(ChronoUnit.SECONDS);
            case MINUTES:
                return duration.get(ChronoUnit.MINUTES);
            case HOURS:
                return duration.get(ChronoUnit.HOURS);
            case DAYS:
                return duration.get(ChronoUnit.DAYS);
            default:
                throw Assert.impossibleSwitchCase(timeUnit);
        }
    }
}
