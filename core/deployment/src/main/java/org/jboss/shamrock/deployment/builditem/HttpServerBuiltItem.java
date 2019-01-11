package org.jboss.shamrock.deployment.builditem;

import org.jboss.builder.item.SimpleBuildItem;

public final class HttpServerBuiltItem extends SimpleBuildItem {
    
    private final String host;

    private final Integer port;

    public HttpServerBuiltItem(String host, Integer port) {
        this.host = host;
        this.port = port;
    }

    public String getHost() {
        return host;
    }

    public Integer getPort() {
        return port;
    }

}
