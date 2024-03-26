package io.quarkus.vertx.runtime.jackson;

import java.util.List;

import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.deser.std.StdDelegatingDeserializer;
import com.fasterxml.jackson.databind.util.Converter;
import com.fasterxml.jackson.databind.util.StdConverter;

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
            JsonDeserializer<?> delegateDeserializer) {
        return new StdDelegatingDeserializer<>(converter, delegateType, delegateDeserializer);
    }
}
