package io.quarkus.resteasy.common.deployment;

import io.quarkus.builder.item.MultiBuildItem;

/**
 * Used to mark a class as a potential REST client interface consumed by the MicroProfile REST client.
 * <p>
 * Useful when you want to apply different behaviors to REST resources and REST clients.
 */
public final class RestClientBuildItem extends MultiBuildItem {

    private String interfaceName;

    public RestClientBuildItem(String interfaceName) {
        this.interfaceName = interfaceName;
    }

    public String getInterfaceName() {
        return interfaceName;
    }
}
