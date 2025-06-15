package io.quarkus.redis.datasource.timeseries;

import static io.smallrye.mutiny.helpers.ParameterValidation.positiveOrZero;

import java.util.List;

/**
 * Represent the range used in the {@code TS.MRANGE} and {@code TS.MREVRANGE} commands.
 */
public class TimeSeriesRange {

    private final long start;
    private final long end;

    private TimeSeriesRange(long s, long e) {
        this.start = positiveOrZero(s, "start");
        this.end = positiveOrZero(e, "end");
    }

    /**
     * Creates a time series range using the given timestamps
     *
     * @param begin
     *        the beginning of the range
     * @param end
     *        the end of the range
     *
     * @return the time series range
     */
    public static TimeSeriesRange fromTimestamps(long begin, long end) {
        return new TimeSeriesRange(begin, end);
    }

    /**
     * Creates a time series range using the earliest sample and latest sample of the time series as bound
     *
     * @return the time series range
     */
    public static TimeSeriesRange fromTimeSeries() {
        return new TimeSeriesRange(Long.MAX_VALUE, Long.MAX_VALUE);
    }

    /**
     * Creates a time series range going from the earliest sample of the series to the given timestamp
     *
     * @return the time series range
     */
    public static TimeSeriesRange fromEarliestToTimestamp(long end) {
        return new TimeSeriesRange(Long.MAX_VALUE, end);
    }

    /**
     * Creates a time series range going from the given timestamp to the latest sample of the series
     *
     * @return the time series range
     */
    public static TimeSeriesRange fromTimestampToLatest(long begin) {
        return new TimeSeriesRange(begin, Long.MAX_VALUE);
    }

    public List<String> toArgs() {
        return List.of(start == Long.MAX_VALUE ? "-" : Long.toString(start),
                end == Long.MAX_VALUE ? "+" : Long.toString(end));
    }

}
