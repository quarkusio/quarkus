package io.quarkus.deployment.builditem;

import io.quarkus.builder.item.MultiBuildItem;

/**
 * An additional configuration property to set when a dev service sets another, specific configuration property.
 * <p>
 * Quarkus will make sure the relevant settings are present in both JVM and native modes.
 * <p>
 * This is used to change the defaults of extension configuration when dev services are in use,
 * for example to enable schema management in the Hibernate ORM extension.
 */
public final class DevServicesAdditionalConfigBuildItem extends MultiBuildItem {

    private final String triggeringKey;
    private final String key;
    private final String value;
    private final Runnable callbackWhenEnabled;

    public DevServicesAdditionalConfigBuildItem(String triggeringKey,
            String key, String value, Runnable callbackWhenEnabled) {
        this.triggeringKey = triggeringKey;
        this.key = key;
        this.value = value;
        this.callbackWhenEnabled = callbackWhenEnabled;
    }

    public String getTriggeringKey() {
        return triggeringKey;
    }

    public String getKey() {
        return key;
    }

    public String getValue() {
        return value;
    }

    public Runnable getCallbackWhenEnabled() {
        return callbackWhenEnabled;
    }
}
