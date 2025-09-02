package io.quarkus.deployment.builditem;

import java.util.Objects;

import io.quarkus.builder.item.MultiBuildItem;
import io.quarkus.runtime.annotations.StaticInitSafe;
import io.smallrye.config.ConfigMapping;
import io.smallrye.config.ConfigMappings.ConfigClass;

/**
 * Represents a build item that registers a configuration mapping interface in the Quarkus build process.
 * It associates a configuration class with the respective configuration prefix, enabling Quarkus to map configuration
 * properties to strongly-typed interfaces.
 */
public final class ConfigMappingBuildItem extends MultiBuildItem {

    /**
     * The configuration class to be registered.
     */
    private final Class<?> configClass;

    /**
     * The configuration prefix associated with the configuration class.
     * This corresponds to the value of {@link ConfigMapping#prefix()}.
     */
    private final String prefix;

    /**
     * Constructs a new ConfigMappingBuildItem.
     *
     * @param configClass the configuration class to be registered
     * @param prefix the configuration prefix associated with the configuration class
     */
    public ConfigMappingBuildItem(final Class<?> configClass, final String prefix) {
        this.configClass = configClass;
        this.prefix = prefix;
    }

    public Class<?> getConfigClass() {
        return configClass;
    }

    public String getPrefix() {
        return prefix;
    }

    /**
     * Checks if the configuration class is safe to use during static initialization.
     *
     * @return true if the configuration class is annotated with {@link StaticInitSafe}, false otherwise
     */
    public boolean isStaticInitSafe() {
        return configClass.isAnnotationPresent(StaticInitSafe.class);
    }

    public ConfigClass toConfigClass() {
        return ConfigClass.configClass(configClass, prefix);
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        final ConfigMappingBuildItem that = (ConfigMappingBuildItem) o;
        return configClass.equals(that.configClass) &&
                prefix.equals(that.prefix);
    }

    @Override
    public int hashCode() {
        return Objects.hash(configClass, prefix);
    }
}
