package io.quarkus.observability.promql.client.data;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonProperty;

public final class Metric {
    private String name;
    private final Map<String, String> labels = new HashMap<>();

    @JsonProperty("__name__")
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @JsonAnyGetter
    public Map<String, String> labels() {
        return labels;
    }

    @JsonAnySetter
    public void setLabel(String name, String value) {
        labels.put(name, value);
    }

    @Override
    public boolean equals(Object o) {
        return this == o ||
                (o instanceof Metric) &&
                        name.equals(((Metric) o).name) &&
                        labels.equals(((Metric) o).labels);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, labels);
    }

    @Override
    public String toString() {
        return labels
                .entrySet()
                .stream()
                .sorted(Map.Entry.comparingByKey())
                .map(e -> e.getKey() + "=\"" + e.getValue() + "\"")
                .collect(Collectors.joining(
                        ",",
                        name == null ? "{" : name + "{",
                        "}"));
    }
}