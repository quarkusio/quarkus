package io.quarkus.runtime.configuration;

import java.util.HashSet;
import java.util.function.IntFunction;

/**
 * A helper class to produce a right-sized hash set.
 */
public final class HashSetFactory<T> implements IntFunction<HashSet<T>> {
    private static final HashSetFactory<?> INSTANCE = new HashSetFactory<>();

    private HashSetFactory() {
    }

    public HashSet<T> apply(final int value) {
        return new HashSet<>(getInitialCapacityFromExpectedSize(value));
    }

    /**
     * As the default loadFactor is of 0.75, we need to calculate the initial capacity from the expected size to avoid
     * resizing the collection when we populate the collection with all the initial elements. We use a calculation
     * similar to what is done in {@link java.util.HashMap#putAll(Map)}.
     *
     * @param expectedSize the expected size of the collection
     * @return the initial capacity of the collection
     */
    private int getInitialCapacityFromExpectedSize(int expectedSize) {
        if (expectedSize < 3) {
            return expectedSize + 1;
        }
        return (int) ((float) expectedSize / 0.75f + 1.0f);
    }

    @SuppressWarnings("unchecked")
    public static <T> HashSetFactory<T> getInstance() {
        return (HashSetFactory<T>) INSTANCE;
    }
}
