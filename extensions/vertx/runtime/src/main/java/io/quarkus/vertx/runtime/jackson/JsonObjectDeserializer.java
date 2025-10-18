package io.quarkus.vertx.runtime.jackson;

import java.util.Map;

import tools.jackson.databind.JavaType;
import tools.jackson.databind.ValueDeserializer;
import tools.jackson.databind.deser.std.StdDelegatingDeserializer;
import tools.jackson.databind.util.Converter;
import tools.jackson.databind.util.StdConverter;

import io.vertx.core.json.JsonObject;

public class JsonObjectDeserializer extends StdDelegatingDeserializer<JsonObject> {

    public JsonObjectDeserializer() {
        super(new StdConverter<Map<?, ?>, JsonObject>() {
            @Override
            public JsonObject convert(Map map) {
                return new JsonObject(map);
            }
        });
    }

    @Override
    protected StdDelegatingDeserializer<JsonObject> withDelegate(Converter<Object, JsonObject> converter,
            JavaType delegateType,
            ValueDeserializer<?> delegateDeserializer) {
        return new StdDelegatingDeserializer<>(converter, delegateType, delegateDeserializer);
    }
}
