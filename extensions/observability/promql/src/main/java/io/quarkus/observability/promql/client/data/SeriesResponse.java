package io.quarkus.observability.promql.client.data;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class SeriesResponse {
    @JsonProperty
    private final Status status;

    @JsonProperty
    private final List<Metric> data;

    @JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
    public SeriesResponse(Status status, List<Metric> data) {
        this.status = status;
        this.data = data;
    }

    public Status status() {
        return status;
    }

    public List<Metric> data() {
        return data;
    }
}