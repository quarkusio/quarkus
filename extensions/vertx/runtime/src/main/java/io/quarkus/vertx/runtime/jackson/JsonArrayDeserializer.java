package io.quarkus.vertx.runtime.jackson;

import java.util.List;

import tools.jackson.databind.JavaType;
import tools.jackson.databind.ValueDeserializer;
import tools.jackson.databind.deser.std.StdDelegatingDeserializer;
import tools.jackson.databind.util.Converter;
import tools.jackson.databind.util.StdConverter;

import io.vertx.core.json.JsonArray;

public class JsonArrayDeserializer extends StdDelegatingDeserializer<JsonArray> {

    public JsonArrayDeserializer() {
        super(new StdConverter<List<?>, JsonArray>() {
            @Override
            public JsonArray convert(List list) {
                return new JsonArray(list);
            }
        });
    }

    @Override
    protected StdDelegatingDeserializer<JsonArray> withDelegate(Converter<Object, JsonArray> converter,
            JavaType delegateType,
            ValueDeserializer<?> delegateDeserializer) {
        return new StdDelegatingDeserializer<>(converter, delegateType, delegateDeserializer);
    }
}
