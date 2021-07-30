package io.quarkus.deployment.builditem;

import io.quarkus.builder.item.MultiBuildItem;

/**
 * see {@link DevServicesConfigResultBuildItem} and {@link DevServicesLauncherConfigResultBuildItem}
 */
@Deprecated
public final class DevServicesNativeConfigResultBuildItem extends MultiBuildItem {

    final String key;
    final String value;

    public DevServicesNativeConfigResultBuildItem(String key, String value) {
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
