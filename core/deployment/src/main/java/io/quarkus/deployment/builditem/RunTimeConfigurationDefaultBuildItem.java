package io.quarkus.deployment.builditem;

import org.jboss.builder.item.MultiBuildItem;
import org.wildfly.common.Assert;

import io.quarkus.runtime.annotations.ConfigItem;

/**
 * A build item which specifies a configuration default value for run time, which is used to establish a default other
 * than the one given for {@link ConfigItem#defaultValue()}.
 */
public final class RunTimeConfigurationDefaultBuildItem extends MultiBuildItem {
    private final String key;
    private final String value;

    /**
     * Construct a new instance.
     *
     * @param key the configuration key (must not be {@code null} or empty)
     * @param value the configuration value (must not be {@code null})
     */
    public RunTimeConfigurationDefaultBuildItem(final String key, final String value) {
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
