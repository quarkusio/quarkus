package io.quarkus.kubernetes.spi;

import java.util.Collection;
import java.util.stream.Stream;

public interface Targetable {
    String getTarget();

    default boolean isActiveFor(String target, boolean strictTargetMatching) {
        if (target == null) {
            return true;
        }
        final var localTarget = getTarget();
        if (strictTargetMatching) {
            return target.equals(localTarget);
        } else {
            return localTarget == null || localTarget.equals(target);
        }
    }

    static <T extends Targetable> Stream<T> filteredByTarget(Collection<T> targetables, String target) {
        return filteredByTarget(targetables, target, false);
    }

    static <T extends Targetable> Stream<T> filteredByTarget(Collection<T> targetables, String target,
            boolean strictTargetMatching) {
        return targetables.stream().filter(targetable -> targetable.isActiveFor(target, strictTargetMatching));
    }
}
