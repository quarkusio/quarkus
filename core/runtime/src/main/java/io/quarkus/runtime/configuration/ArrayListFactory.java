package io.quarkus.runtime.configuration;

import java.util.ArrayList;
import java.util.function.IntFunction;

/**
 * A helper class to produce a right-sized array list.
 */
public final class ArrayListFactory<T> implements IntFunction<ArrayList<T>> {
    private static final ArrayListFactory<?> INSTANCE = new ArrayListFactory<>();

    private ArrayListFactory() {
    }

    public ArrayList<T> apply(final int value) {
        return new ArrayList<>(value);
    }

    @SuppressWarnings("unchecked")
    public static <T> ArrayListFactory<T> getInstance() {
        return (ArrayListFactory<T>) INSTANCE;
    }
}
