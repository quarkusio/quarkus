package io.quarkus.proxy.deployment;

import java.util.function.Supplier;

import io.quarkus.builder.item.SimpleBuildItem;
import io.quarkus.proxy.ProxyConfigurationRegistry;

/**
 * A build item that indicates that the proxy registry has been initialized.
 */
public final class ProxyRegistryBuildItem extends SimpleBuildItem {
    private final Supplier<ProxyConfigurationRegistry> registry;

    public ProxyRegistryBuildItem(Supplier<ProxyConfigurationRegistry> registry) {
        this.registry = registry;
    }

    public Supplier<ProxyConfigurationRegistry> registry() {
        return registry;
    }
}
