package io.quarkus.deployment.builditem.nativeimage;

import io.quarkus.builder.item.MultiBuildItem;

/**
 * A system property that will be set at native image build time
 */
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
