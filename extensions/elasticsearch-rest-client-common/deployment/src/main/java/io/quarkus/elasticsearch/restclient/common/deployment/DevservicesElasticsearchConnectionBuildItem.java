package io.quarkus.elasticsearch.restclient.common.deployment;

import io.quarkus.builder.item.MultiBuildItem;

public final class DevservicesElasticsearchConnectionBuildItem extends MultiBuildItem {
    private final String host;
    private final int port;

    public DevservicesElasticsearchConnectionBuildItem(String host, int port) {
        this.host = host;
        this.port = port;
    }

    public String getHost() {
        return host;
    }

    public int getPort() {
        return port;
    }
}