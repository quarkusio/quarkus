package io.quarkus.deployment.builditem;

import java.util.Objects;

import io.quarkus.builder.item.MultiBuildItem;

/**
 * A {@link MultiBuildItem} used to register a class annotated with {@code @ConfigProperties}
 * for build-time configuration mapping in Quarkus.
 *
 * <p>
 * This build item allows Quarkus to generate an implementation of the configuration class
 * based on the provided prefix, enabling strong typing and reflection-free access
 * to configuration properties.
 * </p>
 *
 * <p>
 * Multiple instances of this build item can be produced, one for each configuration class.
 * </p>
 *
 * @see org.eclipse.microprofile.config.inject.ConfigProperties
 */
public final class ConfigPropertiesBuildItem extends MultiBuildItem {
    private final Class<?> configClass;
    private final String prefix;

    /**
     * Constructs a new {@code ConfigPropertiesBuildItem}.
     *
     * @param configClass the class annotated with {@code @ConfigProperties}. Must not be {@code null}.
     * @param prefix the configuration prefix associated with the class. Must not be {@code null}.
     */
    public ConfigPropertiesBuildItem(final Class<?> configClass, final String prefix) {
        this.configClass = configClass;
        this.prefix = prefix;
    }

    /**
     * Returns the configuration class annotated with {@code @ConfigProperties}.
     *
     * @return the configuration class.
     */
    public Class<?> getConfigClass() {
        return configClass;
    }

    /**
     * Returns the configuration prefix that should be used to map configuration properties
     * to the annotated class.
     *
     * @return the configuration prefix.
     */
    public String getPrefix() {
        return prefix;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        final ConfigPropertiesBuildItem that = (ConfigPropertiesBuildItem) o;
        return configClass.equals(that.configClass) &&
                prefix.equals(that.prefix);
    }

    @Override
    public int hashCode() {
        return Objects.hash(configClass, prefix);
    }
}
