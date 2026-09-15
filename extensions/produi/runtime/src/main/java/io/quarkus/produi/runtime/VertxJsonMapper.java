package io.quarkus.produi.runtime;

import io.quarkus.devjsonrpc.runtime.jsonrpc.json.JsonMapper;
import io.vertx.core.json.Json;

/**
 * A simple {@link JsonMapper} backed by Vert.x JSON (Jackson) for production use.
 * Unlike the dev-mode mapper, this does not need the deployment linker mechanism
 * since there is no split classloader in production.
 */
public class VertxJsonMapper implements JsonMapper {

    @Override
    public String toString(Object object, boolean pretty) {
        if (pretty) {
            return Json.encodePrettily(object);
        }
        return Json.encode(object);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T fromString(String json, Class<T> target) {
        return Json.decodeValue(json, target);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T fromValue(Object json, Class<T> target) {
        if (target.isInstance(json)) {
            return target.cast(json);
        }
        if (target.isPrimitive()) {
            return (T) json;
        }
        return Json.decodeValue(Json.encode(json), target);
    }
}
