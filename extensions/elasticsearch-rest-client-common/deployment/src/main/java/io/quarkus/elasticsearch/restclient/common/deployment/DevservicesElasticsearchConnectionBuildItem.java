package io.quarkus.elasticsearch.restclient.common.deployment;

import io.quarkus.builder.item.MultiBuildItem;
import io.quarkus.elasticsearch.restclient.common.deployment.ElasticsearchCommonBuildTimeConfig.ElasticsearchDevServicesBuildTimeConfig.Distribution;

public final class DevservicesElasticsearchConnectionBuildItem extends MultiBuildItem {
    private final String host;
    private final int port;
    private final Distribution distribution;

    public DevservicesElasticsearchConnectionBuildItem(String host, int port, Distribution distribution) {
        this.host = host;
        this.port = port;
        this.distribution = distribution;
    }

    public String getHost() {
        return host;
    }

    public int getPort() {
        return port;
    }

    public Distribution getDistribution() {
        return distribution;
    }
}