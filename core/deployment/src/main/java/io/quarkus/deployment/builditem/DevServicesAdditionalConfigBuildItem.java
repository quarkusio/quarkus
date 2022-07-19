package io.quarkus.deployment.builditem;

import java.util.Collection;
import java.util.List;

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

    private final Collection<String> triggeringKeys;
    private final String key;
    private final String value;
    private final Runnable callbackWhenEnabled;

    /**
     * @deprecated Call
     *             {@link DevServicesAdditionalConfigBuildItem#DevServicesAdditionalConfigBuildItem(Collection, String, String, Runnable)}
     *             instead.
     */
    @Deprecated
    public DevServicesAdditionalConfigBuildItem(String triggeringKey,
            String key, String value, Runnable callbackWhenEnabled) {
        this(List.of(triggeringKey), key, value, callbackWhenEnabled);
    }

    public DevServicesAdditionalConfigBuildItem(Collection<String> triggeringKeys,
            String key, String value, Runnable callbackWhenEnabled) {
        this.triggeringKeys = triggeringKeys;
        this.key = key;
        this.value = value;
        this.callbackWhenEnabled = callbackWhenEnabled;
    }

    /**
     * @deprecated Call {@link #getTriggeringKeys()} instead.
     */
    @Deprecated
    public String getTriggeringKey() {
        return getTriggeringKeys().iterator().next();
    }

    public Collection<String> getTriggeringKeys() {
        return triggeringKeys;
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
