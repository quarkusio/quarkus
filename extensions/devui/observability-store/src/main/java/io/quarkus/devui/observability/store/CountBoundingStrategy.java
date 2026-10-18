package io.quarkus.devui.observability.store;

/**
 * Count-based bound: the buffer may hold at most {@code maxCount} elements.
 */
public final class CountBoundingStrategy implements BoundingStrategy {

    private final int maxCount;

    public CountBoundingStrategy(int maxCount) {
        if (maxCount <= 0) {
            throw new IllegalArgumentException("maxCount must be positive, was " + maxCount);
        }
        this.maxCount = maxCount;
    }

    public int maxCount() {
        return maxCount;
    }

    @Override
    public boolean exceedsBound(int currentSize) {
        return currentSize > maxCount;
    }
}
