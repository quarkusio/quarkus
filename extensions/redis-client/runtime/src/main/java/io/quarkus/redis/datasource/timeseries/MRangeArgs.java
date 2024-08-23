package io.quarkus.redis.datasource.timeseries;

import static io.smallrye.mutiny.helpers.ParameterValidation.doesNotContainNull;
import static io.smallrye.mutiny.helpers.ParameterValidation.nonNull;
import static io.smallrye.mutiny.helpers.ParameterValidation.positive;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import io.quarkus.redis.datasource.RedisCommandExtraArguments;

/**
 * Represent the {@code TS.MRANGE} and {@code TS.MREVRANGE} commands optional parameters.
 */
public class MRangeArgs implements RedisCommandExtraArguments {

    private boolean latest;
    private List<Long> filterByTimestamps;
    private boolean filterByValue;
    private double min;
    private double max;
    private boolean withLabels;
    private String[] selectedLabels;
    private int count = -1;
    private String align;
    private Aggregation aggregation;
    private long bucketDuration;
    private BucketTimestamp bucketTimestamp;
    private boolean empty;
    private String groupByLabel;
    private Reducer reducer;

    /**
     * Used when a time series is a compaction. With LATEST, TS.MRANGE also reports the compacted value of the latest
     * possibly partial bucket, given that this bucket's start time falls within [fromTimestamp, toTimestamp].
     * Without LATEST, TS.MRANGE does not report the latest possibly partial bucket. When a time series is not a
     * compaction, LATEST is ignored.
     *
     * @return the current {@code MRangeArgs}
     */
    public MRangeArgs latest() {
        this.latest = true;
        return this;
    }

    /**
     * Filters samples by a list of specific timestamps.
     * A sample passes the filter if its exact timestamp is specified and falls within [fromTimestamp, toTimestamp].
     *
     * @param timestamps the timestamps
     * @return the current {@code MRangeArgs}
     */
    public MRangeArgs filterByTimestamp(long... timestamps) {
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
     * @param min the min value of the sample
     * @param max the max value of the sample
     * @return the current {@code MRangeArgs}
     */
    public MRangeArgs filterByValue(double min, double max) {
        this.filterByValue = true;
        this.min = min;
        this.max = max;

        return this;
    }

    /**
     * Includes in the reply all label-value pairs representing metadata labels of the time series.
     * If WITHLABELS or SELECTED_LABELS are not specified, by default, an empty list is reported as label-value pairs.
     *
     * @return the current {@code MRangeArgs}
     */
    public MRangeArgs withLabels() {
        this.withLabels = true;
        return this;
    }

    /**
     * Returns a subset of the label-value pairs that represent metadata labels of the time series.
     * Use when a large number of labels exists per series, but only the values of some of the labels are required.
     * <p>
     * If WITHLABELS or SELECTED_LABELS are not specified, by default, an empty list is reported as label-value pairs.
     *
     * @param labels the set of labels to select
     * @return the current {@code MRangeArgs}
     */
    public MRangeArgs selectedLabels(String... labels) {
        doesNotContainNull(labels, "labels");
        this.selectedLabels = labels;
        return this;
    }

    /**
     * Limits the number of returned samples.
     *
     * @param count the max number of samples to return
     * @return the current {@code MRangeArgs}
     */
    public MRangeArgs count(int count) {
        this.count = count;
        return this;
    }

    /**
     * Set the time bucket alignment control for AGGREGATION.
     * It controls the time bucket timestamps by changing the reference timestamp on which a bucket is defined.
     *
     * @param timestamp the timestamp
     * @return the current {@code MRangeArgs}
     */
    public MRangeArgs align(long timestamp) {
        this.align = Long.toString(timestamp);
        return this;
    }

    /**
     * Set the time bucket alignment control for AGGREGATION to the query start interval time (fromTimestamp).
     * It controls the time bucket timestamps by changing the reference timestamp on which a bucket is defined.
     *
     * @return the current {@code MRangeArgs}
     */
    public MRangeArgs alignUsingRangeStart() {
        this.align = "-";
        return this;
    }

    /**
     * Set the time bucket alignment control for AGGREGATION to the query end interval time (toTimestamp).
     * It controls the time bucket timestamps by changing the reference timestamp on which a bucket is defined.
     *
     * @return the current {@code MRangeArgs}
     */
    public MRangeArgs alignUsingRangeEnd() {
        this.align = "+";
        return this;
    }

    /**
     * Aggregates results into time buckets.
     *
     * @param aggregation the aggregation function, must not be {@code null}
     * @param bucketDuration the duration of each bucket, must not be {@code null}
     * @return the current {@code MRangeArgs}
     */
    public MRangeArgs aggregation(Aggregation aggregation, Duration bucketDuration) {
        this.aggregation = nonNull(aggregation, "aggregation");
        this.bucketDuration = positive(nonNull(bucketDuration, "bucketDuration").toMillis(), "bucketDuration");
        return this;
    }

    /**
     * Controls how bucket timestamps are reported.
     *
     * @param ts the bucket timestamp configuration, must not be {@code null}
     * @return the current {@code MRangeArgs}
     */
    public MRangeArgs bucketTimestamp(BucketTimestamp ts) {
        this.bucketTimestamp = ts;
        return this;
    }

    /**
     * When specified, reports aggregations also for empty buckets.
     *
     * @return the current {@code MRangeArgs}
     */
    public MRangeArgs empty() {
        this.empty = true;
        return this;
    }

    /**
     * Aggregates results across different time series, grouped by the provided label name.
     * When combined with AGGREGATION the groupby/reduce is applied post aggregation stage.
     * <p>
     * {@code label} is label name to group a series by. A new series for each value is produced.
     * {@code reducer} is the reducer type used to aggregate series that share the same label value.
     *
     * @param label the label, must not be {@code null}
     * @param reducer the reducer function, must not be {@code null}
     * @return the current {@code MRangeArgs}
     */
    public MRangeArgs groupBy(String label, Reducer reducer) {
        this.groupByLabel = nonNull(label, "label");
        this.reducer = nonNull(reducer, "reducer");
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

        if (withLabels) {
            list.add("WITHLABELS");
        }

        if (selectedLabels != null && selectedLabels.length != 0) {
            if (withLabels) {
                throw new IllegalArgumentException("Cannot combine `WITHLABELS` and `SELECTED_LABELS`");
            }

            list.add("SELECTED_LABELS");
            list.addAll(Arrays.asList(selectedLabels));
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
                list.add(bucketTimestamp.toString().toLowerCase());
            }

            if (empty) {
                list.add("EMPTY");
            }
        }
        return list;
    }

    public List<String> getGroupByClauseArgs() {
        if (groupByLabel == null) {
            return List.of();
        } else {
            return List.of("GROUPBY", groupByLabel, "REDUCE", reducer.toString());
        }
    }
}
