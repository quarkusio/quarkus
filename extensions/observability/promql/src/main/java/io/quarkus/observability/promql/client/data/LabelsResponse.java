package io.quarkus.observability.promql.client.data;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class LabelsResponse {

    @JsonProperty
    private final Status status;

    @JsonProperty
    private final List<String> data;

    @JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
    public LabelsResponse(Status status, List<String> data) {
        this.status = status;
        this.data = data;
    }

    public Status status() {
        return status;
    }

    public List<String> data() {
        return data;
    }
}
