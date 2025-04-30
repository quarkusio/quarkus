package io.quarkus.gradle.tasks.services;

import java.util.HashMap;
import java.util.Map;

import org.gradle.api.services.BuildService;
import org.gradle.api.services.BuildServiceParameters;

public abstract class ForcedPropertieBuildService implements BuildService<BuildServiceParameters.None> {

    Map<String, String> properties = new HashMap<>();

    public ForcedPropertieBuildService() {

    }

    public Map<String, String> getProperties() {
        return properties;
    }

}
