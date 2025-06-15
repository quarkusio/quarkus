package io.quarkus.redis.datasource.timeseries;

import java.util.Locale;

/**
 * Configure the bucket timestamp of an aggregation in the {@code TS.MRANGE} command. It controls how bucket timestamps
 * are reported.
 */
public enum BucketTimestamp {

    /**
     * the bucket's start time (default).
     */
    LOW,
    /**
     * the bucket's end time.
     */
    HIGH,
    /**
     * the bucket's mid time (rounded down if not an integer)
     */
    MID;

    @Override
    public String toString() {
        return name().toLowerCase(Locale.ROOT);
    }
}
