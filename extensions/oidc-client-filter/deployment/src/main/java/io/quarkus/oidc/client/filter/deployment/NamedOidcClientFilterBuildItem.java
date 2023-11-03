package io.quarkus.oidc.client.filter.deployment;

import java.util.Set;

import io.quarkus.builder.item.SimpleBuildItem;

/**
 * Contains a list of all Rest clients annotated with @OidcClientFilter("someClientName").
 */
public final class NamedOidcClientFilterBuildItem extends SimpleBuildItem {

    final Set<String> namedFilterClientClasses;

    NamedOidcClientFilterBuildItem(Set<String> namedFilterClientClasses) {
        this.namedFilterClientClasses = Set.copyOf(namedFilterClientClasses);
    }
}
