package org.jboss.shamrock.deployment.builditem.substrate;

import org.jboss.builder.item.MultiBuildItem;

public final class SubstrateSystemPropertyBuildItem extends MultiBuildItem {

    private final String key;
    private final String value;

    public SubstrateSystemPropertyBuildItem(String key, String value) {
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
