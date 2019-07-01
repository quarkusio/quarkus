package io.quarkus.deployment.builditem;

import org.wildfly.common.Assert;

import io.quarkus.builder.item.MultiBuildItem;
import io.quarkus.runtime.annotations.ConfigItem;

/**
 * A build item which specifies a configuration default value for build time, which is used to establish a default other
 * than the one given for {@link ConfigItem#defaultValue()}.
 */
public final class BuildTimeConfigurationDefaultBuildItem extends MultiBuildItem {
    private final String key;
    private final String value;

    /**
     * Construct a new instance.
     *
     * @param key the configuration key (must not be {@code null} or empty)
     * @param value the configuration value (must not be {@code null})
     */
    public BuildTimeConfigurationDefaultBuildItem(final String key, final String value) {
        Assert.checkNotNullParam("key", key);
        Assert.checkNotEmptyParam("key", key);
        Assert.checkNotNullParam("value", value);
        this.key = key;
        this.value = value;
    }

    /**
     * Get the configuration key.
     *
     * @return the configuration key (not {@code null} or empty)
     */
    public String getKey() {
        return key;
    }

    /**
     * Get the configuration value.
     *
     * @return the configuration value (must not be {@code null})
     */
    public String getValue() {
        return value;
    }
}
