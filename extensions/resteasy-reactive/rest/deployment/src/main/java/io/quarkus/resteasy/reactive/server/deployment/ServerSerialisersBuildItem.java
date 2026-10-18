package io.quarkus.resteasy.reactive.server.deployment;

import org.jboss.resteasy.reactive.server.core.ServerSerialisers;

import io.quarkus.builder.item.SimpleBuildItem;

public final class ServerSerialisersBuildItem extends SimpleBuildItem {

    private final ServerSerialisers serialisers;

    public ServerSerialisersBuildItem(ServerSerialisers serialisers) {
        this.serialisers = serialisers;
    }

    public ServerSerialisers getSerialisers() {
        return serialisers;
    }
}
