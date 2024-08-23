package io.quarkus.cli.common;

import java.util.HashMap;
import java.util.Map;

import picocli.CommandLine;

public class DataOptions {

    public Map<String, String> data = new HashMap<>();

    @CommandLine.Option(names = "--data", mapFallbackValue = "", description = "Additional data")
    void setData(Map<String, String> data) {
        this.data = data;
    }

    @Override
    public String toString() {
        return data.toString();
    }
}
