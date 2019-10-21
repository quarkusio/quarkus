package io.quarkus.deployment.builditem.substrate;

import io.quarkus.builder.item.MultiBuildItem;

/**
 * A system property that will be set at native image build time
 * 
 * @deprecated Use {@link io.quarkus.deployment.builditem.nativeimage.NativeImageSystemPropertyBuildItem
 *             NativeImageSystemPropertyBuildItem} instead.
 */
@Deprecated
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
