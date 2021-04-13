package io.quarkus.oidc.client.deployment;

import java.util.Collections;
import java.util.Set;

import io.quarkus.builder.item.SimpleBuildItem;

/**
 * Contains non-default names of OIDC Clients.
 */
public final class OidcClientNamesBuildItem extends SimpleBuildItem {
    private final Set<String> oidcClientNames;

    OidcClientNamesBuildItem(Set<String> oidcClientNames) {
        this.oidcClientNames = Collections.unmodifiableSet(oidcClientNames);
    }

    Set<String> oidcClientNames() {
        return oidcClientNames;
    }
}