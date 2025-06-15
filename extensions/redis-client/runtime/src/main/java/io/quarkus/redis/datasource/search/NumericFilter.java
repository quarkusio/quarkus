package io.quarkus.redis.datasource.search;

import static io.quarkus.redis.runtime.datasource.Validation.notNullOrBlank;
import static io.smallrye.mutiny.helpers.ParameterValidation.nonNull;

/**
 * Represents a numeric filter
 */
public class NumericFilter {

    private final String field;
    private final Number min;
    private final Number max;
    private final boolean inclusiveMin;
    private final boolean inclusiveMax;

    public static NumericFilter from(String field, int min, int max) {
        return new NumericFilter(field, (double) min, (double) max);
    }

    public static NumericFilter from(String field, long min, long max) {
        return new NumericFilter(field, (double) min, (double) max);
    }

    public static NumericFilter from(String field, double min, double max) {
        return new NumericFilter(field, min, max);
    }

    public NumericFilter(String field, Number min, Number max) {
        this(field, min, true, max, true);
    }

    public NumericFilter(String field, Number min, boolean inclusiveMin, Number max, boolean inclusiveMax) {
        this.field = notNullOrBlank(field, "field");
        this.min = nonNull(min, "min");
        this.max = nonNull(max, "max");
        this.inclusiveMin = inclusiveMin;
        this.inclusiveMax = inclusiveMax;
    }

    public static NumericFilter unbounded(String field) {
        return new NumericFilter(field, null, null);
    }

    public boolean isUnbounded() {
        if (min == null && max == null) {
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

    public String getField() {
        return field;
    }
}
