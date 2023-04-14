package io.quarkus.observability.promql.client.data;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class VectorData implements Data<List<VectorResult>> {

    @JsonProperty
    private final List<VectorResult> result;

    @JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
    public VectorData(List<VectorResult> result) {
        this.result = result;
    }

    @Override
    public List<VectorResult> result() {
        return result;
    }
}