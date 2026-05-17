package io.quarkus.cli.common;

import java.util.HashMap;
import java.util.Map;

import io.quarkus.quickcli.annotations.Option;

public class DataOptions {

    public Map<String, String> data = new HashMap<>();

    @Option(names = "--data", mapFallbackValue = "", description = "Additional data")
    public void setData(Map<String, String> data) {
        this.data = data;
    }

    @Override
    public String toString() {
        return data.toString();
    }
}
