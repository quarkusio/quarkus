package io.quarkus.tls;

import java.util.function.Supplier;

import io.quarkus.builder.item.SimpleBuildItem;

/**
 * A build item that indicates that the TLS registry has been initialized.
 */
public final class TlsRegistryBuildItem extends SimpleBuildItem {

    private final Supplier<TlsConfigurationRegistry> registry;

    public TlsRegistryBuildItem(Supplier<TlsConfigurationRegistry> reference) {
        this.registry = reference;
    }

    public Supplier<TlsConfigurationRegistry> registry() {
        return registry;
    }
}
