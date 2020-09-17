package io.quarkus.arc.deployment;

import io.quarkus.builder.item.MultiBuildItem;

public final class ConfigMappingBuildItem extends MultiBuildItem {
    private final Class<?> interfaceType;
    private final String prefix;

    public ConfigMappingBuildItem(final Class<?> interfaceType, final String prefix) {
        this.interfaceType = interfaceType;
        this.prefix = prefix;
    }

    public Class<?> getInterfaceType() {
        return interfaceType;
    }

    public String getPrefix() {
        return prefix;
    }
}
