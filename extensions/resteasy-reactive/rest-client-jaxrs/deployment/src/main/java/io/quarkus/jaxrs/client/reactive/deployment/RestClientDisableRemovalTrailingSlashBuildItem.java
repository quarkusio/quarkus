package io.quarkus.jaxrs.client.reactive.deployment;

import java.util.List;

import org.jboss.jandex.DotName;

import io.quarkus.builder.item.MultiBuildItem;

/**
 * This build item disables the removal of trailing slashes from the paths.
 */
public final class RestClientDisableRemovalTrailingSlashBuildItem extends MultiBuildItem {
    private final List<DotName> clients;

    public RestClientDisableRemovalTrailingSlashBuildItem(List<DotName> clients) {
        this.clients = clients;
    }

    public List<DotName> getClients() {
        return clients;
    }
}
