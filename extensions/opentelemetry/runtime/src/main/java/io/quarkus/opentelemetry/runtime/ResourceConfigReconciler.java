package io.quarkus.opentelemetry.runtime;

import java.util.List;
import java.util.Map;
import java.util.Set;

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

    static final String OTEL_SERVICE_NAME = "otel.service.name";

    private static final String OTEL_DISABLED_RESOURCE_PROVIDERS = "otel.java.disabled.resource.providers";
    private static final String SERVICE_INSTANCE_ID_PROVIDER = "io.opentelemetry.sdk.autoconfigure.ServiceInstanceIdResourceProvider";

    private ResourceConfigReconciler() {
    }

    /**
     * The service name is not written here. It is resolved by
     * {@link AutoConfiguredOpenTelemetrySdkBuilderCustomizer.ServiceNameCustomizer}, because these
     * properties reach the SDK through {@code addPropertiesSupplier}, the lowest precedence channel,
     * where an explicit Quarkus setting would lose to an ambient {@code OTEL_SERVICE_NAME}.
     *
     * @param oTelConfigs the properties handed to the autoconfiguration, modified in place
     * @param resourceAttributes the parsed {@code quarkus.otel.resource.attributes}. The parsed list is
     *        used rather than the forwarded {@code otel.resource.attributes} string because SmallRye
     *        Config also accepts the indexed form, which forwards as {@code otel.resource.attributes[0]}
     *        and would hide every attribute from a reader of the plain key. It is also what
     *        {@link io.quarkus.opentelemetry.runtime.tracing.TracerUtil} filters, so both agree.
     */
    public static void reconcile(Map<String, String> oTelConfigs, List<String> resourceAttributes) {
        if (OpenTelemetryUtil.convertKeyValueListToMap(resourceAttributes).containsKey(SERVICE_INSTANCE_ID)) {
            disableServiceInstanceIdProvider(oTelConfigs);
        }
    }

    /**
     * {@code quarkus.otel.service.name} defaults to the application name, so a value equal to it means
     * the user did not set it and a {@code service.name} in the resource attributes is the more
     * specific choice. The SDK builds the {@code service} resource entity from the resolved name, and
     * silently drops the resource attribute of the same name in favour of it.
     *
     * @return the service name the user asked for, or {@code null} when they asked for none and
     *         whatever the SDK resolves on its own should stand
     */
    public static String resolveServiceName(String otelServiceName, List<String> resourceAttributes,
            String quarkusApplicationName) {
        if (otelServiceName != null && !otelServiceName.equals(quarkusApplicationName)) {
            return otelServiceName;
        }
        return OpenTelemetryUtil.convertKeyValueListToMap(resourceAttributes).get(SERVICE_NAME);
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
}
