package org.jboss.shamrock.undertow;

import org.jboss.builder.item.MultiBuildItem;

import io.undertow.server.HandlerWrapper;

public final class HttpHandlerWrapperBuildItem extends MultiBuildItem {

    private final HandlerWrapper value;

    public HttpHandlerWrapperBuildItem(HandlerWrapper value) {
        this.value = value;
    }

    public HandlerWrapper getValue() {
        return value;
    }
}
