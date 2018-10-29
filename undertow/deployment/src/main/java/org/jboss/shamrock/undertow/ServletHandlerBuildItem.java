package org.jboss.shamrock.undertow;

import org.jboss.builder.item.SimpleBuildItem;

import io.undertow.server.HttpHandler;

public final class ServletHandlerBuildItem extends SimpleBuildItem {

    private final HttpHandler handler;

    public ServletHandlerBuildItem(HttpHandler handler) {
        this.handler = handler;
    }

    public HttpHandler getHandler() {
        return handler;
    }
}
