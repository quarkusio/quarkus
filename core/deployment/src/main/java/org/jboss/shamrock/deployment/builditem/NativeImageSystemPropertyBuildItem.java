package org.jboss.shamrock.deployment.builditem;

import org.jboss.builder.item.MultiBuildItem;

public final class NativeImageSystemPropertyBuildItem extends MultiBuildItem {

    private final String key;
    private final String value;

    public NativeImageSystemPropertyBuildItem(String key, String value) {
        this.key = key;
        this.value = value;
    }

    public String getKey() {
        return key;
    }

    public String getValue() {
        return value;
    }
}
