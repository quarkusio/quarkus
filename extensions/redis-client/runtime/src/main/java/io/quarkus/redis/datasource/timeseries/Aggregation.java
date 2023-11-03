package io.quarkus.redis.datasource.timeseries;

import java.util.Locale;

/**
 * Time Series Aggregation functions
 */
public enum Aggregation {

    /**
     * Arithmetic mean of all values
     */
    AVG,
    /**
     * Sum of all values
     */
    SUM,
    /**
     * Minimum value
     */
    MIN,
    /**
     * Maximum value
     */
    MAX,
    /**
     * Difference between the highest and the lowest value
     */
    RANGE,
    /**
     * Number of values.
     */
    COUNT,
    /**
     * Value with lowest timestamp in the bucket
     */
    FIRST,
    /**
     * Value with highest timestamp in the bucket
     */
    LAST,
    /**
     * Population standard deviation of the values
     */
    STD_P,
    /**
     * Sample standard deviation of the values
     */
    STD_S,
    /**
     * Population variance of the values
     */
    VAR_P,
    /**
     * Sample variance of the values
     */
    VAR_S,
    /**
     * Time-weighted average over the bucket's timeframe (since RedisTimeSeries v1.8)
     */
    TWA;

    @Override
    public String toString() {
        return name().toLowerCase(Locale.ROOT);
    }
}
