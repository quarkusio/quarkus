package io.quarkus.deployment.builditem;

import java.util.Objects;

import io.quarkus.builder.item.MultiBuildItem;

public final class ConfigPropertiesBuildItem extends MultiBuildItem {
    private final Class<?> configClass;
    private final String prefix;

    public ConfigPropertiesBuildItem(final Class<?> configClass, final String prefix) {
        this.configClass = configClass;
        this.prefix = prefix;
    }

    public Class<?> getConfigClass() {
        return configClass;
    }

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
