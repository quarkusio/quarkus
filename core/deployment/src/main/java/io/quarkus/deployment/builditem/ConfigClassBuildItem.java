package io.quarkus.deployment.builditem;

import java.util.Objects;
import java.util.Set;

import org.jboss.jandex.DotName;
import org.jboss.jandex.Type;

import io.quarkus.builder.item.MultiBuildItem;

public final class ConfigClassBuildItem extends MultiBuildItem {
    private final Class<?> configClass;
    /**
     * All the config interfaces registered for this config mapping (including the nested ones)
     */
    private final Set<Class<?>> configComponentInterfaces;
    private final Set<Type> types;
    private final Set<String> generatedClasses;
    private final String prefix;
    private final Kind kind;

    private final DotName name;

    public ConfigClassBuildItem(
            final Class<?> configClass,
            final Set<Class<?>> configComponentInterfaces,
            final Set<Type> types,
            final Set<String> generatedClasses,
            final String prefix,
            final Kind kind) {

        this.configClass = configClass;
        this.configComponentInterfaces = configComponentInterfaces;
        this.types = types;
        this.generatedClasses = generatedClasses;
        this.prefix = prefix;
        this.kind = kind;
        this.name = DotName.createSimple(configClass.getName());
    }

    /**
     * Returns the class of the BuildItem.
     *
     * @return the class of the ConfigClass
     */
    public Class<?> getConfigClass() {
        return configClass;
    }

    /**
     * Returns a Set of interface classes for the ConfigComponent
     *
     * @return a Set of interface classes for the ConfigComponent
     */
    public Set<Class<?>> getConfigComponentInterfaces() {
        return configComponentInterfaces;
    }

    /**
     * Returns a Set of types for the ConfigComponent
     *
     * @return a Set of types for the ConfigComponent
     */
    public Set<Type> getTypes() {
        return types;
    }

    /**
     * Returns a Set of generated classes for the ConfigComponent
     *
     * @return a Set of generated classes for the ConfigComponent
     */
    public Set<String> getGeneratedClasses() {
        return generatedClasses;
    }

    /**
     * Returns the prefix for the ConfigComponent
     *
     * @return the prefix for the ConfigComponent
     */
    public String getPrefix() {
        return prefix;
    }

    /**
     * Returns the {@link Kind} of the ConfigComponent
     *
     * @return a {@link Kind} object for the ConfigComponent
     */
    public Kind getKind() {
        return kind;
    }

    /**
     * Returns the name of the ConfigComponent
     *
     * @return the name of the ConfigComponent
     */
    public DotName getName() {
        return name;
    }

    /**
     * Returns whether the {@link Kind} of the ConfigComponent is a Mapping
     *
     * @return true if the {@link Kind} of the ConfigComponent is a Mapping and false if it is not
     */
    public boolean isMapping() {
        return Kind.MAPPING.equals(kind);
    }

    /**
     * Returns whether the {@link Kind} of the ConfigComponent is a Property
     *
     * @return true if the {@link Kind} of the ConfigComponent is a Property and false if it is not
     */
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
