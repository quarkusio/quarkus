package io.quarkus.observability.promql.client.data;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class ScalarData implements Data<ScalarResult> {
    @JsonProperty
    private final ScalarResult result;

    @JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
    public ScalarData(ScalarResult result) {
        this.result = result;
    }

    @Override
    public ScalarResult result() {
        return result;
    }
}