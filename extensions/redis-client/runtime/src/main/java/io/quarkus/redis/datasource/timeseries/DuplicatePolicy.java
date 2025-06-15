package io.quarkus.redis.datasource.timeseries;

public enum DuplicatePolicy {
    /**
     * BLOCK: ignore any newly reported value and reply with an error
     */
    BLOCK,
    /**
     * FIRST: ignore any newly reported value
     */
    FIRST,
    /**
     * LAST: override with the newly reported value
     */
    LAST,
    /**
     * MIN: only override if the value is lower than the existing value
     */
    MIN,
    /**
     * MAX: only override if the value is higher than the existing value
     */
    MAX,
    /**
     * SUM: If a previous sample exists, add the new sample to it so that the updated value is equal to (previous +
     * new). If no previous sample exists, set the updated value equal to the new value.
     */
    SUM
}
