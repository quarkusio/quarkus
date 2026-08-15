package io.quarkus.aesh.deployment;

import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class InfoService {

    public String getInfo(String topic) {
        return "Info: " + topic + " (from CDI)";
    }
}
