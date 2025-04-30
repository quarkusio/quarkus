package io.quarkus.redis.datasource.timeseries;

import static io.smallrye.mutiny.helpers.ParameterValidation.nonNull;

import java.util.ArrayList;
import java.util.List;

import io.quarkus.redis.datasource.RedisCommandExtraArguments;

/**
 * Represents the extra arguments of the {@code ts.mget} command.
 */
public class MGetArgs implements RedisCommandExtraArguments {

    private boolean latest;

    private boolean withLabels;

    private final List<String> selectedLabels = new ArrayList<>();

    /**
     * With LATEST, TS.MGET also reports the compacted value of the latest possibly partial bucket, given that this
     * bucket's start time falls within [fromTimestamp, toTimestamp]. Without LATEST, TS.MGET does not report the
     * latest possibly partial bucket. When a time series is not a compaction, LATEST is ignored.
     *
     * @return the current {@code MGetArgs}
     */
    public MGetArgs latest() {
        this.latest = true;
        return this;
    }

    /**
     * includes in the reply all label-value pairs representing metadata labels of the time series.
     * If WITHLABELS or SELECTED_LABELS are not specified, by default, an empty list is reported as label-value pairs.
     *
     * @return the current {@code MGetArgs}
     */
    public MGetArgs withLabels() {
        this.withLabels = true;
        return this;
    }

    /**
     * Add a label to the list of selected label.
     * Returns a subset of the label-value pairs that represent metadata labels of the time series.
     * Use when a large number of labels exists per series, but only the values of some of the labels are required.
     * If WITHLABELS or SELECTED_LABELS are not specified, by default, an empty list is reported as label-value pairs.
     *
     * @param label the label to select
     * @return the current {@code MGetArgs}
     */
    public MGetArgs selectedLabel(String label) {
        selectedLabels.add(nonNull(label, "label"));
        return this;
    }

    @Override
    public List<Object> toArgs() {
        List<Object> list = new ArrayList<>();
        if (latest) {
            list.add("LATEST");
        }
        if (withLabels) {
            list.add("WITHLABELS");
        }
        if (!selectedLabels.isEmpty()) {
            list.add("SELECTED_LABELS");
            list.addAll(selectedLabels);
        }
        if (withLabels && !selectedLabels.isEmpty()) {
            throw new IllegalArgumentException("Cannot use `WITHLABELS` and `SELECTED_LABELS together");
        }

        return list;
    }
}
