package io.quarkus.opentelemetry.runtime.declarativeconfig;

import java.util.Set;

import io.opentelemetry.common.ComponentLoader;
import io.opentelemetry.sdk.autoconfigure.spi.ConfigProperties;
import io.opentelemetry.sdk.autoconfigure.spi.Ordered;
import io.opentelemetry.sdk.autoconfigure.spi.ResourceProvider;
import io.opentelemetry.sdk.autoconfigure.spi.internal.ConditionalResourceProvider;
import io.opentelemetry.sdk.resources.Resource;

// The resource of the autoconfigure ResourceProvider SPI, which upstream's declarative configuration does not run
final class QuarkusSpiResource {

    private QuarkusSpiResource() {
    }

    /**
     * Builds the {@link Resource} contributed by the autoconfigure {@link ResourceProvider} SPI, mirroring
     * autoconfigure's {@code ResourceConfiguration.configureResource}: providers are applied in
     * {@code Ordered.order()} order, filtered by {@code otel.java.enabled.resource.providers} and
     * {@code otel.java.disabled.resource.providers}, a {@link ConditionalResourceProvider} is consulted with the
     * result so far, and {@code otel.resource.disabled.keys} attributes are removed from the result. MicroProfile
     * Telemetry requires this SPI to be honored.
     * <p>
     * The result starts from {@link Resource#getDefault()}, like autoconfigure's; QuarkusDeclarativeSdk merges the
     * Quarkus-managed resource layers on top of it, so they win conflicting keys, matching the legacy ordering
     * where the {@code ResourceCustomizer} merged onto autoconfigure's result.
     * </p>
     */
    static Resource create(ComponentLoader componentLoader, ConfigProperties otelConfigProperties) {
        Resource result = Resource.getDefault();

        Set<String> enabledProviders = Set.copyOf(otelConfigProperties.getList("otel.java.enabled.resource.providers"));
        Set<String> disabledProviders = Set.copyOf(otelConfigProperties.getList("otel.java.disabled.resource.providers"));

        for (ResourceProvider provider : Ordered.loadOrderedList(componentLoader, ResourceProvider.class)) {
            if (!enabledProviders.isEmpty() && !enabledProviders.contains(provider.getClass().getName())) {
                continue;
            }
            if (disabledProviders.contains(provider.getClass().getName())) {
                continue;
            }
            if (provider instanceof ConditionalResourceProvider conditional
                    && !conditional.shouldApply(otelConfigProperties, result)) {
                continue;
            }
            result = result.merge(provider.createResource(otelConfigProperties));
        }

        Set<String> disabledKeys = Set.copyOf(otelConfigProperties.getList("otel.resource.disabled.keys"));
        if (disabledKeys.isEmpty()) {
            return result;
        }
        return result.toBuilder()
                .removeIf(attributeKey -> disabledKeys.contains(attributeKey.getKey()))
                .build();
    }
}
