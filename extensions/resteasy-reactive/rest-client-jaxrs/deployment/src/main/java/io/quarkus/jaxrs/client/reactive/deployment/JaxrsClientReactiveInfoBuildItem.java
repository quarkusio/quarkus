package io.quarkus.jaxrs.client.reactive.deployment;

import java.util.Set;

import io.quarkus.builder.item.SimpleBuildItem;

/**
 * Contains information for dev-ui about jax rs clients
 */
public final class JaxrsClientReactiveInfoBuildItem extends SimpleBuildItem {
    private final Set<String> interfaceNames;

    public JaxrsClientReactiveInfoBuildItem(Set<String> interfaceNames) {
        this.interfaceNames = interfaceNames;
    }

    /**
     * Names of all the rest client interfaces for which an implementation was successfully generated
     *
     * @return set, may be empty, never null
     */
    public Set<String> getInterfaceNames() {
        return interfaceNames;
    }
}
