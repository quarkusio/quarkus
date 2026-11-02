package io.quarkus.devjsonrpc.deployment;

import io.quarkus.builder.item.SimpleBuildItem;
import io.quarkus.devjsonrpc.runtime.jsonrpc.json.JsonMapper;
import io.quarkus.runtime.RuntimeValue;

/**
 * The JSON mapper the {@link io.quarkus.devjsonrpc.runtime.comms.JsonRpcRouter} codec should use, for example one that
 * knows about the Vert.x JSON types. When absent, the router uses a mapper without any extra type adapters.
 */
public final class JsonRpcMapperBuildItem extends SimpleBuildItem {

    private final RuntimeValue<JsonMapper> jsonMapper;

    public JsonRpcMapperBuildItem(RuntimeValue<JsonMapper> jsonMapper) {
        this.jsonMapper = jsonMapper;
    }

    public RuntimeValue<JsonMapper> getJsonMapper() {
        return jsonMapper;
    }
}
