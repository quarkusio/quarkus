package io.quarkus.devjsonrpc.deployment;

import java.util.function.Consumer;

import com.fasterxml.jackson.databind.ObjectMapper;

import io.quarkus.builder.item.MultiBuildItem;

public final class JsonRpcCodecCustomizerBuildItem extends MultiBuildItem {
    private final Consumer<ObjectMapper> customizer;

    public JsonRpcCodecCustomizerBuildItem(Consumer<ObjectMapper> customizer) {
        this.customizer = customizer;
    }

    public Consumer<ObjectMapper> getCustomizer() {
        return customizer;
    }
}
