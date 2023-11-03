package io.quarkus.deployment.builditem;

import java.util.Objects;
import java.util.Set;

import org.jboss.jandex.DotName;
import org.jboss.jandex.Type;

import io.quarkus.builder.item.MultiBuildItem;

public final class ConfigClassBuildItem extends MultiBuildItem {
    private final Class<?> configClass;
    private final Set<Type> types;
    private final Set<String> generatedClasses;
    private final String prefix;
    private final Kind kind;

    private final DotName name;

    public ConfigClassBuildItem(
            final Class<?> configClass,
            final Set<Type> types,
            final Set<String> generatedClasses,
            final String prefix,
            final Kind kind) {

        this.configClass = configClass;
        this.types = types;
        this.generatedClasses = generatedClasses;
        this.prefix = prefix;
        this.kind = kind;
        this.name = DotName.createSimple(configClass.getName());
    }

    public Class<?> getConfigClass() {
        return configClass;
    }

    public Set<Type> getTypes() {
        return types;
    }

    public Set<String> getGeneratedClasses() {
        return generatedClasses;
    }

    public String getPrefix() {
        return prefix;
    }

    public Kind getKind() {
        return kind;
    }

    public DotName getName() {
        return name;
    }

    public boolean isMapping() {
        return Kind.MAPPING.equals(kind);
    }

    public boolean isProperties() {
        return Kind.PROPERTIES.equals(kind);
    }

    public enum Kind {
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
                kind == that.kind;
    }

    @Override
    public int hashCode() {
        return Objects.hash(configClass, prefix, kind);
    }
}
