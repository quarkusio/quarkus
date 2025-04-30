package io.quarkus.resteasy.reactive.server.spi;

import org.jboss.resteasy.reactive.server.model.HandlerChainCustomizer;

import io.quarkus.builder.item.MultiBuildItem;

/**
 * Allows for extension to register global handler customizers.
 * These are useful for adding handlers that run before and after pre matching
 */
public final class GlobalHandlerCustomizerBuildItem extends MultiBuildItem {

    private final HandlerChainCustomizer customizer;

    public GlobalHandlerCustomizerBuildItem(HandlerChainCustomizer customizer) {
        this.customizer = customizer;
    }

    public HandlerChainCustomizer getCustomizer() {
        return customizer;
    }
}
