package io.quarkus.cli.common;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import io.quarkus.quickcli.annotations.Option;

public class PropertiesOptions {
    public Map<String, String> properties = new HashMap<>();

    @Option(names = "-D", mapFallbackValue = "", description = "Java properties")
    public void setProperty(Map<String, String> props) {
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
