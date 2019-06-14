package io.quarkus.undertow.deployment;

import io.quarkus.builder.item.MultiBuildItem;

public final class ListenerBuildItem extends MultiBuildItem {

    private final String listenerClass;

    public ListenerBuildItem(String listenerClass) {
        this.listenerClass = listenerClass;
    }

    public String getListenerClass() {
        return listenerClass;
    }

}
