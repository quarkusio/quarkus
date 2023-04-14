package io.quarkus.observability.promql.client.data;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class MatrixData implements Data<List<MatrixResult>> {

    @JsonProperty
    private final List<MatrixResult> result;

    @JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
    public MatrixData(List<MatrixResult> result) {
        this.result = result;
    }

    @Override
    public List<MatrixResult> result() {
        return result;
    }
}