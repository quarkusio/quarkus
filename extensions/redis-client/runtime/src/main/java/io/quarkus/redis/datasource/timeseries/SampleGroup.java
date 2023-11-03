package io.quarkus.redis.datasource.timeseries;

import java.util.List;
import java.util.Map;

/**
 * Represent a group of samples returned by the range of {@code mget} methods.
 */
public class SampleGroup {

    private final String group;
    private final Map<String, String> labels;
    private final List<Sample> samples;

    public SampleGroup(String group, Map<String, String> labels, List<Sample> samples) {
        this.group = group;
        this.labels = labels;
        this.samples = samples;
    }

    public String group() {
        return group;
    }

    public Map<String, String> labels() {
        return labels;
    }

    public List<Sample> samples() {
        return samples;
    }
}
