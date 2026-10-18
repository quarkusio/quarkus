package io.quarkus.cli.common;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import picocli.CommandLine;

public class PropertiesOptions {
    public Map<String, String> properties = new HashMap<>();

    @CommandLine.Option(names = "-D", mapFallbackValue = "", description = "Java properties")
    void setProperty(Map<String, String> props) {
        this.properties = props;
    }

    public void flattenJvmArgs(List<String> jvmArgs, Collection<String> args) {
        String jvmArgProperty = properties.remove("jvm.args");
        if (jvmArgProperty != null && !jvmArgProperty.isBlank()) {
            jvmArgs.add(jvmArgProperty);
        }

        if (!jvmArgs.isEmpty()) {
            args.add("-Djvm.args='" + String.join(" ", jvmArgs) + "'");
        }
    }

    @Override
    public String toString() {
        return properties.toString();
    }
}
