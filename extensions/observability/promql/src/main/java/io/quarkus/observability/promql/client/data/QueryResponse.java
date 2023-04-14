package io.quarkus.observability.promql.client.data;

import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class QueryResponse {
    @JsonProperty
    private final Status status;
    @JsonProperty
    private final Data<?> data;
    @JsonProperty
    private final String errorType;
    @JsonProperty
    private final String error;
    @JsonProperty
    private final List<String> warnings;
    @JsonProperty
    private final Map<String, String> stats;

    @JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
    public QueryResponse(Status status, Data<?> data, String errorType, String error, List<String> warnings,
            Map<String, String> stats) {
        this.status = status;
        this.data = data;
        this.errorType = errorType;
        this.error = error;
        this.warnings = warnings;
        this.stats = stats;
    }

    public Status status() {
        return status;
    }

    public Data<?> data() {
        return data;
    }

    public String errorType() {
        return errorType;
    }

    public String error() {
        return error;
    }

    public List<String> warnings() {
        return warnings;
    }

    public Map<String, String> stats() {
        return stats;
    }
}