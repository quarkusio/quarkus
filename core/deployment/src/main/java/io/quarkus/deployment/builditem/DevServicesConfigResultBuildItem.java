package io.quarkus.deployment.builditem;

import io.quarkus.builder.item.MultiBuildItem;

/**
 * Configuration property that is the result of starting dev services.
 *
 * Used to start and configure dev services, any processor starting dev services should produce these items.
 *
 * Quarkus will make sure the relevant settings are present in both JVM and native modes.
 *
 * @deprecated use {@link DevServicesResultBuildItem}
 */
@Deprecated(since = "https://github.com/quarkusio/quarkus/pull/23048")
public final class DevServicesConfigResultBuildItem extends MultiBuildItem {

    final String key;
    final String value;

    public DevServicesConfigResultBuildItem(String key, String value) {
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
