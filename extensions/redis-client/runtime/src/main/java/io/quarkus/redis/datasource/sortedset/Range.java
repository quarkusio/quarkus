package io.quarkus.redis.datasource.sortedset;

public class Range<V> {

    private final V min;
    private final V max;
    private final boolean inclusiveMin;
    private final boolean inclusiveMax;

    public static final Range<?> UNBOUNDED = new Range<>(null, null);

    @SuppressWarnings("unchecked")
    public static <V> Range<V> unbounded() {
        return (Range<V>) UNBOUNDED;
    }

    public Range(V min, V max) {
        this.min = min;
        this.max = max;
        this.inclusiveMin = true;
        this.inclusiveMax = true;
    }

    public Range(V min, boolean inclusiveMin, V max, boolean inclusiveMax) {
        this.min = min;
        this.max = max;
        this.inclusiveMin = inclusiveMin;
        this.inclusiveMax = inclusiveMax;
    }

    public boolean isUnbounded() {
        return this == UNBOUNDED;
    }

    public String getLowerBound() {
        if (isUnbounded() || min == null || min.equals("-")) {
            return "-";
        }
        if (!inclusiveMin) {
            return "(" + min;
        } else {
            return "[" + min;
        }
    }

    public String getUpperBound() {
        if (isUnbounded() || max == null || max.equals("+")) {
            return "+";
        }
        if (!inclusiveMax) {
            return "(" + max;
        } else {
            return "[" + max;
        }
    }
}
