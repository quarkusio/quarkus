package io.quarkus.websockets.next.deployment;

import java.util.function.Consumer;

import io.quarkus.builder.item.MultiBuildItem;
import io.quarkus.websockets.next.runtime.telemetry.WebsocketTelemetryProviderBuilder;

/**
 * Provides a way to set up metrics and/or traces support in the WebSockets extension.
 */
final class WebsocketTelemetryCustomizerBuildItem extends MultiBuildItem {

    final Consumer<WebsocketTelemetryProviderBuilder> builderCustomizer;

    WebsocketTelemetryCustomizerBuildItem(Consumer<WebsocketTelemetryProviderBuilder> builderCustomizer) {
        this.builderCustomizer = builderCustomizer;
    }
}
