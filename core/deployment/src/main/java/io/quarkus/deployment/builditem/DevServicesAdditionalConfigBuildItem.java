package io.quarkus.deployment.builditem;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;

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

    private final DevServicesAdditionalConfigProvider configProvider;
    private final Collection<String> triggeringKeys;
    private final String key;
    private final String value;
    private final Runnable callbackWhenEnabled;

    /**
     * @deprecated Call
     *             {@link DevServicesAdditionalConfigBuildItem#DevServicesAdditionalConfigBuildItem(DevServicesAdditionalConfigProvider)}
     *             instead.
     */
    @Deprecated
    public DevServicesAdditionalConfigBuildItem(String triggeringKey,
            String key, String value, Runnable callbackWhenEnabled) {
        this(List.of(triggeringKey), key, value, callbackWhenEnabled);
    }

    /**
     * @deprecated Call
     *             {@link DevServicesAdditionalConfigBuildItem#DevServicesAdditionalConfigBuildItem(DevServicesAdditionalConfigProvider)}
     *             instead.
     */
    @Deprecated
    public DevServicesAdditionalConfigBuildItem(Collection<String> triggeringKeys,
            String key, String value, Runnable callbackWhenEnabled) {
        this.triggeringKeys = triggeringKeys;
        this.key = key;
        this.value = value;
        this.callbackWhenEnabled = callbackWhenEnabled;
        this.configProvider = devServicesConfig -> {
            if (triggeringKeys.stream().anyMatch(devServicesConfig::containsKey)) {
                if (callbackWhenEnabled != null) {
                    callbackWhenEnabled.run();
                }
                return Map.of(key, value);
            } else {
                return Map.of();
            }
        };
    }

    public DevServicesAdditionalConfigBuildItem(DevServicesAdditionalConfigProvider configProvider) {
        this.triggeringKeys = Collections.emptyList();
        this.key = null;
        this.value = null;
        this.callbackWhenEnabled = null;
        this.configProvider = configProvider;
    }

    /**
     * @deprecated Don't call this method, use {@link #getConfigProvider()} instead.
     */
    @Deprecated
    public String getTriggeringKey() {
        return getTriggeringKeys().iterator().next();
    }

    /**
     * @deprecated Don't call this method, use {@link #getConfigProvider()} instead.
     */
    @Deprecated
    public Collection<String> getTriggeringKeys() {
        return triggeringKeys;
    }

    /**
     * @deprecated Don't call this method, use {@link #getConfigProvider()} instead.
     */
    @Deprecated
    public String getKey() {
        return key;
    }

    /**
     * @deprecated Don't call this method, use {@link #getConfigProvider()} instead.
     */
    @Deprecated
    public String getValue() {
        return value;
    }

    /**
     * @deprecated Don't call this method, use {@link #getConfigProvider()} instead.
     */
    @Deprecated
    public Runnable getCallbackWhenEnabled() {
        return callbackWhenEnabled;
    }

    public DevServicesAdditionalConfigProvider getConfigProvider() {
        return configProvider;
    }

    public interface DevServicesAdditionalConfigProvider {

        Map<String, String> provide(Map<String, String> devServicesConfig);

    }
}
