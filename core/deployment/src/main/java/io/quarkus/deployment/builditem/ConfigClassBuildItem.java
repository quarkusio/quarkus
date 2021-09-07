package io.quarkus.deployment.builditem;

import java.util.Objects;
import java.util.Set;

import org.jboss.jandex.DotName;

import io.quarkus.builder.item.MultiBuildItem;

public final class ConfigClassBuildItem extends MultiBuildItem {
    private final Class<?> configClass;
    private final Set<String> generatedClasses;
    private final String prefix;
    private final Type type;

    private final DotName name;

    public ConfigClassBuildItem(
            final Class<?> configClass,
            final Set<String> generatedClasses,
            final String prefix,
            final Type type) {

        this.configClass = configClass;
        this.generatedClasses = generatedClasses;
        this.prefix = prefix;
        this.type = type;
        this.name = DotName.createSimple(configClass.getName());
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

    public DotName getName() {
        return name;
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

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        final ConfigClassBuildItem that = (ConfigClassBuildItem) o;
        return configClass.equals(that.configClass) &&
                prefix.equals(that.prefix) &&
                type == that.type;
    }

    @Override
    public int hashCode() {
        return Objects.hash(configClass, prefix, type);
    }
}
