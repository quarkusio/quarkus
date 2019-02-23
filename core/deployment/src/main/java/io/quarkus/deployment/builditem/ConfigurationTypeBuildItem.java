package io.quarkus.deployment.builditem;

import org.jboss.builder.item.MultiBuildItem;
import org.wildfly.common.Assert;

/**
 * The configuration type build item. Every configuration type should be registered using this build item
 * to ensure that the converter is properly loaded in the native image case.
 */
public final class ConfigurationTypeBuildItem extends MultiBuildItem {
    private final Class<?> valueType;

    /**
     * Construct a new instance.
     *
     * @param valueType the value type (must not be {@code null})
     */
    public ConfigurationTypeBuildItem(final Class<?> valueType) {
        Assert.checkNotNullParam("valueType", valueType);
        this.valueType = valueType;
    }

    /**
     * Get the value type.
     *
     * @return the value type (not {@code null})
     */
    public Class<?> getValueType() {
        return valueType;
    }
}
