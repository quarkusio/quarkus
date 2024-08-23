package io.quarkus.redis.datasource.timeseries;

import java.util.Locale;

/**
 * Time Series Reducer functions
 */
public enum Reducer {

    /**
     * per label value: arithmetic mean of all values
     */
    AVG,
    /**
     * per label value: sum of all values
     */
    SUM,
    /**
     * per label value: minimum value
     */
    MIN,
    /**
     * per label value: maximum value
     */
    MAX,
    /**
     * per label value: difference between the highest and the lowest value
     */
    RANGE,
    /**
     * per label value: number of values
     */
    COUNT,
    /**
     * per label value: population standard deviation of the values
     */
    STD_P,
    /**
     * per label value: sample standard deviation of the values
     */
    STD_S,
    /**
     * per label value: population variance of the values
     */
    VAR_P,
    /**
     * per label value: sample variance of the values
     */
    VAR_S;

    @Override
    public String toString() {
        return name().toLowerCase(Locale.ROOT);
    }
}
