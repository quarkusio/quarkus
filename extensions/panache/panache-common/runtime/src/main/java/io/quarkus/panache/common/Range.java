package io.quarkus.panache.common;

/**
 * <p>
 * Utility class to represent ranging information. Range instances are immutable.
 * </p>
 *
 * <p>
 * Usage:
 * </p>
 *
 * <code><pre>
 * Range range = Range.of(0, 5);
 * </pre></code>
 *
 * Note that due to the end index being inclusive, it's impossible to write empty ranges.
 */
public class Range {
    // FIXME: bump to long? Jakarta Data uses longs for its Limit type
    private final int startIndex;
    private final int lastIndex;

    /**
     * Creates a new range
     *
     * @param startIndex the start index to include, 0-based
     * @param lastIndex the end index to include, 0-based
     */
    public Range(int startIndex, int lastIndex) {
        if (startIndex < 0)
            throw new IllegalArgumentException("Start index must be >= 0: " + startIndex);
        if (lastIndex < 0)
            throw new IllegalArgumentException("Last index must be >= 0: " + lastIndex);
        if (lastIndex < startIndex)
            throw new IllegalArgumentException(
                    "Last index must be >= start index: " + lastIndex + " is not larger or equal to " + startIndex);
        this.startIndex = startIndex;
        this.lastIndex = lastIndex;
    }

    /**
     * Creates a new range
     *
     * @param startIndex the start index to include, 0-based
     * @param lastIndex the end index to include, 0-based
     */
    public static Range of(int startIndex, int lastIndex) {
        return new Range(startIndex, lastIndex);
    }

    /**
     * @return the start index to include, 0-based
     */
    public int getStartIndex() {
        return startIndex;
    }

    /**
     * @return the last index to include, 0-based
     */
    public int getLastIndex() {
        return lastIndex;
    }
}
