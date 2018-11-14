package org.jboss.shamrock.jaxrs;

import org.jboss.builder.item.MultiBuildItem;

/**
 * A build item that represents a JAX-RS provider class, these items will be merged
 * into the 'resteasy.providers' context param.
 */
public final class JaxrsProviderBuildItem extends MultiBuildItem {

    private final String name;

    public JaxrsProviderBuildItem(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }
}
