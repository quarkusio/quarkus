package io.quarkus.vertx.runtime.jackson;

import java.util.Map;

import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.deser.std.StdDelegatingDeserializer;
import com.fasterxml.jackson.databind.util.Converter;
import com.fasterxml.jackson.databind.util.StdConverter;

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
            JsonDeserializer<?> delegateDeserializer) {
        return new StdDelegatingDeserializer<>(converter, delegateType, delegateDeserializer);
    }
}
