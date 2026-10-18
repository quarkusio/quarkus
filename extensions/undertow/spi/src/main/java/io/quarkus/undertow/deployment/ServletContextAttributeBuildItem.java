package io.quarkus.undertow.deployment;

import io.quarkus.builder.item.MultiBuildItem;

public final class ServletContextAttributeBuildItem extends MultiBuildItem {

    final String key;
    final Object value;

    public ServletContextAttributeBuildItem(String key, Object value) {
        this.key = key;
        this.value = value;
    }

    public String getKey() {
        return key;
    }

    public Object getValue() {
        return value;
    }
}
