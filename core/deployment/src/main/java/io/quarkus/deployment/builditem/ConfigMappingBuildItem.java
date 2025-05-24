package io.quarkus.deployment.builditem;

import java.util.Objects;

import io.quarkus.builder.item.MultiBuildItem;
import io.quarkus.runtime.annotations.StaticInitSafe;
import io.smallrye.config.ConfigMappings.ConfigClass;

/**
 * A build item that registers a configuration mapping class with a specific prefix.
 * <p>
 * Configuration mapping classes are interfaces or abstract classes annotated with
 * {@code @ConfigMapping} that define strongly-typed views of configuration properties.
 * </p>
 * <p>
 * This build item is consumed during the build to generate code for accessing configuration values
 * and to validate that the specified configuration is correctly mapped.
 * </p>
 */
public final class ConfigMappingBuildItem extends MultiBuildItem {
    private final Class<?> configClass;
    private final String prefix;

    /**
     * Constructs a new {@code ConfigMappingBuildItem}.
     *
     * @param configClass the configuration mapping class (typically annotated with {@code @ConfigMapping})
     * @param prefix the configuration property prefix to bind this class to
     */
    public ConfigMappingBuildItem(final Class<?> configClass, final String prefix) {
        this.configClass = configClass;
        this.prefix = prefix;
    }

    /**
     * Returns the configuration mapping class.
     *
     * @return the class that defines the configuration structure
     */
    public Class<?> getConfigClass() {
        return configClass;
    }

    /**
     * Returns the configuration prefix associated with this mapping.
     *
     * @return the configuration prefix
     */
    public String getPrefix() {
        return prefix;
    }

    /**
     * Indicates whether the configuration class is safe to be initialized at static init time.
     * <p>
     * This is determined by checking if the class is annotated with {@link StaticInitSafe}.
     * </p>
     *
     * @return {@code true} if the class is static-init safe, {@code false} otherwise
     */
    public boolean isStaticInitSafe() {
        return configClass.isAnnotationPresent(StaticInitSafe.class);
    }

    /**
     * Converts this build item into a {@link ConfigClass} representation,
     * used for internal processing and configuration resolution.
     *
     * @return a {@code ConfigClass} instance based on this build item
     */
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
