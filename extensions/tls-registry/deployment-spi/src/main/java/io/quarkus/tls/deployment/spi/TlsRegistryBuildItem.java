package io.quarkus.tls.deployment.spi;

import java.util.function.Supplier;

import io.quarkus.builder.item.SimpleBuildItem;
import io.quarkus.tls.TlsConfigurationRegistry;

/**
 * A build item that indicates that the TLS registry has been initialized.
 * <p>
 * Do not produce this build item elsewhere than in the TLS registry deployment processor.
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
