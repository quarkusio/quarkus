package io.quarkus.quickcli.deployment;

import java.util.HashMap;
import java.util.Map;

import io.quarkus.quickcli.annotations.Command;
import io.quarkus.quickcli.annotations.Option;

@Command(name = "method-opt", description = "Test method-level @Option")
public class MethodOptionCommand implements Runnable {

    public Map<String, String> properties = new HashMap<>();

    @Option(names = "-D", mapFallbackValue = "", description = "Java properties")
    public void setProperty(Map<String, String> props) {
        this.properties = props;
    }

    @Override
    public void run() {
        if (properties.isEmpty()) {
            System.out.println("No properties set");
        } else {
            properties.forEach((k, v) -> System.out.println(k + "=" + v));
        }
    }
}
