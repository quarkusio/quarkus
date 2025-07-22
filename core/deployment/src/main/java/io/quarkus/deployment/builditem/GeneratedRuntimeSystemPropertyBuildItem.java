package io.quarkus.deployment.builditem;

import java.util.function.Supplier;

import io.quarkus.builder.item.MultiBuildItem;

/**
 * Represents a system property that will be set immediately on application startup,
 * the system property will be generated when set.
 */
public final class GeneratedRuntimeSystemPropertyBuildItem extends MultiBuildItem {

    private final String key;
    private final String generatorClass;

    public GeneratedRuntimeSystemPropertyBuildItem(String key, Class<? extends Supplier<String>> generatorClass) {
        this.key = key;
        this.generatorClass = generatorClass.getName();
    }

    public String getKey() {
        return key;
    }

    public String getGeneratorClass() {
        return generatorClass;
    }
}
