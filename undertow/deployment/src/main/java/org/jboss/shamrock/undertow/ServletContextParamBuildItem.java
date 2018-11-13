package org.jboss.shamrock.undertow;

import org.jboss.builder.item.MultiBuildItem;

public final class ServletContextParamBuildItem extends MultiBuildItem {

    final String key;
    final String value;

    public ServletContextParamBuildItem(String key, String value) {
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
