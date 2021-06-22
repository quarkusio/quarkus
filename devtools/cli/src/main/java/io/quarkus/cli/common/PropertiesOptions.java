package io.quarkus.cli.common;

import java.util.HashMap;
import java.util.Map;

import picocli.CommandLine;

public class PropertiesOptions {
    public Map<String, String> properties = new HashMap<>();

    @CommandLine.Option(order = 5, names = "-D", mapFallbackValue = "", description = "Java properties")
    void setProperty(Map<String, String> props) {
        this.properties = props;
    }

    @Override
    public String toString() {
        return properties.toString();
    }
}
