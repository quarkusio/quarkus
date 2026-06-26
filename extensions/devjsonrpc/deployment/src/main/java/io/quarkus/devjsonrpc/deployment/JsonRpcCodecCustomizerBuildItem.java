package io.quarkus.devjsonrpc.deployment;

import java.util.function.Consumer;

import io.quarkus.builder.item.MultiBuildItem;
import tools.jackson.databind.json.JsonMapper;

public final class JsonRpcCodecCustomizerBuildItem extends MultiBuildItem {
    private final Consumer<JsonMapper.Builder> customizer;

    public JsonRpcCodecCustomizerBuildItem(Consumer<JsonMapper.Builder> customizer) {
        this.customizer = customizer;
    }

    public Consumer<JsonMapper.Builder> getCustomizer() {
        return customizer;
    }
}
