package io.quarkus.opentelemetry.runtime;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Adjusts the OpenTelemetry autoconfiguration properties so that the SDK builds the resource
 * entities Quarkus expects.
 * <p>
 * Since SDK 1.67 a {@code Resource} carries resource entities — the OpenTelemetry kind, unrelated to
 * JPA entities — and an ordinary attribute that changes an entity owned value drops the entity
 * association. The SDK owns two such attributes out of the box: {@code service.name}, which identifies
 * the {@code service} entity, and {@code service.instance.id}, which identifies the
 * {@code service.instance} entity.
 */
public final class ResourceConfigReconciler {

    public static final String SERVICE_NAME = "service.name";
    public static final String SERVICE_INSTANCE_ID = "service.instance.id";

    /**
     * The attributes the SDK builds resource entities from. Quarkus must not write them as ordinary
     * attributes.
     */
    public static final Set<String> ENTITY_OWNED_ATTRIBUTES = Set.of(SERVICE_NAME, SERVICE_INSTANCE_ID);

    private static final String OTEL_SERVICE_NAME = "otel.service.name";
    private static final String OTEL_RESOURCE_ATTRIBUTES = "otel.resource.attributes";
    private static final String OTEL_DISABLED_RESOURCE_PROVIDERS = "otel.java.disabled.resource.providers";
    private static final String SERVICE_INSTANCE_ID_PROVIDER = "io.opentelemetry.sdk.autoconfigure.ServiceInstanceIdResourceProvider";

    // SmallRye Config escapes a comma that belongs to a value as \, so only unescaped commas separate
    private static final Pattern ATTRIBUTE_SEPARATOR = Pattern.compile("(?<!\\\\),");

    private ResourceConfigReconciler() {
    }

    /**
     * @param oTelConfigs the properties handed to the autoconfiguration, modified in place
     * @param applicationName the value {@code quarkus.otel.service.name} defaults to
     */
    public static void reconcile(Map<String, String> oTelConfigs, String applicationName) {
        Map<String, String> attributes = OpenTelemetryUtil.convertKeyValueListToMap(
                split(oTelConfigs.get(OTEL_RESOURCE_ATTRIBUTES)));

        resolveServiceName(oTelConfigs, attributes.get(SERVICE_NAME), applicationName);

        if (attributes.containsKey(SERVICE_INSTANCE_ID)) {
            disableServiceInstanceIdProvider(oTelConfigs);
        }
    }

    /**
     * {@code quarkus.otel.service.name} defaults to the application name, so a value equal to it means
     * the user did not set it and a {@code service.name} in the resource attributes is the more
     * specific choice. The SDK builds the {@code service} resource entity from the resolved name, and
     * silently drops the resource attribute of the same name in favour of it.
     */
    private static void resolveServiceName(Map<String, String> oTelConfigs, String attributeServiceName,
            String applicationName) {
        if (attributeServiceName == null) {
            return;
        }

        String serviceName = oTelConfigs.get(OTEL_SERVICE_NAME);
        if (serviceName == null || serviceName.equals(applicationName)) {
            oTelConfigs.put(OTEL_SERVICE_NAME, attributeServiceName);
        }
    }

    /**
     * The provider builds a {@code service.instance} resource entity from an id it generates itself,
     * and a user supplied id would overwrite it and drop the association. Switching the provider off
     * leaves the user value as a plain attribute with no entity to collide with: that path
     * deliberately has no {@code service.instance} entity.
     */
    private static void disableServiceInstanceIdProvider(Map<String, String> oTelConfigs) {
        String disabledProviders = oTelConfigs.get(OTEL_DISABLED_RESOURCE_PROVIDERS);
        if (disabledProviders == null || disabledProviders.isBlank()) {
            oTelConfigs.put(OTEL_DISABLED_RESOURCE_PROVIDERS, SERVICE_INSTANCE_ID_PROVIDER);
            return;
        }
        for (String provider : disabledProviders.split(",")) {
            if (provider.trim().equals(SERVICE_INSTANCE_ID_PROVIDER)) {
                return;
            }
        }
        oTelConfigs.put(OTEL_DISABLED_RESOURCE_PROVIDERS, disabledProviders + "," + SERVICE_INSTANCE_ID_PROVIDER);
    }

    private static List<String> split(String resourceAttributes) {
        return resourceAttributes == null ? List.of() : List.of(ATTRIBUTE_SEPARATOR.split(resourceAttributes));
    }
}
