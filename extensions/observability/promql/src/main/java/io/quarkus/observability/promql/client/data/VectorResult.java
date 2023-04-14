package io.quarkus.observability.promql.client.data;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class VectorResult {

    @JsonProperty
    private final Metric metric;
    @JsonProperty
    private final ScalarResult value;

    @JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
    public VectorResult(
            Metric metric,
            ScalarResult value) {
        this.metric = metric;
        this.value = value;
    }

    public Metric metric() {
        return metric;
    }

    public ScalarResult value() {
        return value;
    }
}
