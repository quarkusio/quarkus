package io.quarkus.observability.promql.client.data;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class StringData implements Data<StringResult> {
    @JsonProperty
    private final StringResult result;

    @JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
    public StringData(StringResult result) {
        this.result = result;
    }

    @Override
    public StringResult result() {
        return result;
    }
}