package io.quarkus.kubernetes.spi;

import io.quarkus.builder.item.MultiBuildItem;

/**
 * A build item that wraps around ConfigurationSupplier objects. The purpose of those build items is influence the
 * configuration that will be feed to the generator process.
 */
public final class ConfigurationSupplierBuildItem extends MultiBuildItem {

    /**
     * The configuration supplier
     */
    private final Object configurationSupplier;

    public ConfigurationSupplierBuildItem(Object configurationSupplier) {
        this.configurationSupplier = configurationSupplier;
    }

    public Object getConfigurationSupplier() {
        return this.configurationSupplier;
    }

    public boolean matches(Class type) {
        return type.isInstance(configurationSupplier);
    }
}
