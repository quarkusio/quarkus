package io.quarkus.redis.datasource.timeseries;

import static io.smallrye.mutiny.helpers.ParameterValidation.nonNull;
import static io.smallrye.mutiny.helpers.ParameterValidation.positive;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

import io.quarkus.redis.datasource.RedisCommandExtraArguments;

/**
 * Represent the {@code TS.RANGE} and {@code TS.REVRANGE} commands optional parameters.
 */
public class RangeArgs implements RedisCommandExtraArguments {

    private boolean latest;
    private List<Long> filterByTimestamps;
    private boolean filterByValue;
    private double min;
    private double max;
    private int count = -1;
    private String align;
    private Aggregation aggregation;
    private long bucketDuration;
    private BucketTimestamp bucketTimestamp;
    private boolean empty;

    /**
     * Used when a time series is a compaction. With LATEST, TS.MRANGE also reports the compacted value of the latest
     * possibly partial bucket, given that this bucket's start time falls within [fromTimestamp, toTimestamp]. Without
     * LATEST, TS.MRANGE does not report the latest possibly partial bucket. When a time series is not a compaction,
     * LATEST is ignored.
     *
     * @return the current {@code MRangeArgs}
     */
    public RangeArgs latest() {
        this.latest = true;
        return this;
    }

    /**
     * Filters samples by a list of specific timestamps. A sample passes the filter if its exact timestamp is specified
     * and falls within [fromTimestamp, toTimestamp].
     *
     * @param timestamps
     *        the timestamps
     *
     * @return the current {@code MRangeArgs}
     */
    public RangeArgs filterByTimestamp(long... timestamps) {
        if (filterByTimestamps == null) {
            filterByTimestamps = new ArrayList<>(timestamps.length);
        }
        for (long timestamp : timestamps) {
            if (timestamp < 0) {
                throw new IllegalArgumentException("The timestamp must be positive");
            }
            filterByTimestamps.add(timestamp);
        }
        return this;
    }

    /**
     * Filters samples by minimum and maximum values.
     *
     * @param min
     *        the min value of the sample
     * @param max
     *        the max value of the sample
     *
     * @return the current {@code MRangeArgs}
     */
    public RangeArgs filterByValue(double min, double max) {
        this.filterByValue = true;
        this.min = min;
        this.max = max;

        return this;
    }

    /**
     * Limits the number of returned samples.
     *
     * @param count
     *        the max number of samples to return
     *
     * @return the current {@code MRangeArgs}
     */
    public RangeArgs count(int count) {
        this.count = count;
        return this;
    }

    /**
     * Set the time bucket alignment control for AGGREGATION. It controls the time bucket timestamps by changing the
     * reference timestamp on which a bucket is defined.
     *
     * @param timestamp
     *        the timestamp
     *
     * @return the current {@code MRangeArgs}
     */
    public RangeArgs align(long timestamp) {
        this.align = Long.toString(timestamp);
        return this;
    }

    /**
     * Set the time bucket alignment control for AGGREGATION to the query start interval time (fromTimestamp). It
     * controls the time bucket timestamps by changing the reference timestamp on which a bucket is defined.
     *
     * @return the current {@code MRangeArgs}
     */
    public RangeArgs alignUsingRangeStart() {
        this.align = "-";
        return this;
    }

    /**
     * Set the time bucket alignment control for AGGREGATION to the query end interval time (toTimestamp). It controls
     * the time bucket timestamps by changing the reference timestamp on which a bucket is defined.
     *
     * @return the current {@code MRangeArgs}
     */
    public RangeArgs alignUsingRangeEnd() {
        this.align = "+";
        return this;
    }

    /**
     * Aggregates results into time buckets.
     *
     * @param aggregation
     *        the aggregation function, must not be {@code null}
     * @param bucketDuration
     *        the duration of each bucket, must not be {@code null}
     *
     * @return the current {@code MRangeArgs}
     */
    public RangeArgs aggregation(Aggregation aggregation, Duration bucketDuration) {
        this.aggregation = nonNull(aggregation, "aggregation");
        this.bucketDuration = positive(nonNull(bucketDuration, "bucketDuration").toMillis(), "bucketDuration");
        return this;
    }

    /**
     * Controls how bucket timestamps are reported.
     *
     * @param ts
     *        the bucket timestamp configuration, must not be {@code null}
     *
     * @return the current {@code MRangeArgs}
     */
    public RangeArgs bucketTimestamp(BucketTimestamp ts) {
        this.bucketTimestamp = ts;
        return this;
    }

    /**
     * When specified, reports aggregations also for empty buckets.
     *
     * @return the current {@code MRangeArgs}
     */
    public RangeArgs empty() {
        this.empty = true;
        return this;
    }

    @Override
    public List<Object> toArgs() {
        List<Object> list = new ArrayList<>();

        if (latest) {
            list.add("LATEST");
        }

        if (filterByTimestamps != null && !filterByTimestamps.isEmpty()) {
            list.add("FILTER_BY_TS");
            for (Long ts : filterByTimestamps) {
                list.add(Long.toString(ts));
            }
        }

        if (filterByValue) {
            list.add("FILTER_BY_VALUE");
            list.add(Double.toString(min)); // TODO Check format
            list.add(Double.toString(max));
        }

        if (count != -1) {
            list.add("COUNT");
            list.add(Integer.toString(count));
        }

        if (aggregation != null) {
            if (align != null) {
                list.add("ALIGN");
                list.add(align);
            }

            list.add("AGGREGATION");
            list.add(aggregation.toString());
            list.add(Long.toString(bucketDuration));

            if (bucketTimestamp != null) {
                list.add("BUCKETTIMESTAMP");
                list.add(bucketTimestamp.toString());
            }

            if (empty) {
                list.add("EMPTY");
            }
        }
        return list;
    }
}
