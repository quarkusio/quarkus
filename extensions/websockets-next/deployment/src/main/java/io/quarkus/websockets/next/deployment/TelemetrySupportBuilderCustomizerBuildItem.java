package io.quarkus.websockets.next.deployment;

import java.util.function.Consumer;

import io.quarkus.builder.item.MultiBuildItem;
import io.quarkus.websockets.next.runtime.telemetry.TelemetrySupportProviderBuilder;

/**
 * Provides a way to set up metrics and/or traces support in the WebSockets extension.
 */
final class TelemetrySupportBuilderCustomizerBuildItem extends MultiBuildItem {

    final Consumer<TelemetrySupportProviderBuilder> builderCustomizer;

    TelemetrySupportBuilderCustomizerBuildItem(Consumer<TelemetrySupportProviderBuilder> builderCustomizer) {
        this.builderCustomizer = builderCustomizer;
    }
}
