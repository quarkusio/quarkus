package io.quarkus.redis.datasource.sortedset;

public class ScoreRange<T extends Number> {

    private final Number min;
    private final Number max;
    private final boolean inclusiveMin;
    private final boolean inclusiveMax;

    public static final ScoreRange<?> UNBOUNDED = new ScoreRange<>(null, null);

    public static ScoreRange<Double> from(int min, int max) {
        return new ScoreRange<>((double) min, (double) max);
    }

    public static ScoreRange<Double> from(long min, long max) {
        return new ScoreRange<>((double) min, (double) max);
    }

    public static ScoreRange<Double> from(double min, double max) {
        return new ScoreRange<>(min, max);
    }

    public ScoreRange(T min, T max) {
        this.min = min;
        this.max = max;
        this.inclusiveMin = true;
        this.inclusiveMax = true;
    }

    public ScoreRange(T min, boolean inclusiveMin, T max, boolean inclusiveMax) {
        this.min = min;
        this.max = max;
        this.inclusiveMin = inclusiveMin;
        this.inclusiveMax = inclusiveMax;
    }

    public static ScoreRange<Double> unbounded() {
        return new ScoreRange<>(null, null);
    }

    public boolean isUnbounded() {
        if (this == UNBOUNDED || min == null && max == null) {
            return true;
        }
        return min != null && Double.isInfinite(min.doubleValue()) && max != null
                && Double.isInfinite(max.doubleValue());
    }

    public String getLowerBound() {
        if (isUnbounded() || min == null || Double.isInfinite(min.doubleValue())) {
            return "-inf";
        }
        if (!inclusiveMin) {
            return "(" + min;
        }
        return min.toString();
    }

    public String getUpperBound() {
        if (isUnbounded() || max == null || Double.isInfinite(max.doubleValue())) {
            return "+inf";
        }
        if (!inclusiveMax) {
            return "(" + max;
        }
        return max.toString();
    }
}
