package io.quarkus.elytron.security.deployment;

import java.util.Objects;

import org.wildfly.security.auth.server.SecurityRealm;

import io.quarkus.builder.item.MultiBuildItem;

/**
 * Declares the name of a {@link SecurityRealm} registered as a service by a realm extension. Elytron only supports one
 * realm per realm name in a security domain, so every realm extension must produce this build item for each realm it
 * registers; the build fails when two realms declare the same name.
 */
public final class ElytronSecurityRealmNameValidationBuildItem extends MultiBuildItem {

    final String realmName;
    /**
     * The configuration property that defines the realm name, or {@code null} when the realm name is fixed.
     */
    final String realmNameConfigProperty;
    final String extensionName;

    /**
     * Declares a realm whose name is defined by a configuration property.
     *
     * @param realmName the realm name
     * @param realmNameConfigProperty the configuration property that defines the realm name
     * @param extensionName the name of the extension that registers the realm
     */
    public ElytronSecurityRealmNameValidationBuildItem(String realmName, String realmNameConfigProperty, String extensionName) {
        this.realmName = Objects.requireNonNull(realmName);
        this.realmNameConfigProperty = Objects.requireNonNull(realmNameConfigProperty);
        this.extensionName = Objects.requireNonNull(extensionName);
    }

    /**
     * Declares a realm whose name is fixed, that is, not configurable.
     *
     * @param realmName the realm name
     * @param extensionName the name of the extension that registers the realm
     */
    public ElytronSecurityRealmNameValidationBuildItem(String realmName, String extensionName) {
        this.realmName = Objects.requireNonNull(realmName);
        this.realmNameConfigProperty = null;
        this.extensionName = Objects.requireNonNull(extensionName);
    }
}
