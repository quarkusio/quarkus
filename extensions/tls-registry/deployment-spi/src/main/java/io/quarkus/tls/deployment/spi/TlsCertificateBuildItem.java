package io.quarkus.tls.deployment.spi;

import java.util.function.Supplier;

import io.quarkus.builder.item.MultiBuildItem;
import io.quarkus.tls.TlsConfiguration;

/**
 * A build item to register a TLS certificate.
 * An extension can produce an instance of this build item to add a custom certificate into the registry.
 */
public final class TlsCertificateBuildItem extends MultiBuildItem {

    public final String name;

    public final Supplier<TlsConfiguration> supplier;

    /**
     * Create an instance of {@link TlsCertificateBuildItem} to register a TLS certificate.
     * The certificate will be registered just after the regular TLS certificate configuration is registered.
     *
     * @param name the name of the certificate, cannot be {@code null}, cannot be {@code <default>}
     * @param supplier the supplier providing the TLS configuration, must not return {@code null}
     */
    public TlsCertificateBuildItem(String name, Supplier<TlsConfiguration> supplier) {
        this.name = name;
        this.supplier = supplier;
    }

}
