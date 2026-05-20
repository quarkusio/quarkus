package io.quarkus.deployment.builditem;

import java.util.Collection;
import java.util.Collections;
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

    public DevServicesAdditionalConfigBuildItem(DevServicesAdditionalConfigProvider configProvider) {
        this.triggeringKeys = Collections.emptyList();
        this.key = null;
        this.value = null;
        this.callbackWhenEnabled = null;
        this.configProvider = configProvider;
    }

    public DevServicesAdditionalConfigProvider getConfigProvider() {
        return configProvider;
    }

    public interface DevServicesAdditionalConfigProvider {

        Map<String, String> provide(Map<String, String> devServicesConfig);

    }
}
