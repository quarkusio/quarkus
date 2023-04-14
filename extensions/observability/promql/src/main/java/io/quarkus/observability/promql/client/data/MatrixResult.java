package io.quarkus.observability.promql.client.data;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class MatrixResult {

    @JsonProperty
    private final Metric metric;
    @JsonProperty
    private final List<ScalarResult> values;

    @JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
    public MatrixResult(
            Metric metric,
            List<ScalarResult> values) {
        this.metric = metric;
        this.values = values;
    }

    public Metric metric() {
        return metric;
    }

    public List<ScalarResult> values() {
        return values;
    }
}
