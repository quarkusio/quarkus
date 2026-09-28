package io.quarkus.devui.deployment;

import java.util.List;

import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;

/**
 * Backward compatibility: convert deprecated {@link io.quarkus.devui.spi.JsonRPCProvidersBuildItem}
 * instances produced by extensions that have not migrated yet into the new
 * {@link io.quarkus.devjsonrpc.spi.JsonRPCProvidersBuildItem}.
 *
 * @deprecated Will be removed when the legacy build item is removed.
 */
@Deprecated(since = "4.0", forRemoval = true)
public class JsonRPCProvidersBackwardCompatibilityProcessor {

    @BuildStep
    void convertLegacyJsonRPCProviders(
            List<io.quarkus.devui.spi.JsonRPCProvidersBuildItem> legacyItems,
            BuildProducer<io.quarkus.devjsonrpc.spi.JsonRPCProvidersBuildItem> producer) {
        for (io.quarkus.devui.spi.JsonRPCProvidersBuildItem legacy : legacyItems) {
            if (legacy.getCustomIdentifier() != null) {
                producer.produce(new io.quarkus.devjsonrpc.spi.JsonRPCProvidersBuildItem(
                        legacy.getCustomIdentifier(), legacy.getJsonRPCMethodProviderClass()));
            } else if (legacy.getDefaultBeanScope() != null) {
                producer.produce(new io.quarkus.devjsonrpc.spi.JsonRPCProvidersBuildItem(
                        legacy.getJsonRPCMethodProviderClass(), legacy.getDefaultBeanScope()));
            } else {
                producer.produce(new io.quarkus.devjsonrpc.spi.JsonRPCProvidersBuildItem(
                        legacy.getJsonRPCMethodProviderClass()));
            }
        }
    }
}
