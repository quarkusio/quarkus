package io.quarkus.extest.runtime.def;

import io.quarkus.runtime.annotations.RecordableConstructor;

public class QuarkusControllerConfiguration implements ControllerConfiguration {

    private final String name;
    private final String resourceTypeName;

    @RecordableConstructor
    public QuarkusControllerConfiguration(String name, String resourceTypeName) {
        this.name = name;
        this.resourceTypeName = resourceTypeName;
    }

    @Override
    public String getName() {
        return name;
    }

    public String getResourceTypeName() {
        return resourceTypeName;
    }
}
