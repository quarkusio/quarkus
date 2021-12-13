package io.quarkus.oidc.deployment;

import java.util.Optional;
import java.util.function.Supplier;

import io.quarkus.builder.item.MultiBuildItem;
import io.quarkus.oidc.OidcTenantConfig;

/**
 * An interface that abstracts details of a well known OIDC provider (google, github etc)
 */
public final class KnownProviderSupplierBuildItem extends MultiBuildItem {

    final String name;
    final Supplier<Optional<OidcTenantConfig>> supplier;

    public KnownProviderSupplierBuildItem(String name, Supplier<Optional<OidcTenantConfig>> supplier) {
        this.name = name;
        this.supplier = supplier;
    }

    public Supplier<Optional<OidcTenantConfig>> getSupplier() {
        return supplier;
    }

    public String getName() {
        return name;
    }
}
