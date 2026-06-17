package io.quarkus.deployment.builditem;

import io.quarkus.builder.item.MultiBuildItem;
import io.smallrye.common.constraint.Assert;

/**
 * A build item which specifies a configuration default value for run time.
 */
public final class RunTimeConfigurationDefaultBuildItem extends MultiBuildItem {
    private final String key;
    private final String value;
    private final boolean includedInReproducibilityCheck;

    /**
     * Construct a new instance.
     *
     * @param key the configuration key (must not be {@code null} or empty)
     * @param value the configuration value (must not be {@code null})
     */
    public RunTimeConfigurationDefaultBuildItem(final String key, final String value) {
        this(key, value, true);
    }

    /**
     * Construct a new instance.
     *
     * @param key the configuration key (must not be {@code null} or empty)
     * @param value the configuration value (must not be {@code null})
     * @param includedInReproducibilityCheck {@code true} if this default should be included in generated config during
     *        reproducibility checks
     */
    public RunTimeConfigurationDefaultBuildItem(final String key, final String value,
            boolean includedInReproducibilityCheck) {
        Assert.checkNotNullParam("key", key);
        Assert.checkNotEmptyParam("key", key);
        Assert.checkNotNullParam("value for key " + key, value);
        this.key = key;
        this.value = value;
        this.includedInReproducibilityCheck = includedInReproducibilityCheck;
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

    /**
     * @return {@code true} if this default should be included in generated config during reproducibility checks
     */
    public boolean isIncludedInReproducibilityCheck() {
        return includedInReproducibilityCheck;
    }
}
