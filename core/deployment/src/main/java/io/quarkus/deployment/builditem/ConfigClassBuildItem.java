package io.quarkus.deployment.builditem;

import java.util.Set;

import io.quarkus.builder.item.MultiBuildItem;

public final class ConfigClassBuildItem extends MultiBuildItem {
    private final Class<?> configClass;
    private final Set<String> generatedClasses;
    private final String prefix;
    private final Type type;

    public ConfigClassBuildItem(
            final Class<?> configClass,
            final Set<String> generatedClasses,
            final String prefix,
            final Type type) {

        this.configClass = configClass;
        this.generatedClasses = generatedClasses;
        this.prefix = prefix;
        this.type = type;
    }

    public Class<?> getConfigClass() {
        return configClass;
    }

    public Set<String> getGeneratedClasses() {
        return generatedClasses;
    }

    public String getPrefix() {
        return prefix;
    }

    public Type getType() {
        return type;
    }

    public boolean isMapping() {
        return Type.MAPPING.equals(type);
    }

    public boolean isProperties() {
        return Type.PROPERTIES.equals(type);
    }

    public enum Type {
        MAPPING,
        PROPERTIES
    }
}
